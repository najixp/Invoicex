package com.bytecodr.invoicing.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.network.Network;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String SESSION_USER = "User";

    private EditText editEmail;
    private EditText editPassword;

    public static final String TAG = "LoginActivity";
    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        TextView txtSignUp = (TextView) findViewById(R.id.txtSignUp);
        txtSignUp.setOnClickListener(this);

        editEmail = (EditText) findViewById(R.id.editEmail);
        editPassword = (EditText) findViewById(R.id.editPassword);

        Button buttonLogin = (Button) findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.txtSignUp:
                intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                break;
            case R.id.buttonLogin:
                if (isFormValid())
                {
                    api_parameter = new JSONObject();
                    try
                    {
                        api_parameter.put("email", editEmail.getText().toString().trim());
                        api_parameter.put("password", editPassword.getText().toString().trim());
                    }
                    catch (JSONException e)   {}

                    RunLoginService();
                }

                break;
            default:
                break;
        }
    }

    public boolean isFormValid()
    {
        boolean isValid = true;

        if (editEmail.getText().toString().trim().length() == 0){
            editEmail.setError("Email required");
            isValid = false;
        }
        else
        {
            editEmail.setError(null);
        }

        if (editPassword.getText().toString().trim().length() == 0){
            editPassword.setError("Password required");
            isValid = false;
        }
        else
        {
            editPassword.setError(null);
        }

        return isValid;
    }

    public void RunLoginService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "auth/login", api_parameter, response -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        // If the response is JSONObject instead of expected JSONArray
                        progressDialog.dismiss();
                    }

                    try {
                        JSONObject data = ((JSONObject) response.get("data"));

                        SharedPreferences.Editor user = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE).edit();
                        Log.e("RESPONSE", data.toString());

                        user.putInt("id", data.getInt("id"));
                        user.putInt("logged_in", 1);
                        user.putString("firstname", data.getString("first_name"));
                        user.putString("lastname", data.getString("last_name"));
                        user.putString("email", data.getString("email"));
                        user.putString("api_key", data.getString("api_key"));

                        JsonElement jsonElement = new Gson().fromJson(data.getString("content"), JsonElement.class);
                        JsonObject jsonObject = jsonElement.getAsJsonObject();

                        try {
                            user.putString(SettingActivity.KEY_ADDRESS, jsonObject.get(SettingActivity.KEY_ADDRESS).getAsString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            user.putString(SettingActivity.KEY_CURRENCY_SYMBOL, jsonObject.get(SettingActivity.KEY_CURRENCY_SYMBOL).getAsString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            user.putFloat(SettingActivity.KEY_VAT, jsonObject.get(SettingActivity.KEY_VAT).getAsFloat());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        user.commit();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        finish();
                        startActivity(intent);
                    } catch (Exception ex) {
                        Toast.makeText(LoginActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    // TODO Auto-generated method stub
                    if (progressDialog != null && progressDialog.isShowing()) {
                        // If the response is JSONObject instead of expected JSONArray
                        progressDialog.dismiss();
                    }

                    NetworkResponse response = error.networkResponse;
                    if (response != null && response.data != null) {
                        try {
                            JSONObject json = new JSONObject(new String(response.data));
                            Toast.makeText(LoginActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(LoginActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
                    }
                })
        {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("X-API-KEY", Network.API_KEY);
                params.put("Authorization",
                        "Basic " + Base64.encodeToString(
                                (editEmail.getText().toString().trim() + ":" + editPassword.getText().toString().trim()).getBytes(), Base64.NO_WRAP)
                );
                return params;
            }
        };

        App.getInstance().addToRequestQueue(postRequest);
    }
}
