package com.bytecodr.invoicing.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.model.Description;
import com.bytecodr.invoicing.model.Item;
import com.bytecodr.invoicing.network.MySingleton;
import com.bytecodr.invoicing.network.Network;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

import static com.bytecodr.invoicing.App.*;

public class NewDescriptionActivity extends AppCompatActivity {
    public static final String TAG = "NewItemActivity";
    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    private String userId;
    private Description description;

    private EditText etTitle;
    private EditText etDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_description);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);

        //Means user is not logged in
        if (settings == null || settings.getInt("logged_in", 0) == 0 || settings.getString("api_key", "").equals(""))
        {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        userId = String.valueOf(settings.getInt("id", -1));

        progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        etTitle = (EditText)findViewById(R.id.etTitle);
        etDescription = (EditText)findViewById(R.id.etDescription);

        description = (Description) getIntent().getParcelableExtra("data");

        if (description != null && !userId.equals("-1"))
        {
            etTitle.setText(description.title);
            etDescription.setText(description.description);

            toolbar.setTitle(description.title);
        }
        else
        {
            toolbar.setTitle(getResources().getString(R.string.title_activity_new_item));
        }

        /*AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        MySingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_item, menu);

        if (description != null)
        {
            MenuItem item = menu.findItem(R.id.action_delete);
            if (item != null)
            {
                item.setVisible(true);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save)
        {
            if (isFormValid())
            {
                if (description==null) {
                    create(etTitle.getText().toString(), etDescription.getText().toString());
                } else {
                    update(description.id,etTitle.getText().toString(), etDescription.getText().toString());
                }
                return true;
            }
            else
            {
                return false;
            }
        }
        else if (id == R.id.action_delete)
        {
            new MaterialDialog.Builder(this)
                    .title(R.string.delete)
                    .content(R.string.delete_item)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .cancelable(false)
                    .negativeColorRes(R.color.colorAccent)
                    .positiveColorRes(R.color.colorAccent)
                    .callback(new MaterialDialog.ButtonCallback()
                    {
                        @Override
                        public void onPositive(MaterialDialog dialog)
                        {
                            delete(description.id);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog)
                        {
                            //Cancel
                            dialog.dismiss();

                            if (dialog != null && dialog.isShowing())
                            {
                                // If the response is JSONObject instead of expected JSONArray
                                dialog.dismiss();
                            }
                        }
                    })
                    .show();
        }
        else if (id == android.R.id.home) //Handles the back button, to make sure items fragment is preselected
        {
            goToHome();
        }

        return super.onOptionsItemSelected(item);
    }

    private void goToHome() {
        Intent intent = new Intent(NewDescriptionActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("tab", "descriptions");
        startActivity(intent);
        finish();
    }

    public boolean isFormValid() {

        if (etTitle.getText().toString().trim().length() == 0){
            etTitle.setError("Title required");
            return false;
        }
        else
        {
            etTitle.setError(null);
        }

        if (etDescription.getText().toString().trim().length() == 0){
            etDescription.setError("Description required");
            return false;
        }
        else
        {
            etDescription.setError(null);
        }

        return true;
    }

    public void create(String title, String description){
        progressDialog.show();
        getApis().addDescription(SERVER_KEY_HASH, userId, title, description).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject responseJo = response.body();
                    String status = responseJo.get("status").getAsString();
                    if (status.equals("true")) {
                        dismissProgress();
                        goToHome();
                    } else {
                        dismissProgress();
                        failedToAdd();
                    }

                } else {
                    dismissProgress();
                    failedToAdd();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                dismissProgress();
                failedToAdd();
            }
        });
    }

    public void update(String descriptionId, String title, String description){
        progressDialog.show();
        getApis().updateDescription(SERVER_KEY_HASH, userId, descriptionId, title, description).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject responseJo = response.body();
                    String status = responseJo.get("status").getAsString();
                    if (status.equals("true")) {
                        dismissProgress();
                        goToHome();
                    } else {
                        dismissProgress();
                        failedToUpdate();
                    }

                } else {
                    dismissProgress();
                    failedToUpdate();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                dismissProgress();
                failedToUpdate();
            }
        });
    }

    private void delete(String descriptionId) {
        progressDialog.show();
        getApis().deleteDescription(SERVER_KEY_HASH, userId, descriptionId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject responseJo = response.body();
                    String status = responseJo.get("status").getAsString();
                    if (status.equals("true")) {
                        dismissProgress();
                        goToHome();
                    } else {
                        dismissProgress();
                        failedToDelete();
                    }

                } else {
                    dismissProgress();
                    failedToDelete();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                dismissProgress();
                failedToDelete();
            }
        });
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

    private void failedToAdd() {
        try {
            Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void failedToUpdate() {
        try {
            Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void failedToDelete() {
        try {
            Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
