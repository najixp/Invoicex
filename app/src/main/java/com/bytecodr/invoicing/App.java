package com.bytecodr.invoicing;

import android.app.Application;
import android.content.SharedPreferences;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bytecodr.invoicing.helper.helper_number;
import com.bytecodr.invoicing.helper.helper_string;
import com.bytecodr.invoicing.main.LoginActivity;
import com.bytecodr.invoicing.main.MainActivity;
import com.bytecodr.invoicing.model.Client;
import com.bytecodr.invoicing.model.Description;
import com.bytecodr.invoicing.model.DoublePreference;
import com.bytecodr.invoicing.model.Estimate;
import com.bytecodr.invoicing.model.EstimateItem;
import com.bytecodr.invoicing.model.Invoice;
import com.bytecodr.invoicing.model.InvoiceItem;
import com.bytecodr.invoicing.model.Item;
import com.bytecodr.invoicing.model.StringPreference;
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
    public Apis api;

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

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor).build();

        retrofit = new Retrofit.Builder()
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .build();

        api = retrofit.create(Apis.class);
        VolleyLog.DEBUG = true;
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());

        Realm.setDefaultConfiguration(realmConfiguration);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        mRequestQueue.add(req);
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

    List<Long> pendingClients = new ArrayList<>(),
            pendingDescriptions = new ArrayList<>(),
            pendingEstimates = new ArrayList<>(),
            pendingInvoices = new ArrayList<>(),
            pendingItems = new ArrayList<>();

    public void updateData() {
        SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);
        String userId = String.valueOf(settings.getInt("id", -1));

        JSONObject api_parameter = new JSONObject();
        try {
            api_parameter.put("user_id", settings.getInt("id", 0));
            api_parameter.put("include_logo", 1);
        } catch (JSONException ex) {
        }

        addToRequestQueue(new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "clients/get", api_parameter, response -> {
                    try {
                        JSONObject result = ((JSONObject) response.get("data"));
                        JSONArray items = (JSONArray) result.get("clients");

                        String logoImage = helper_string.optString(result, "logo");

                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(realm13 -> realm13.insertOrUpdate(new StringPreference("logoImage", logoImage)));
                            realm.executeTransaction(realm1 -> realm1.where(Client.class).equalTo("pendingUpdate", false).equalTo("pendingDelete", false).findAll().deleteAllFromRealm());

                            for (int i = 0; i < items.length(); i++) {
                                JSONObject obj = items.getJSONObject(i);

                                Client item = new Client();

                                item.Id = obj.optInt("id");

                                if (realm.where(Client.class).equalTo("Id", item.Id).count() > 0)
                                    continue;

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

                                realm.executeTransaction(realm12 -> realm12.insert(item));
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
                        JSONArray estimate_lines = (JSONArray) result.get("estimate_lines");

                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(realm1 -> realm1.where(Estimate.class).equalTo("pendingUpdate", false).equalTo("pendingDelete", false).findAll().deleteAllFromRealm());

                            for (int i = 0; i < estimates.length(); i++) {
                                try {
                                    JSONObject obj = estimates.getJSONObject(i);

                                    Estimate estimate = new Estimate();

                                    estimate.Id = obj.optInt("id");

                                    if (realm.where(Estimate.class).equalTo("Id", estimate.Id).count() > 0)
                                        continue;

                                    estimate.UserId = obj.optInt("user_id");

                                    estimate.EstimateNumber = obj.getInt("estimate_number");
                                    estimate.ClientName = helper_string.optString(obj, "client_name");
                                    estimate.ClientId = obj.getInt("client_id");
                                    estimate.ClientNote = helper_string.optString(obj, "notes");
                                    estimate.EstimateDate = obj.optInt("estimate_date", 0);
                                    estimate.EstimateDueDate = obj.optInt("due_date", 0);
                                    estimate.TaxRate = helper_number.optDouble(obj, "tax_rate");
                                    estimate.TotalMoney = helper_number.optDouble(obj, "total");
                                    estimate.IsInvoiced = (obj.getInt("is_invoiced") == 1);

                                    estimate.Created = obj.optInt("created_on", 0);
                                    estimate.Updated = obj.optInt("updated_on", 0);

                                    realm.executeTransaction(realm12 -> realm12.insertOrUpdate(estimate));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            for (int j = 0; j < estimate_lines.length(); j++) {
                                JSONObject obj1 = estimate_lines.getJSONObject(j);

                                EstimateItem item = new EstimateItem();
                                item.Id = obj1.optInt("id");
                                item.EstimateId = obj1.optInt("estimate_id");
                                item.Quantity = obj1.optDouble("quantity");
                                item.Name = helper_string.optString(obj1, "name");
                                item.Rate = obj1.optDouble("rate");
                                item.Description = helper_string.optString(obj1, "description");

                                realm.executeTransaction(realm14 -> realm14.insertOrUpdate(item));
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
                        JSONArray invoice_lines = (JSONArray) result.get("invoice_lines");

                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(realm1 -> realm1.where(Invoice.class).equalTo("pendingUpdate", false).equalTo("pendingDelete", false).findAll().deleteAllFromRealm());

                            for (int i = 0; i < invoices.length(); i++) {
                                try {
                                    JSONObject obj = invoices.getJSONObject(i);

                                    Invoice invoice = new Invoice();

                                    invoice.Id = obj.optInt("id");

                                    if (realm.where(Invoice.class).equalTo("Id", invoice.Id).count() > 0)
                                        continue;

                                    invoice.UserId = obj.optInt("user_id");

                                    invoice.InvoiceNumber = obj.getInt("invoice_number");
                                    invoice.ClientName = helper_string.optString(obj, "client_name");
                                    invoice.ClientId = obj.getInt("client_id");
                                    invoice.ClientNote = helper_string.optString(obj, "notes");
                                    invoice.InvoiceDate = obj.optInt("invoice_date", 0);
                                    invoice.InvoiceDueDate = obj.optInt("due_date", 0);
                                    invoice.TaxRate = helper_number.optDouble(obj, "tax_rate");
                                    invoice.TotalMoney = helper_number.optDouble(obj, "total");
                                    invoice.IsPaid = (obj.getInt("is_paid") == 1);

                                    invoice.Created = obj.optInt("created_on", 0);
                                    invoice.Updated = obj.optInt("updated_on", 0);

                                    if (invoice.InvoiceDate >= monthStartDate && invoice.InvoiceDate <= monthEndDate && !invoice.IsPaid)
                                        unpaid_total += invoice.TotalMoney;

                                    realm.executeTransaction(realm12 -> realm12.insertOrUpdate(invoice));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            for (int j = 0; j < invoice_lines.length(); j++) {
                                JSONObject obj1 = invoice_lines.getJSONObject(j);

                                InvoiceItem item = new InvoiceItem();
                                item.Id = obj1.optInt("id");
                                item.InvoiceId = obj1.optInt("invoice_id");
                                item.Quantity = obj1.optDouble("quantity");
                                item.Name = helper_string.optString(obj1, "name");
                                item.Rate = obj1.optDouble("rate");
                                item.Description = helper_string.optString(obj1,
                                        "description");

                                realm.executeTransaction(realm14 -> realm14.insertOrUpdate(item));
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
                            realm.executeTransaction(realm1 -> realm1.where(Item.class).equalTo("pendingUpdate", false).equalTo("pendingDelete", false).findAll().deleteAllFromRealm());

                            for (int i = 0; i < items.length(); i++) {
                                JSONObject obj = items.getJSONObject(i);

                                Item item = new Item();

                                item.Id = obj.optInt("id");

                                if (realm.where(Item.class).equalTo("Id", item.Id).count() > 0)
                                    continue;

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

        App.getInstance().api.getDescriptions(SERVER_KEY_HASH, userId).enqueue(new Callback<JsonObject>() {
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
                                realm1.where(Description.class).equalTo("pendingUpdate", false).equalTo("pendingDelete", false).findAll().deleteAllFromRealm();

                                for (Description description : descriptions)
                                    if (realm1.where(Description.class).equalTo("id", description.id).count() == 0)
                                        realm1.insertOrUpdate(description);
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

        try (Realm realm = Realm.getDefaultInstance()) {
            for (final Client client : realm.copyFromRealm(realm.where(Client.class).equalTo("pendingUpdate", true).findAll())) {
                if (pendingClients.contains(client.Id))
                    continue;

                pendingClients.add(client.Id);

                try {
                    api_parameter = new JSONObject();
                    api_parameter.put("user_id", settings.getInt("id", 0));

                    if (client.Id >= 0) {
                        api_parameter.put("id", client.Id + "");
                    }

                    api_parameter.put("name", client.Name);
                    api_parameter.put("email", client.Email);
                    api_parameter.put("address1", client.Address1);
                    api_parameter.put("address2", client.Address2);
                    api_parameter.put("city", client.City);
                    api_parameter.put("state", client.State);
                    api_parameter.put("postcode", client.Postcode);
                    api_parameter.put("country", client.Country);

                    addToRequestQueue(new JsonObjectRequest
                            (Request.Method.POST, Network.API_URL + "clients/create", api_parameter, response -> {
                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> realm2.where(Client.class).equalTo("Id", client.Id).findAll().deleteAllFromRealm());
                                }
                                updateData();
                                pendingClients.remove(client.Id);
                            }, error -> pendingClients.remove(client.Id)) {

                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> params = new HashMap<>();
                            params.put("X-API-KEY", MainActivity.api_key);
                            return params;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            for (final Client client : realm.copyFromRealm(realm.where(Client.class).equalTo("pendingDelete", true).findAll())) {
                if (pendingClients.contains(client.Id))
                    continue;

                pendingClients.add(client.Id);

                try {
                    api_parameter = new JSONObject();
                    api_parameter.put("id", client.Id + "");
                    api_parameter.put("user_id", settings.getInt("id", 0));

                    addToRequestQueue(new JsonObjectRequest
                            (Request.Method.POST, Network.API_URL + "clients/delete", api_parameter, response -> {
                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> realm2.where(Client.class).equalTo("Id", client.Id).findAll().deleteAllFromRealm());
                                }
                                updateData();
                                pendingClients.remove(client.Id);
                            }, error -> pendingClients.remove(client.Id)) {

                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> params = new HashMap<>();
                            params.put("X-API-KEY", MainActivity.api_key);
                            return params;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            for (final Item item : realm.copyFromRealm(realm.where(Item.class).equalTo("pendingUpdate", true).findAll())) {
                if (pendingItems.contains(item.Id))
                    continue;

                pendingItems.add(item.Id);

                try {
                    api_parameter = new JSONObject();
                    api_parameter.put("user_id", settings.getInt("id", 0));

                    if (item.Id >= 0) {
                        api_parameter.put("id", item.Id + "");
                    }

                    api_parameter.put("name", item.Name);
                    api_parameter.put("rate", item.Rate);
                    api_parameter.put("description", item.Description);

                    addToRequestQueue(new JsonObjectRequest
                            (Request.Method.POST, Network.API_URL + "items/create", api_parameter, response -> {
                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> realm2.where(Item.class).equalTo("Id", item.Id).findAll().deleteAllFromRealm());
                                }
                                updateData();
                                pendingItems.remove(item.Id);
                            }, error -> pendingItems.remove(item.Id)) {

                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> params = new HashMap<>();
                            params.put("X-API-KEY", MainActivity.api_key);
                            return params;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            for (final Item item : realm.copyFromRealm(realm.where(Item.class).equalTo("pendingDelete", true).findAll())) {
                if (pendingItems.contains(item.Id))
                    continue;

                pendingItems.add(item.Id);

                try {
                    api_parameter = new JSONObject();
                    api_parameter.put("id", item.Id + "");
                    api_parameter.put("user_id", settings.getInt("id", 0));

                    addToRequestQueue(new JsonObjectRequest
                            (Request.Method.POST, Network.API_URL + "items/delete", api_parameter, response -> {
                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> realm2.where(Item.class).equalTo("Id", item.Id).findAll().deleteAllFromRealm());
                                }
                                updateData();
                                pendingItems.remove(item.Id);
                            }, error -> pendingItems.remove(item.Id)) {

                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> params = new HashMap<>();
                            params.put("X-API-KEY", MainActivity.api_key);
                            return params;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            for (final Description description : realm.copyFromRealm(realm.where(Description.class).equalTo("pendingUpdate", true).findAll())) {
                if (pendingDescriptions.contains(description.id))
                    continue;

                pendingDescriptions.add(description.id);

                App.getInstance().api.addDescription(SERVER_KEY_HASH, userId, description.title, description.description).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            JsonObject responseJo = response.body();
                            String status = responseJo.get("status").getAsString();
                            if (status.equals("true")) {
                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> realm2.where(Description.class).equalTo("id", description.id).findAll().deleteAllFromRealm());
                                }
                                updateData();
                                pendingDescriptions.remove(description.id);
                            }

                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        pendingDescriptions.remove(description.id);
                    }
                });
            }

            for (final Description description : realm.copyFromRealm(realm.where(Description.class).equalTo("pendingDelete", true).findAll())) {
                if (pendingDescriptions.contains(description.id))
                    continue;

                pendingDescriptions.add(description.id);

                App.getInstance().api.deleteDescription(SERVER_KEY_HASH, userId, description.id + "").enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            JsonObject responseJo = response.body();
                            String status = responseJo.get("status").getAsString();
                            if (status.equals("true")) {
                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> realm2.where(Description.class).equalTo("id", description.id).findAll().deleteAllFromRealm());
                                }
                                updateData();
                                pendingDescriptions.remove(description.id);
                            }

                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        pendingDescriptions.remove(description.id);
                    }
                });
            }

            for (final Estimate estimate : realm.copyFromRealm(realm.where(Estimate.class).equalTo("pendingUpdate", true).findAll())) {
                if (pendingEstimates.contains(estimate.Id))
                    continue;

                pendingEstimates.add(estimate.Id);

                try {
                    api_parameter = new JSONObject();
                    api_parameter.put("user_id", settings.getInt("id", 0));

                    if (estimate.Id >= 0) {
                        api_parameter.put("id", estimate.Id + "");
                    }

                    api_parameter.put("estimate_number", estimate.EstimateNumber);
                    api_parameter.put("tax_rate", estimate.TaxRate);
                    api_parameter.put("client_id", estimate.ClientId);
                    api_parameter.put("notes", estimate.ClientNote);

                    api_parameter.put("estimate_date", estimate.EstimateDate);
                    api_parameter.put("due_date", estimate.EstimateDueDate);

                    Gson json = new Gson();

                    api_parameter.put("items", json.toJson(realm.copyFromRealm(realm.where(EstimateItem.class).equalTo("EstimateId", estimate.Id).findAll())));

                    addToRequestQueue(new JsonObjectRequest
                            (Request.Method.POST, Network.API_URL + "estimates/create", api_parameter, response -> {
                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> {
                                        realm2.where(Estimate.class).equalTo("Id", estimate.Id).findAll().deleteAllFromRealm();
                                        realm2.where(EstimateItem.class).equalTo("EstimateId", estimate.Id).findAll().deleteAllFromRealm();
                                    });
                                }
                                updateData();
                                pendingEstimates.remove(estimate.Id);
                            }, error -> pendingEstimates.remove(estimate.Id)) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> params = new HashMap<>();
                            params.put("X-API-KEY", MainActivity.api_key);
                            return params;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            for (final Estimate estimate : realm.copyFromRealm(realm.where(Estimate.class).equalTo("pendingDelete", true).findAll())) {
                if (pendingEstimates.contains(estimate.Id))
                    continue;

                pendingEstimates.add(estimate.Id);

                try {
                    api_parameter = new JSONObject();
                    api_parameter.put("id", estimate.Id + "");
                    api_parameter.put("user_id", settings.getInt("id", 0));

                    addToRequestQueue(new JsonObjectRequest
                            (Request.Method.POST, Network.API_URL + "estimates/delete", api_parameter, response -> {

                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> {
                                        realm2.where(Estimate.class).equalTo("Id", estimate.Id).findAll().deleteAllFromRealm();
                                        realm2.where(EstimateItem.class).equalTo("EstimateId", estimate.Id).findAll().deleteAllFromRealm();
                                    });
                                }
                                updateData();
                                pendingEstimates.remove(estimate.Id);
                            }, error -> pendingEstimates.remove(estimate.Id)) {

                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> params = new HashMap<>();
                            params.put("X-API-KEY", MainActivity.api_key);
                            return params;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            for (final Invoice invoice : realm.copyFromRealm(realm.where(Invoice.class).equalTo("pendingUpdate", true).findAll())) {
                if (pendingInvoices.contains(invoice.Id))
                    continue;

                pendingInvoices.add(invoice.Id);

                try {
                    api_parameter = new JSONObject();
                    api_parameter.put("user_id", settings.getInt("id", 0));

                    if (invoice.Id >= 0) {
                        api_parameter.put("id", invoice.Id + "");
                    }

                    api_parameter.put("invoice_number", invoice.InvoiceNumber);
                    api_parameter.put("tax_rate", invoice.TaxRate);
                    api_parameter.put("client_id", invoice.ClientId);
                    api_parameter.put("notes", invoice.ClientNote);
                    api_parameter.put("paid", (invoice.IsPaid ? 1 : 0));

                    api_parameter.put("invoice_date", invoice.InvoiceDate);
                    api_parameter.put("due_date", invoice.InvoiceDueDate);

                    Gson json = new Gson();

                    api_parameter.put("items", json.toJson(realm.copyFromRealm(realm.where(InvoiceItem.class).equalTo("InvoiceId", invoice.Id).findAll())));

                    addToRequestQueue(new JsonObjectRequest
                            (Request.Method.POST, Network.API_URL + "invoices/create", api_parameter, response -> {
                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> {
                                        realm2.where(Invoice.class).equalTo("Id", invoice.Id).findAll().deleteAllFromRealm();
                                        realm2.where(InvoiceItem.class).equalTo("InvoiceId", invoice.Id).findAll().deleteAllFromRealm();
                                    });
                                }
                                updateData();
                                pendingInvoices.remove(invoice.Id);
                            }, error -> pendingEstimates.remove(invoice.Id)) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> params = new HashMap<>();
                            params.put("X-API-KEY", MainActivity.api_key);
                            return params;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            for (final Invoice invoice : realm.copyFromRealm(realm.where(Invoice.class).equalTo("pendingDelete", true).findAll())) {
                if (pendingInvoices.contains(invoice.Id))
                    continue;

                pendingInvoices.add(invoice.Id);

                try {
                    api_parameter = new JSONObject();
                    api_parameter.put("id", invoice.Id + "");
                    api_parameter.put("user_id", settings.getInt("id", 0));

                    addToRequestQueue(new JsonObjectRequest
                            (Request.Method.POST, Network.API_URL + "invoices/delete", api_parameter, response -> {

                                try (Realm realm1 = Realm.getDefaultInstance()) {
                                    realm1.executeTransaction(realm2 -> {
                                        realm2.where(Invoice.class).equalTo("Id", invoice.Id).findAll().deleteAllFromRealm();
                                        realm2.where(InvoiceItem.class).equalTo("InvoiceId", invoice.Id).findAll().deleteAllFromRealm();
                                    });
                                }
                                updateData();
                                pendingInvoices.remove(invoice.Id);
                            }, error -> pendingEstimates.remove(invoice.Id)) {

                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> params = new HashMap<>();
                            params.put("X-API-KEY", MainActivity.api_key);
                            return params;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
