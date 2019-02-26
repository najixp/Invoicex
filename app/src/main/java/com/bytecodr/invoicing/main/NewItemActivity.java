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
import com.bytecodr.invoicing.model.Item;

import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;
import io.realm.Sort;

public class NewItemActivity extends AppCompatActivity {
    public static final String TAG = "NewItemActivity";
    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    private Item currentItem;

    private EditText edit_name;
    private EditText edit_description;
    private EditText edit_rate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);

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

        progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        edit_name = (EditText) findViewById(R.id.edit_name);
        edit_description = (EditText) findViewById(R.id.edit_description);
        edit_rate = (EditText) findViewById(R.id.edit_rate);

        currentItem = (Item) getIntent().getSerializableExtra("data");

        if (currentItem != null && currentItem.Id > 0) {
            edit_name.setTag(currentItem.Id);
            edit_name.setText(currentItem.Name);
            edit_description.setText(currentItem.Description);
            edit_rate.setText(String.format("%.2f", currentItem.Rate));

            toolbar.setTitle(currentItem.Name);
        } else {
            toolbar.setTitle(getResources().getString(R.string.title_activity_new_item));
        }

        /*AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_item, menu);

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
                    Item item1 = new Item();

                    String Id = edit_name.getTag().toString();
                    if (Id.equals("0")) {
                        Item item2 = realm.where(Item.class).sort("Id", Sort.ASCENDING).findFirst();
                        if (item2 == null || item2.Id > -1)
                            item1.Id = -1;
                        else
                            item1.Id = item2.Id - 1;
                    } else
                        item1.Id = Long.valueOf(Id);


                    item1.Name = edit_name.getText().toString().trim();
                    try {
                        item1.Rate = Double.valueOf(edit_rate.getText().toString().trim().replace(',', '.'));
                    } catch (NumberFormatException e) {

                    }
                    item1.Description = edit_description.getText().toString().trim();
                    item1.Updated = Integer.parseInt(new Date().getTime() / 1000 + "");
                    item1.pendingUpdate = true;

                    realm.executeTransaction(realm1 -> realm1.insertOrUpdate(item1));

                    Intent intent = new Intent(NewItemActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("tab", "items");
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
                                    Item item1 = realm.where(Item.class).equalTo("Id", Long.valueOf(id.toString())).findFirst();
                                    if (item1 != null) {
                                        realm.executeTransaction(realm1 -> {
                                                    item1.pendingDelete = true;
                                                    realm1.insertOrUpdate(item1);
                                                }
                                        );
                                    }

                                    Intent intent = new Intent(NewItemActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("tab", "items");
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
        } else if (id == android.R.id.home) //Handles the back button, to make sure items fragment is preselected
        {
            Intent intent = new Intent(NewItemActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("tab", "items");
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

        return isValid;
    }
}
