package com.bytecodr.invoicing.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.CommonUtilities;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.network.Network;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "SettingActivity";

    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_NAME = "setting_name";
    public static final String KEY_VALUE = "setting_value";

    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    private Toolbar toolbar;

    private EditText setting_currency_edit;

    private EditText etTax;
    private EditText setting_address1_edit;
    private EditText setting_address2_edit;
    private EditText setting_city_edit;
    private EditText setting_state_edit;
    private EditText setting_postcode_edit;
    private EditText setting_country_edit;

    private MaterialDialog setting_currency_edit_dialog;
    private MaterialDialog setting_address_edit_dialog;
    private MaterialDialog taxDialog;

    private String api_key;
    private Integer user_id;
    private String currency;
    private String address;
    private float tax;

    TextView settings_currency_text;
    TextView settings_name_text;
    TextView settings_address_text;
    TextView currency_format_example;
    TextView tvTaxPreview;

    public static final String KEY_ADDRESS = "KEY_ADDRESS";
    public static final String KEY_CURRENCY_SYMBOL = "KEY_CURRENCY_SYMBOL";
    public static final String KEY_VAT = "KEY_VAT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);

        //Checks whether a user is logged in, otherwise redirects to the Login screen
        if (settings == null || settings.getInt("logged_in", 0) == 0 || settings.getString("api_key", "").equals(""))
        {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        api_key = settings.getString("api_key", "");
        user_id = settings.getInt("id", 0);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_settings);



        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();



        address = settings.getString(KEY_ADDRESS, null);
        currency = settings.getString(KEY_CURRENCY_SYMBOL, "$");
        tax = settings.getFloat(KEY_VAT, 0);

        LinearLayout settings_address_layout = (LinearLayout) findViewById(R.id.settings_address_layout);
        LinearLayout settings_currency_layout = (LinearLayout) findViewById(R.id.settings_currency_layout);
        LinearLayout llTax = (LinearLayout) findViewById(R.id.llTax);
        LinearLayout settings_logo_layout = (LinearLayout) findViewById(R.id.settings_logo_layout);
        Button settings_logout_button = (Button) findViewById(R.id.settings_logout_button);

        settings_address_layout.setOnClickListener(this);
        settings_currency_layout.setOnClickListener(this);
        llTax.setOnClickListener(this);
        settings_logo_layout.setOnClickListener(this);
        settings_logout_button.setOnClickListener(this);

        settings_address_text = (TextView) findViewById(R.id.settings_address_text);
        settings_address_text.setText(getAddress());
        settings_currency_text = (TextView) findViewById(R.id.settings_currency_text);
        settings_currency_text.setText(currency);
        tvTaxPreview = (TextView) findViewById(R.id.tvTaxPreview);
        tvTaxPreview.setText(String.valueOf(CommonUtilities.round(tax, 2)));



        /*AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/


    }

    public String getAddress() {
        try {
            String[] addressArray = address.split("\\|\\#", -1);
            String newStringValue = "";

            for (String anAddressArray : addressArray) {
                String addressValue = anAddressArray.trim();

                if (!addressValue.equals("")) newStringValue += addressValue + ", ";
            }

            return newStringValue.replaceAll(", $", "");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId())
        {
            case R.id.settings_address_layout:
                setting_address_edit_dialog = new MaterialDialog.Builder(SettingActivity.this)
                        .customView(R.layout.setting_address_edit_layout, true)
                        .positiveText("OK")
                        .cancelable(false)
                        .negativeText("Cancel")
                        .showListener(dialog -> {
                            View view = setting_address_edit_dialog.getCustomView();

                            setting_address1_edit = (EditText) view.findViewById(R.id.setting_address1_edit);
                            setting_address2_edit = (EditText) view.findViewById(R.id.setting_address2_edit);
                            setting_city_edit = (EditText) view.findViewById(R.id.setting_city_edit);
                            setting_state_edit = (EditText) view.findViewById(R.id.setting_state_edit);
                            setting_postcode_edit = (EditText) view.findViewById(R.id.setting_postcode_edit);
                            setting_country_edit = (EditText) view.findViewById(R.id.setting_country_edit);

                            String[] currentAddress = new String[0];
                            try {
                                currentAddress = address.split("\\|\\#", -1);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return;
                            }

                            setting_address1_edit.setText(currentAddress[0]);
                            setting_address2_edit.setText(currentAddress[1]);
                            setting_city_edit.setText(currentAddress[2]);
                            setting_state_edit.setText(currentAddress[3]);
                            setting_postcode_edit.setText(currentAddress[4]);
                            setting_country_edit.setText(currentAddress[5]);
                        })
                        .callback(new MaterialDialog.ButtonCallback()
                        {
                            @Override
                            public void onPositive(MaterialDialog dialog)
                            {
                                try
                                {
                                    api_parameter = new JSONObject();
                                    api_parameter.put(KEY_USER_ID, user_id);
                                    api_parameter.put(KEY_NAME, KEY_ADDRESS);

                                    String address = setting_address1_edit.getText().toString() + "|#" + setting_address2_edit.getText().toString()+ "|#" + setting_city_edit.getText().toString()+ "|#" + setting_state_edit.getText().toString()+ "|#" + setting_postcode_edit.getText().toString()+ "|#" + setting_country_edit.getText().toString();
                                    api_parameter.put(KEY_VALUE, address);

                                }
                                catch(JSONException ex) {       }

                                RunUpdateSettingsService();
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog)
                            {
                                if (dialog != null && dialog.isShowing()) {
                                    // If the response is JSONObject instead of expected JSONArray
                                    dialog.dismiss();
                                }
                            }
                        })
                        .show();
                break;
            case R.id.settings_currency_layout:
                setting_currency_edit_dialog = new MaterialDialog.Builder(SettingActivity.this)
                        .customView(R.layout.setting_currency_edit_layout, true)
                        .positiveText("OK")
                        .cancelable(false)
                        .negativeText("Cancel")
                        .showListener(dialog -> {
                            View view = setting_currency_edit_dialog.getCustomView();

                            setting_currency_edit = (EditText) view.findViewById(R.id.setting_one_edit);
                            setting_currency_edit.setText(currency);

                            setting_currency_edit.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {

                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    currency_format_example.setText(s.toString() + "100.00");
                                }
                            });

                            currency_format_example = (TextView) view.findViewById(R.id.currency_format_example);
                            currency_format_example.setText(currency + "100.00");
                        })
                        .callback(new MaterialDialog.ButtonCallback()
                        {
                            @Override
                            public void onPositive(MaterialDialog dialog)
                            {
                                try
                                {
                                    api_parameter = new JSONObject();
                                    api_parameter.put(KEY_USER_ID, user_id);
                                    api_parameter.put(KEY_NAME, KEY_CURRENCY_SYMBOL);
                                    api_parameter.put(KEY_VALUE, setting_currency_edit.getText().toString());

                                }
                                catch(JSONException ex) {       }

                                RunUpdateSettingsService();
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog)
                            {
                                //Cancel
                                if (dialog != null && dialog.isShowing()) {
                                    // If the response is JSONObject instead of expected JSONArray
                                    dialog.dismiss();
                                }
                            }
                        })
                        .show();
                break;
            case R.id.llTax: {
                taxDialog = new MaterialDialog.Builder(SettingActivity.this)
                        .customView(R.layout.setting_tax_layout, true)
                        .positiveText("OK")
                        .cancelable(false)
                        .negativeText("Cancel")
                        .showListener(dialog -> {
                            View view = taxDialog.getCustomView();
                            etTax = (EditText) view.findViewById(R.id.etTax);
                            etTax.setText(String.valueOf(CommonUtilities.round(tax, 2)));
                        })
                        .callback(new MaterialDialog.ButtonCallback()
                        {
                            @Override
                            public void onPositive(MaterialDialog dialog)
                            {

                                try
                                {
                                    api_parameter = new JSONObject();
                                    api_parameter.put(KEY_USER_ID, user_id);
                                    api_parameter.put(KEY_NAME, KEY_VAT);
                                    api_parameter.put(KEY_VALUE, etTax.getText().toString());

                                }
                                catch(JSONException ex) {       }

                                RunUpdateSettingsService();
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog)
                            {
                                //Cancel
                                if (dialog != null && dialog.isShowing()) {
                                    // If the response is JSONObject instead of expected JSONArray
                                    dialog.dismiss();
                                }
                            }
                        })
                        .show();
                break;
            }
            case R.id.settings_logo_layout:
                Intent intent = new Intent(SettingActivity.this, SettingLogoActivity.class);
                startActivity(intent);
                break;
            case R.id.settings_logout_button:
                SharedPreferences.Editor settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE).edit();
                settings.clear();
                settings.commit();
                Intent settingsIntent = new Intent(SettingActivity.this, LoginActivity.class);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settingsIntent);
                finish();
                break;

        }
    }

    public void RunUpdateSettingsService() {
        progressDialog.show();
        VolleyLog.d("settings/update", api_parameter.toString());

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "settings/update", api_parameter, response -> {
                    try {
                        JSONObject data = ((JSONObject) response.get("data"));

                        String setting_name = data.getString("setting_name");
                        String setting_value = data.getString("setting_value");

                        SharedPreferences.Editor settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE).edit();

                        if (setting_name.equals(KEY_ADDRESS)) {
                            settings.putString(KEY_ADDRESS, setting_value);
                            address = setting_value;
                            settings_address_text.setText(getAddress());
                        } else if (setting_name.equals(KEY_CURRENCY_SYMBOL)) {
                            settings_currency_text.setText(setting_value);
                            currency = setting_value;
                            settings.putString(KEY_CURRENCY_SYMBOL, setting_value);
                        } else if (setting_name.equals(KEY_VAT)) {
                            tvTaxPreview.setText(setting_value);
                            tax = Float.parseFloat(setting_value);
                            settings.putFloat(KEY_VAT, tax);
                        }

                        settings.commit();
                    } catch (JSONException ex) {
                    }

                    if (progressDialog != null && progressDialog.isShowing()) {
                        // If the response is JSONObject instead of expected JSONArray
                        progressDialog.dismiss();
                    }
                }, error -> {
                    NetworkResponse response = error.networkResponse;
                    if (response != null && response.data != null) {
                        try {
                            JSONObject json = new JSONObject(new String(response.data));
                            Toast.makeText(SettingActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(SettingActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(SettingActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
                    }
                })
        {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("X-API-KEY", MainActivity.api_key);
                return params;
            }
        };

        App.getInstance().addToRequestQueue(postRequest);
    }

}
