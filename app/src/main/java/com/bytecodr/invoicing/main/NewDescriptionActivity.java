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
import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.model.Description;

import io.realm.Realm;
import io.realm.Sort;

public class NewDescriptionActivity extends AppCompatActivity {
    public static final String TAG = "NewItemActivity";

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
        if (settings == null || settings.getInt("logged_in", 0) == 0 || settings.getString("api_key", "").equals("")) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        userId = String.valueOf(settings.getInt("id", -1));

        etTitle = (EditText) findViewById(R.id.etTitle);
        etDescription = (EditText) findViewById(R.id.etDescription);

        description = getIntent().getParcelableExtra("data");

        if (description != null && !userId.equals("-1")) {
            etTitle.setText(description.title);
            etDescription.setText(description.description);

            toolbar.setTitle(description.title);
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

        if (description != null) {
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
                if (description == null) {
                    try (Realm realm = Realm.getDefaultInstance()) {
                        Description description1 = new Description();

                        Description description2 = realm.where(Description.class).sort("id", Sort.ASCENDING).findFirst();
                        if (description2 == null || description2.id > -1)
                            description1.id = -1;
                        else
                            description1.id = description2.id - 1;
                        description1.title = etTitle.getText().toString();
                        description1.description = etDescription.getText().toString();
                        description1.pendingUpdate = true;

                        realm.executeTransaction(realm12 -> realm12.insertOrUpdate(description1));
                    }
                } else {
                    try (Realm realm = Realm.getDefaultInstance()) {
                        Description description1 = realm.where(Description.class).equalTo("id", description.id).findFirst();
                        if (description1 != null) {
                            realm.executeTransaction(realm1 -> {
                                description1.title = etTitle.getText().toString();
                                description1.description = etDescription.getText().toString();
                                description1.pendingUpdate = true;
                                realm1.insertOrUpdate(description1);
                            });
                            App.getInstance().updateData();
                        }
                    }
                }
                Intent intent = new Intent(NewDescriptionActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("tab", "descriptions");
                startActivity(intent);
                finish();
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
                            try (Realm realm = Realm.getDefaultInstance()) {
                                Description description1 = realm.where(Description.class).equalTo("id", description.id).findFirst();
                                if (description1 != null) {
                                    realm.executeTransaction(realm1 -> {
                                        description1.pendingDelete = true;
                                        realm1.insertOrUpdate(description1);
                                    });
                                    App.getInstance().updateData();
                                }

                                Intent intent = new Intent(NewDescriptionActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("tab", "descriptions");
                                startActivity(intent);
                                finish();
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
            Intent intent = new Intent(NewDescriptionActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("tab", "descriptions");
            startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isFormValid() {

        if (etTitle.getText().toString().trim().length() == 0) {
            etTitle.setError("Title required");
            return false;
        } else {
            etTitle.setError(null);
        }

        if (etDescription.getText().toString().trim().length() == 0) {
            etDescription.setError("Description required");
            return false;
        } else {
            etDescription.setError(null);
        }

        return true;
    }
}
