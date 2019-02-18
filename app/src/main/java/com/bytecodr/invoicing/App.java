package com.bytecodr.invoicing;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bytecodr.invoicing.helper.helper_string;
import com.bytecodr.invoicing.main.LoginActivity;
import com.bytecodr.invoicing.main.MainActivity;
import com.bytecodr.invoicing.model.Client;
import com.bytecodr.invoicing.model.Description;
import com.bytecodr.invoicing.model.DoublePreference;
import com.bytecodr.invoicing.model.Estimate;
import com.bytecodr.invoicing.model.Invoice;
import com.bytecodr.invoicing.model.Item;
import com.bytecodr.invoicing.network.Network;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class App extends Application {

    private static final String BASE_URL = "http://soldv.com/gold/index.php/";
    public static final String SERVER_KEY_HASH = "d4c8255fd7e91f8e3a9c3af31ff8274a";

    final static int dbSchemaVersion = 1;

    private static App mInstance;

    private static Retrofit retrofit = null;
    private RequestQueue mRequestQueue;

    public static synchronized App getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        Realm.init(getApplicationContext());

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .schemaVersion(dbSchemaVersion)
                .build();

        Realm.setDefaultConfiguration(realmConfiguration);
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    private static OkHttpClient buildClient() {
        return new OkHttpClient
                .Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
    }

    private static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .client(buildClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(BASE_URL)
                    .build();
        }
        return retrofit;
    }

    public static Apis getApis() {
        return getClient().create(Apis.class);
    }

    private List<Runnable> onUpdatedListeners = new ArrayList<>();

    public void registerOnUpdateListener(Runnable listener) {
        if (!onUpdatedListeners.contains(listener))
            onUpdatedListeners.add(listener);
    }

    private void notifyOnUpdateListeners() {
        for (Runnable onUpdatedListener : onUpdatedListeners)
            if (onUpdatedListener != null)
                try {
                    onUpdatedListener.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
    }

    public void updateData() {
        SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);
        String userId = String.valueOf(settings.getInt("id", -1));

        JSONObject api_parameter = new JSONObject();
        try {
            api_parameter.put("user_id", settings.getInt("id", 0));
        } catch (JSONException ex) {
        }

        addToRequestQueue(new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "clients/get", api_parameter, response -> {
                    try {
                        JSONObject result = ((JSONObject) response.get("data"));
                        JSONArray items = (JSONArray) result.get("clients");

                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(realm1 -> realm1.where(Client.class).findAll().deleteAllFromRealm());

                            for (int i = 0; i < items.length(); i++) {
                                JSONObject obj = items.getJSONObject(i);

                                Client item = new Client();

                                item.Id = obj.optInt("id");
                                item.UserId = obj.optInt("user_id");

                                item.Name = helper_string.optString(obj, "name");
                                item.Email = helper_string.optString(obj, "email");
                                item.Address1 = helper_string.optString(obj, "address1");
                                item.Address2 = helper_string.optString(obj, "address2");

                                item.City = helper_string.optString(obj, "city");
                                item.State = helper_string.optString(obj, "state");
                                item.Postcode = helper_string.optString(obj, "postcode");
                                item.Country = helper_string.optString(obj, "country");
                                item.TotalMoney = obj.optDouble("total", 0);

                                item.Created = obj.optInt("created_on", 0);
                                item.Updated = obj.optInt("updated_on", 0);

                                realm.executeTransaction(realm12 -> realm12.insertOrUpdate(item));
                            }
                        }

                        notifyOnUpdateListeners();
                    } catch (Exception ex) {
                    }

                }, error -> {
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("X-API-KEY", MainActivity.api_key);
                return params;
            }
        });

        addToRequestQueue(new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "estimates/get", api_parameter, response -> {
                    try {
                        JSONObject result = ((JSONObject) response.get("data"));
                        JSONArray estimates = (JSONArray) result.get("estimates");

                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(realm1 -> realm1.where(Estimate.class).findAll().deleteAllFromRealm());

                            for (int i = 0; i < estimates.length(); i++) {
                                JSONObject obj = estimates.getJSONObject(i);

                                Estimate estimate = new Estimate();

                                estimate.Id = obj.optInt("id");
                                estimate.UserId = obj.optInt("user_id");

                                estimate.EstimateNumber = obj.getInt("estimate_number");
                                estimate.ClientName = helper_string.optString(obj, "client_name");
                                estimate.ClientId = obj.getInt("client_id");
                                estimate.ClientNote = helper_string.optString(obj, "notes");
                                estimate.EstimateDate = obj.optInt("estimate_date", 0);
                                estimate.EstimateDueDate = obj.optInt("due_date", 0);
                                estimate.TaxRate = obj.getDouble("tax_rate");
                                estimate.TotalMoney = obj.getDouble("total");
                                estimate.IsInvoiced = (obj.getInt("is_invoiced") == 1);

                                estimate.Created = obj.optInt("created_on", 0);
                                estimate.Updated = obj.optInt("updated_on", 0);

                                realm.executeTransaction(realm12 -> realm12.insertOrUpdate(estimate));
                            }
                        }
                        notifyOnUpdateListeners();
                    } catch (Exception ex) {
                    }
                }, error -> {
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("X-API-KEY", MainActivity.api_key);
                return params;
            }
        });

        addToRequestQueue(new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "invoices/get", api_parameter, response -> {
                    try {
                        Calendar calendar = Calendar.getInstance(); // this takes current date
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
                        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
                        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
                        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));

                        //Getting first of the last 4 months
                        long monthStartDate = calendar.getTimeInMillis() / 1000;

                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
                        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
                        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));

                        long monthEndDate = calendar.getTimeInMillis() / 1000;

                        double unpaid_total = 0;

                        JSONObject result = ((JSONObject) response.get("data"));
                        JSONArray invoices = (JSONArray) result.get("invoices");

                        Log.e("INVOICES", invoices.toString());

                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(realm1 -> realm1.where(Invoice.class).findAll().deleteAllFromRealm());

                            for (int i = 0; i < invoices.length(); i++) {
                                JSONObject obj = invoices.getJSONObject(i);

                                Invoice invoice = new Invoice();

                                invoice.Id = obj.optInt("id");
                                invoice.UserId = obj.optInt("user_id");

                                invoice.InvoiceNumber = obj.getInt("invoice_number");
                                invoice.ClientName = helper_string.optString(obj, "client_name");
                                invoice.ClientId = obj.getInt("client_id");
                                invoice.ClientNote = helper_string.optString(obj, "notes");
                                invoice.InvoiceDate = obj.optInt("invoice_date", 0);
                                invoice.InvoiceDueDate = obj.optInt("due_date", 0);
                                invoice.TaxRate = obj.getDouble("tax_rate");
                                invoice.TotalMoney = obj.getDouble("total");
                                invoice.IsPaid = (obj.getInt("is_paid") == 1);

                                invoice.Created = obj.optInt("created_on", 0);
                                invoice.Updated = obj.optInt("updated_on", 0);

                                if (invoice.InvoiceDate >= monthStartDate && invoice.InvoiceDate <= monthEndDate && !invoice.IsPaid)
                                    unpaid_total += invoice.TotalMoney;

                                realm.executeTransaction(realm12 -> realm12.insertOrUpdate(invoice));
                            }

                            double finalUnpaid_total = unpaid_total;
                            realm.executeTransaction(realm1 -> realm1.insertOrUpdate(new DoublePreference("unpaidTotal", finalUnpaid_total)));
                        }
                        notifyOnUpdateListeners();
                    } catch (Exception ex) {
                    }
                }, error -> {
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("X-API-KEY", MainActivity.api_key);
                return params;
            }
        });

        addToRequestQueue(new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "items/get", api_parameter, response -> {
                    try {
                        JSONObject result = ((JSONObject) response.get("data"));
                        JSONArray items = (JSONArray) result.get("items");

                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(realm1 -> realm1.where(Item.class).findAll().deleteAllFromRealm());

                            for (int i = 0; i < items.length(); i++) {
                                JSONObject obj = items.getJSONObject(i);

                                Item item = new Item();

                                item.Id = obj.optInt("id");
                                item.UserId = obj.optInt("user_id");

                                item.Name = helper_string.optString(obj, "name");
                                item.Description = helper_string.optString(obj, "description");
                                item.Rate = obj.optDouble("rate");

                                item.Created = obj.optInt("created_on", 0);
                                item.Updated = obj.optInt("updated_on", 0);

                                realm.executeTransaction(realm12 -> realm12.insertOrUpdate(item));
                            }
                        }
                        notifyOnUpdateListeners();
                    } catch (Exception ex) {
                    }

                }, error -> {
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("X-API-KEY", MainActivity.api_key);
                return params;
            }
        });

        App.getApis().getDescriptions(SERVER_KEY_HASH, userId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject responseJo = response.body();
                    String status = responseJo.get("status").getAsString();
                    if (status.equals("true")) {
                        JsonElement dataJe = responseJo.get("desc_data");
                        Type type = new TypeToken<List<Description>>() {
                        }.getType();
                        List<Description> descriptions = new Gson().fromJson(dataJe, type);
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(realm1 -> {
                                realm1.where(Description.class).findAll().deleteAllFromRealm();
                                realm1.insertOrUpdate(descriptions);
                            });
                        }
                    }
                    notifyOnUpdateListeners();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }
}
