package com.bytecodr.invoicing.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.model.Client;

import java.util.Date;

import io.realm.Realm;
import io.realm.Sort;

public class NewClientActivity extends AppCompatActivity {
    public static final String TAG = "NewClientActivity";

    private Client currentItem;

    private EditText edit_name;
    private EditText edit_email;
    private EditText edit_address1;
    private EditText edit_address2;
    private EditText edit_city;
    private EditText edit_state;
    private EditText edit_postcode;
    private EditText edit_country;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_client);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);

        //Means user is not logged in
        if (settings == null || settings.getInt("logged_in", 0) == 0 || settings.getString("api_key", "").equals("")) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        edit_name = (EditText) findViewById(R.id.edit_name);
        edit_email = (EditText) findViewById(R.id.edit_email);
        edit_address1 = (EditText) findViewById(R.id.edit_address1);
        edit_address2 = (EditText) findViewById(R.id.edit_address2);
        edit_city = (EditText) findViewById(R.id.edit_city);
        edit_state = (EditText) findViewById(R.id.edit_state);
        edit_postcode = (EditText) findViewById(R.id.edit_postcode);
        edit_country = (EditText) findViewById(R.id.edit_country);

        currentItem = (Client) getIntent().getSerializableExtra("data");

        if (currentItem != null && currentItem.Id > 0) {
            edit_name.setTag(currentItem.Id);
            edit_name.setText(currentItem.Name);
            edit_email.setText(currentItem.Email);
            edit_address1.setText(currentItem.Address1);
            edit_address2.setText(currentItem.Address2);
            edit_city.setText(currentItem.City);
            edit_state.setText(currentItem.State);
            edit_postcode.setText(currentItem.Postcode);
            edit_country.setText(currentItem.Country);

            toolbar.setTitle(currentItem.Name);
        } else {
            toolbar.setTitle(getResources().getString(R.string.title_activity_new_client));
        }

        /*AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_client, menu);

        if (currentItem != null) {
            MenuItem item = menu.findItem(R.id.action_delete);
            if (item != null) {
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
        if (id == R.id.action_save) {
            if (isFormValid()) {
                try (Realm realm = Realm.getDefaultInstance()) {
                    Client client = new Client();

                    String Id = edit_name.getTag().toString();
                    if (Id.equals("0")) {
                        Client client1 = realm.where(Client.class).sort("Id", Sort.ASCENDING).findFirst();
                        if (client1 == null || client1.Id > -1)
                            client.Id = -1;
                        else
                            client.Id = client1.Id - 1;
                    } else
                        client.Id = Long.valueOf(Id);
                    client.Name = edit_name.getText().toString().trim();
                    client.Email = edit_email.getText().toString().trim();
                    client.Address1 = edit_address1.getText().toString().trim();
                    client.Address2 = edit_address2.getText().toString().trim();
                    client.City = edit_city.getText().toString().trim();
                    client.State = edit_state.getText().toString().trim();
                    client.Postcode = edit_postcode.getText().toString().trim();
                    client.Country = edit_country.getText().toString().trim();
                    client.Updated = Integer.parseInt(new Date().getTime() / 1000 + "");
                    client.pendingUpdate = true;

                    realm.executeTransaction(realm1 -> realm1.insertOrUpdate(client));

                    Intent intent = new Intent(NewClientActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("tab", "clients");
                    startActivity(intent);
                    finish();
                }

                return true;
            } else {
                return false;
            }
        } else if (id == R.id.action_delete) {
            new MaterialDialog.Builder(this)
                    .title(R.string.delete)
                    .content(R.string.delete_item)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .cancelable(false)
                    .negativeColorRes(R.color.colorAccent)
                    .positiveColorRes(R.color.colorAccent)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            Object id = edit_name.getTag();

                            if (!id.equals("0")) {
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    Client client = realm.where(Client.class).equalTo("Id", Long.valueOf(id.toString())).findFirst();
                                    if (client != null) {
                                        realm.executeTransaction(realm1 -> {
                                            client.pendingDelete = true;
                                            realm1.insertOrUpdate(client);
                                        });
                                    }

                                    Intent intent = new Intent(NewClientActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("tab", "clients");
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            //Cancel
                            dialog.dismiss();

                            if (dialog != null && dialog.isShowing()) {
                                // If the response is JSONObject instead of expected JSONArray
                                dialog.dismiss();
                            }
                        }
                    })
                    .show();
        } else if (id == android.R.id.home) //Handles the back button, to make sure clients fragment is preselected
        {
            Intent intent = new Intent(NewClientActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("tab", "clients");
            startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isFormValid() {
        boolean isValid = true;

        if (edit_name.getText().toString().trim().length() == 0) {
            edit_name.setError("Name required");
            isValid = false;
        } else {
            edit_name.setError(null);
        }

        String email = edit_email.getText().toString().trim();

        if (email.length() > 0) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edit_email.setError("Valid email required");
                isValid = false;
            } else {
                edit_email.setError(null);
            }
        } else {
            edit_email.setError(null);
        }

        return isValid;
    }
}
