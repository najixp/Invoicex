package com.bytecodr.invoicing.main;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.model.Description;
import com.bytecodr.invoicing.model.EstimateItem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.rey.material.widget.Spinner;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

import static com.bytecodr.invoicing.App.SERVER_KEY_HASH;

public class EstimateItemPickerActivity extends AppCompatActivity
{
    private MaterialDialog progressDialog;

    private String userId;
    EstimateItem currentItem;
    Integer itemPosition;

    private List<EstimateItem> array_list_items = new ArrayList<>();
    private List<Description> array_list_descriptions = new ArrayList<>();
    Spinner spinner_items;
    Spinner spinner_descriptions;

    private EditText edit_name;
    private EditText edit_description;
    private EditText edit_rate;
    private EditText edit_quantity;

    private String currency;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_picker);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);
        userId = String.valueOf(settings.getInt("id", -1));

        //Means user is not logged in
        if (settings == null || settings.getInt("logged_in", 0) == 0 || settings.getString("api_key", "").equals(""))
        {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        currency = settings.getString(SettingActivity.KEY_CURRENCY_SYMBOL, "$");

        progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        edit_name = (EditText) findViewById(R.id.edit_name);
        edit_description = (EditText) findViewById(R.id.edit_description);
        edit_rate = (EditText) findViewById(R.id.edit_rate);
        edit_quantity = (EditText) findViewById(R.id.edit_quantity);

        spinner_items = (Spinner) findViewById(R.id.spinner_items);
        spinner_items.setOnItemSelectedListener((parent, view, position, id) -> {
            if (position != 0) {
                EstimateItem item = array_list_items.get(position - 1);

                edit_name.setText(item.Name);
                edit_description.setText(item.Description);
                edit_rate.setText(String.format("%.2f", item.Rate));
                edit_quantity.setText("1");
            }
        });

        spinner_descriptions = (Spinner) findViewById(R.id.spinner_description);
        spinner_descriptions.setOnItemSelectedListener((parent, view, position, id) -> {
            if (position != 0) {
                Description description = array_list_descriptions.get(position - 1);
                edit_description.setText(description.description);
            }
        });

        currentItem = (EstimateItem) getIntent().getSerializableExtra("data");
        itemPosition = getIntent().getIntExtra("position", -1);

        if (currentItem != null)
        {
            edit_name.setText(currentItem.Name);
            edit_description.setText(currentItem.Description);
            edit_rate.setText(String.format("%.2f", currentItem.Rate));
            edit_quantity.setText(String.valueOf(currentItem.Quantity));
        }

        getData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_item_picker, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save)
        {
            if (isFormValid())
            {
                EstimateItem newItem = new EstimateItem();
                newItem.Name = edit_name.getText().toString();
                newItem.Description = edit_description.getText().toString();

                String rate = edit_rate.getText().toString();
                String quantity = edit_quantity.getText().toString();

                newItem.Rate = 0;

                try
                {
                    if (!rate.isEmpty())
                    {
                        NumberFormat format = NumberFormat.getInstance(getResources().getConfiguration().locale);
                        Number number = format.parse(rate);
                        newItem.Rate =  number.doubleValue();
                    }
                }
                catch (Exception ex)    { }

                //newItem.Rate = Double.parseDouble((rate.isEmpty() ? "0" : rate));
                //newItem.Quantity = Integer.parseInt((quantity.isEmpty() ? "0" : quantity));
                newItem.Quantity = Double.parseDouble((quantity.isEmpty() ? "0" : quantity));

                Intent returnIntent = new Intent();
                returnIntent.putExtra("data", newItem);

                if (currentItem != null) returnIntent.putExtra("position", itemPosition);

                setResult(Activity.RESULT_OK, returnIntent);
                finish();

                return true;
            }
            else
            {
                return false;
            }
        }
        else if (id == android.R.id.home)
        {
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED,returnIntent);
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isFormValid()
    {
        boolean isValid = true;

        if (edit_name.getText().toString().trim().length() == 0) {
            edit_name.setError("Name required");
            isValid = false;
        } else
            edit_name.setError(null);

        return isValid;
    }

/*    public void RunGetItemService() {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "items/get", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            JSONObject result = ((JSONObject)response.get("data"));
                            JSONArray items = (JSONArray)result.get("items");

                            array_items = new String[ items.length() + 1];
                            array_list_items.clear();

                            Item selectItem = new Item();
                            selectItem.Id = 0;
                            selectItem.Name = "Select Item";
                            array_list_items.add(selectItem);
                            array_items[0] = "Select Item";

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

                                array_list_items.add(item);
                                array_items[i+1] = item.Name;
                            }

                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(ItemPickerActivity.this, R.layout.custom_simple_spinner_item, array_items);
                            spinner_items.setAdapter(adapter);

                            if (itemPosition != null)
                            {

                            }
                        }
                        catch(Exception ex)
                        {
                            Toast.makeText(ItemPickerActivity.this, R.string.error_try_again_support, Toast.LENGTH_LONG).show();
                        }

                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    }
                }, new Response.ErrorListener()
                {

                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        // TODO Auto-generated method stub
                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }

                        NetworkResponse response = error.networkResponse;
                        if (response != null && response.data != null)
                        {
                            try
                            {
                                JSONObject json = new JSONObject(new String(response.data));
                                Toast.makeText(ItemPickerActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(ItemPickerActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(ItemPickerActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
        {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-API-KEY", MainActivity.api_key);
                return params;
            }
        };

        // Get a RequestQueue
        RequestQueue queue = MySingleton.getInstance(ItemPickerActivity.this).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(MainActivity.TAG);

        MySingleton.getInstance(ItemPickerActivity.this).addToRequestQueue(postRequest);
    }*/

    public void getData(){
        progressDialog.show();
        App.getInstance().api.getDescriptions(SERVER_KEY_HASH, userId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject responseJo = response.body();
                    String status = responseJo.get("status").getAsString();
                    if (status.equals("true")) {
                        JsonElement itemDataJe = responseJo.get("item_data");
                        JsonElement descDataJe = responseJo.get("desc_data");
                        Type type1 = new TypeToken<List<EstimateItem>>() {
                        }.getType();
                        Type type2 = new TypeToken<List<Description>>(){}.getType();

                        List<EstimateItem> items;
                        try {
                            items = new Gson().fromJson(itemDataJe, type1);
                        } catch (RuntimeException e) {
                            items = new ArrayList<>();
                        }

                        List<Description> descriptions;
                        try {
                            descriptions = new Gson().fromJson(descDataJe, type2);
                        } catch (RuntimeException e) {
                            descriptions = new ArrayList<>();
                        }

                        array_list_items = items;
                        array_list_descriptions = descriptions;

                        int itemArraySize = items.size();
                        String[] itemArray = new String[itemArraySize+1];
                        itemArray[0] = " - Select an item - ";
                        for (int i = 0; i < itemArraySize; i++) {
                            itemArray[i+1] = items.get(i).Name;
                        }

                        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(EstimateItemPickerActivity.this, R.layout.custom_simple_spinner_item, itemArray);
                        spinner_items.setAdapter(adapter1);

                        int descriptionArraySize = descriptions.size();
                        String[] descriptionsArray = new String[descriptionArraySize+1];
                        descriptionsArray[0] = " - Select a description - ";
                        for (int i = 0; i < descriptionArraySize; i++) {
                            descriptionsArray[i+1] = descriptions.get(i).title;
                        }
                        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(EstimateItemPickerActivity.this, R.layout.custom_simple_spinner_item, descriptionsArray);
                        spinner_descriptions.setAdapter(adapter2);
                        dismissProgress();
                    } else {
                        failed();
                    }

                } else {
                    failed();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                failed();
            }
        });
    }

    private void failed() {
        dismissProgress();
        try {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dismissProgress() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                // If the response is JSONObject instead of expected JSONArray
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
