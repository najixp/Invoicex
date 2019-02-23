package com.bytecodr.invoicing.main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.BuildConfig;
import com.bytecodr.invoicing.CommonUtilities;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.helper.helper_number;
import com.bytecodr.invoicing.model.Client;
import com.bytecodr.invoicing.model.Estimate;
import com.bytecodr.invoicing.model.EstimateItem;
import com.bytecodr.invoicing.model.StringPreference;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.rey.material.widget.Spinner;
import com.rey.material.widget.Switch;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.Sort;

import static com.bytecodr.invoicing.main.LoginActivity.SESSION_USER;
import static com.itextpdf.text.pdf.ColumnText.AR_LIG;

public class NewPurchaseActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    public static final String TAG = "NewEstimateActivity";

    private static final int SIZE_TEXT_TITLE_PAGE = 22;
    private static final int SIZE_TEXT_HEADER_PAGE = 14;
    private static final int SIZE_TEXT_HEADER_TABLE = 14;
    private static final int SIZE_TEXT_TOTAL = 14;
    private static final int SIZE_TEXT_NORMAL = 14;

    private Estimate currentEstimate;
    private Client currentClient;

    private ArrayList<Client> array_list_clients;
    Spinner spinner_client;

    private EditText edit_purchase_number;
    private EditText edit_tax_rate;
    private EditText edit_client_notes;
    private EditText edit_purchase_date;
    private EditText edit_purchase_due_date;

    private TextView text_subtotal_amount;
    private TextView text_tax_amount;
    private TextView text_total_amount;

    private DatePickerDialog datePicker;

    private Switch switch_payment_received;

    private ArrayList<EstimateItem> array_list_items;
    private EstimateItemAdapter adapter_item;
    private ListView list_items;

    static final int ITEM_PICKER_TAG = 1;

    private String currency;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * INVOICE SETUP
     **/
    private BaseFont bfBold;
    private BaseFont bf;
    private int pageNumber = 0;
    private double Subtotal;
    private double Tax;

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_purchase);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        SharedPreferences settings = getSharedPreferences(SESSION_USER, MODE_PRIVATE);

        //Means user is not logged in
        if (settings == null || settings.getInt("logged_in", 0) == 0 || settings.getString("api_key", "").equals("")) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= 23) verifyStoragePermissions(this);

        currency = settings.getString(SettingActivity.KEY_CURRENCY_SYMBOL, "$");

        edit_purchase_number = (EditText) findViewById(R.id.edit_purchase_number);
        edit_tax_rate = (EditText) findViewById(R.id.edit_tax_rate);
        edit_client_notes = (EditText) findViewById(R.id.edit_client_notes);
        edit_purchase_date = (EditText) findViewById(R.id.edit_purchase_date);
        edit_purchase_due_date = (EditText) findViewById(R.id.edit_purchase_due_date);

        text_subtotal_amount = (TextView) findViewById(R.id.text_subtotal_amount);
        text_tax_amount = (TextView) findViewById(R.id.text_tax_amount);
        text_total_amount = (TextView) findViewById(R.id.text_total_amount);

        text_subtotal_amount.setText(currency + helper_number.round(0));
        text_tax_amount.setText(currency + helper_number.round(0));
        text_total_amount.setText(currency + helper_number.round(0));

        switch_payment_received = (Switch) findViewById(R.id.switch_payment_received);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MMM yyyy");
        Calendar calendar = Calendar.getInstance();

        datePicker = DatePickerDialog.newInstance(
                NewPurchaseActivity.this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.vibrate(false);

        currentEstimate = (Estimate) getIntent().getSerializableExtra("data");
        currentClient = new Client();

        if (currentEstimate != null && currentEstimate.Id > 0) {
            edit_purchase_number.setTag(currentEstimate.Id);
            edit_purchase_number.setText(currentEstimate.getEstimateNumberFormatted());
            edit_tax_rate.setText(String.format("%.2f", currentEstimate.TaxRate));
            edit_client_notes.setText(currentEstimate.ClientNote);

            if (currentEstimate.EstimateDate != 0)
                edit_purchase_date.setText(dateFormat.format(currentEstimate.getEstimateDate()));
            if (currentEstimate.EstimateDueDate != 0)
                edit_purchase_due_date.setText(dateFormat.format(currentEstimate.getEstimateDueDate()));

            toolbar.setTitle(currentEstimate.getPurchaseName());
        } else {
            try (Realm realm = Realm.getDefaultInstance()) {
                Estimate estimate1 = realm.where(Estimate.class).sort("Id", Sort.ASCENDING).findFirst();
                if (estimate1 == null || estimate1.Id > -1)
                    edit_purchase_number.setTag(-1);
                else
                    edit_purchase_number.setTag(estimate1.Id - 1);
            }
            edit_purchase_date.setText(dateFormat.format(calendar.getTime()));
            toolbar.setTitle(getResources().getString(R.string.title_activity_new_purchase));
        }

        array_list_clients = new ArrayList<>();
        array_list_items = new ArrayList<>();

        list_items = (ListView) findViewById(R.id.list_item);
        adapter_item = new EstimateItemAdapter(NewPurchaseActivity.this, array_list_items);
        list_items.setAdapter(adapter_item);
        list_items.setOnItemClickListener((arg0, arg1, position, arg3) -> {

            EstimateItem item = array_list_items.get(position);
            Intent intent = new Intent(NewPurchaseActivity.this, EstimateItemPickerActivity.class);
            intent.putExtra("data", item);
            intent.putExtra("position", position);
            startActivityForResult(intent, ITEM_PICKER_TAG);
        });

        spinner_client = (Spinner) findViewById(R.id.spinner_client);

        edit_purchase_date.setOnClickListener(v -> {
            if (!datePicker.isAdded())
                datePicker.show(getFragmentManager(), "estimate_date");
        });

        edit_purchase_due_date.setOnClickListener(v -> {
            if (!datePicker.isAdded())
                datePicker.show(getFragmentManager(), "estimate_due_date");
        });

        edit_tax_rate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                calculate_total();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_item_button);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(NewPurchaseActivity.this, EstimateItemPickerActivity.class);
            startActivityForResult(intent, ITEM_PICKER_TAG);
        });

        FloatingActionButton fab1 = (FloatingActionButton) findViewById(R.id.printOut);
        fab1.setOnClickListener(view -> {

            String path = downloadPDF();

            if (path.length() == 0) {
                Toast.makeText(NewPurchaseActivity.this, getResources().getString(R.string
                        .pdf_not_created), Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(FileProvider.getUriForFile(NewPurchaseActivity.this, BuildConfig.APPLICATION_ID + ".provider", new File(path)), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }


        });

        /*AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        try (Realm realm = Realm.getDefaultInstance()) {
            List<Client> clients = realm.copyFromRealm(realm.where(Client.class).equalTo("pendingDelete", false).greaterThan("Id", 0).findAll());

            array_list_clients.clear();
            List<String> array_clients = new ArrayList<>();

            Integer selected_client_index = 0;

            for (int i = 0; i < clients.size(); i++) {
                Client client = clients.get(i);
                array_list_clients.add(client);
                array_clients.add(client.Name);

                if (currentEstimate != null && currentEstimate.Id > 0 && currentEstimate.ClientId == client.Id) {
                    selected_client_index = i;
                    currentClient = client;
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(NewPurchaseActivity.this, R.layout.custom_simple_spinner_item, array_clients);
            spinner_client.setAdapter(adapter);
            spinner_client.setSelection(selected_client_index);

            if (currentEstimate != null)
                array_list_items.addAll(realm.copyFromRealm(realm.where(EstimateItem.class).equalTo("EstimateId", currentEstimate.Id).findAll()));

            calculate_total();
            setListViewHeightBasedOnChildren(list_items);
        }

        if (currentEstimate == null) {
            final SharedPreferences preference = getSharedPreferences(SESSION_USER, MODE_PRIVATE);
            float tax = preference.getFloat(SettingActivity.KEY_VAT, 0);
            edit_tax_rate.setText((String.valueOf(CommonUtilities.round(tax, 2))));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_purchase, menu);

        if (currentEstimate != null) {
            MenuItem item = menu.findItem(R.id.action_delete);
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.printOut);
            if (item != null) {
                item.setVisible(true);
            }

            /*MenuItem item_convert_invoice = menu.findItem(R.id.action_convert_to_invoice);
            if (item_convert_invoice != null)
            {
                item_convert_invoice.setVisible(true);
            }*/

            item = menu.findItem(R.id.action_download);
            if (item != null) {
                item.setVisible(true);
                findViewById(R.id.printOut).setVisibility(View.VISIBLE);
            }

            item = menu.findItem(R.id.action_email);
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
                if (isEstimateFormValid()) {
                    try (Realm realm = Realm.getDefaultInstance()) {
                        Estimate estimate = new Estimate();

                        estimate.Id = Long.valueOf(edit_purchase_number.getTag().toString());

                        try {
                            estimate.EstimateNumber = Long.valueOf(edit_purchase_number.getText().toString().trim());
                        } catch (NumberFormatException e) {
                        }
                        try {
                            estimate.TaxRate = Double.valueOf(edit_tax_rate.getText().toString().trim());
                        } catch (NumberFormatException e) {
                        }
                        estimate.ClientId = array_list_clients.get(spinner_client.getSelectedItemPosition()).Id;
                        estimate.ClientName = array_list_clients.get(spinner_client.getSelectedItemPosition()).Name;
                        estimate.ClientNote = edit_client_notes.getText().toString().trim();

                        String estimateDateString = edit_purchase_date.getText().toString().trim();
                        String estimateDueDateString = edit_purchase_due_date.getText().toString().trim();

                        SimpleDateFormat sdf = new SimpleDateFormat("dd. MMM yyyy");
                        Date estimateDate = null;
                        Date estimateDueDate = null;
                        try {
                            estimateDate = sdf.parse(estimateDateString);
                            estimateDueDate = sdf.parse(estimateDueDateString);
                        } catch (ParseException e) {
                        }
                        if (estimateDate != null)
                            estimate.EstimateDate = Integer.parseInt(estimateDate.getTime() / 1000 + "");
                        if (estimateDueDate != null)
                            estimate.EstimateDueDate = Integer.parseInt(estimateDueDate.getTime() / 1000 + "");
                        estimate.pendingUpdate = true;

                        realm.executeTransaction(realm1 -> {
                            realm1.insertOrUpdate(estimate);
                            if (currentEstimate != null) {
                                List<EstimateItem> dbItems = realm1.where(EstimateItem.class).equalTo("EstimateId", currentEstimate.Id).findAll();
                                for (EstimateItem dbItem : dbItems) {
                                    boolean found = false;
                                    for (EstimateItem listItem : array_list_items) {
                                        if (listItem.Id == dbItem.Id) {
                                            realm1.insertOrUpdate(listItem);
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        realm1.insertOrUpdate(dbItem);
                                    }
                                }
                                for (EstimateItem listItem : array_list_items) {
                                    boolean found = false;
                                    for (EstimateItem dbItem : dbItems) {
                                        if (listItem.Id == dbItem.Id) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found)
                                        realm1.insertOrUpdate(listItem);
                                }
                            } else
                                realm1.insertOrUpdate(array_list_items);
                        });

                        App.getInstance().updateData();

                        Intent intent = new Intent(NewPurchaseActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("tab", "purchases");
                        startActivity(intent);
                        finish();
                    }
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
                            //Delete
                            Object id = edit_purchase_number.getTag();

                            if (!id.equals("0")) {
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    Estimate estimate = realm.where(Estimate.class).equalTo("Id", Long.valueOf(id.toString())).findFirst();
                                    if (estimate != null) {
                                        realm.executeTransaction(realm1 -> {
                                            estimate.pendingDelete = true;
                                            realm1.insertOrUpdate(estimate);
                                        });
                                        App.getInstance().updateData();
                                    }

                                    Intent intent = new Intent(NewPurchaseActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("tab", "purchases");
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
        } else if (id == R.id.action_download) {
            final String path = downloadPDF();

            if (path.length() == 0) {
                Toast.makeText(NewPurchaseActivity.this, getResources().getString(R.string.pdf_not_created), Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(path)), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        } else if (id == R.id.action_email) {
            String path = downloadPDF();

            SharedPreferences settings = getSharedPreferences(SESSION_USER, MODE_PRIVATE);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{currentClient.Email});

            emailIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(getResources().getString(R.string.estimate_email_subject), currentEstimate.getEstimateNumberFormatted(), settings.getString("firstname", ""), settings.getString("lastname", "")));
            emailIntent.putExtra(Intent.EXTRA_TEXT, String.format(getResources().getString(R.string.estimate_email_text), currentClient.Name, currentEstimate.getPurchaseName(), settings.getString("firstname", ""), settings.getString("lastname", "")));

            emailIntent.setType("text/plain");
            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(path));
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        } else if (id == android.R.id.home) //Handles the back button, to make sure clients fragment is preselected
        {
            Intent intent = new Intent(NewPurchaseActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("tab", "purchases");
            startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isEstimateFormValid() {
        boolean isValid = true;

        if (edit_purchase_number.getText().toString().trim().length() == 0) {
            edit_purchase_number.setError(getResources().getString(R.string.estimate_number_required));
            isValid = false;
        } else
            edit_purchase_number.setError(null);

        if (spinner_client.getSelectedItemPosition() == -1 || array_list_clients.size() == 0) {
            Toast.makeText(NewPurchaseActivity.this, getResources().getString(R.string.client_required), Toast.LENGTH_LONG).show();
            isValid = false;
        }

        if (array_list_items.size() == 0) {
            Toast.makeText(NewPurchaseActivity.this, getResources().getString(R.string.items_required), Toast.LENGTH_LONG).show();
            isValid = false;
        }

        return isValid;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ITEM_PICKER_TAG) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                EstimateItem item = (EstimateItem) data.getSerializableExtra("data");

                item.EstimateId = Long.valueOf(edit_purchase_number.getTag().toString());

                Integer position = data.getIntExtra("position", -1);

                if (position > -1) {
                    EstimateItem existingItem = array_list_items.get(position);
                    existingItem.Name = item.Name;
                    existingItem.Description = item.Description;
                    existingItem.Rate = item.Rate;
                    existingItem.Quantity = item.Quantity;
                } else {
                    array_list_items.add(item);
                }

                adapter_item.notifyDataSetChanged();

                calculate_total();

                setListViewHeightBasedOnChildren(list_items);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        if (view.getTag().equals("estimate_date"))
            edit_purchase_date.setText(dayOfMonth + ". " + helper_number.getMonthName(monthOfYear) + " " + year);
        else
            edit_purchase_due_date.setText(dayOfMonth + ". " + helper_number.getMonthName(monthOfYear) + " " + year);
    }

    public boolean isFormValid() {
        boolean isValid = true;

        if (edit_purchase_number.getText().toString().trim().length() == 0) {
            edit_purchase_number.setError(getResources().getString(R.string.estimate_number_required));
            isValid = false;
        } else
            edit_purchase_number.setError(null);

        if (spinner_client.getSelectedItemPosition() == -1) {
            Toast.makeText(NewPurchaseActivity.this, getResources().getString(R.string.client_required), Toast.LENGTH_LONG).show();
            return false;
        }

        return isValid;
    }

    public void calculate_total() {
        double subtotal = 0;

        for (int i = 0; i < array_list_items.size(); i++) {
            EstimateItem item = array_list_items.get(i);

            subtotal = subtotal + (item.Rate * item.Quantity);
        }

        double tax_rate = 0;

        try {
            if (edit_tax_rate.getText().toString().length() > 0) {
                tax_rate = Double.parseDouble(edit_tax_rate.getText().toString());
            }
        } catch (Exception ex) {
        }

        double tax = (tax_rate * subtotal) / 100;

        text_subtotal_amount.setText(currency + helper_number.round(subtotal));
        text_tax_amount.setText(currency + helper_number.round(tax));
        text_total_amount.setText(currency + helper_number.round(subtotal + tax));
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public class EstimateItemAdapter extends ArrayAdapter<EstimateItem> {
        private final Context context;
        private final ArrayList<EstimateItem> values;
        private String currency;

        public EstimateItemAdapter(Context context, ArrayList<EstimateItem> values) {
            super(context, R.layout.layout_estimate_item_row, values);

            this.context = context;
            this.values = values;

            SharedPreferences settings = context.getSharedPreferences(SESSION_USER, MODE_PRIVATE);
            currency = settings.getString(SettingActivity.KEY_CURRENCY_SYMBOL, "$");
        }

        public EstimateItem getItem(int position) {
            return values.get(position);
        }

        /**
         * Here we go and get our rowlayout.xml file and set the textview text.
         * This happens for every row in your listview.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(R.layout.layout_estimate_item_row, parent, false);

            EstimateItem item = values.get(position);

            TextView text_name = (TextView) rowView.findViewById(R.id.text_name);
            TextView text_rate = (TextView) rowView.findViewById(R.id.text_rate);
            TextView text_quantity = (TextView) rowView.findViewById(R.id.text_quantity);
            ImageView image_remove_item = (ImageView) rowView.findViewById(R.id.image_remove_item);
            image_remove_item.setTag(position);

            text_name.setText(item.Name + "  -  " + item.Description);
            text_rate.setText(currency + String.format("%.2f", item.Quantity * item.Rate));
            text_quantity.setText(item.Quantity + " x " + String.format("%.2f", item.Rate));

            image_remove_item.setOnClickListener(v -> {
                array_list_items.remove(Integer.parseInt(v.getTag().toString()));
                adapter_item.notifyDataSetChanged();
                calculate_total();
                setListViewHeightBasedOnChildren(list_items);
            });

            return rowView;
        }
    }

    public String downloadPDF() {
        return createPDF(currentEstimate.getPurchaseName() + ".pdf");
    }

    private Integer address1_start = 700;
    private Integer address2_start = 575;
    private Integer address_height = SIZE_TEXT_HEADER_PAGE + 3;

    private Integer line_heading_start = 435;
    private Integer line_item_start = 400;
    private Integer line_item_height = 28;

    private BaseColor color_light_grey = new BaseColor(0, 0, 0);
    private BaseColor color_invoice_header_background = new BaseColor(255, 255, 255);

    private String createPDF(String pdfFilename) {

        pageNumber = 0;
        Subtotal = 0;
        Tax = 0;
        Document doc = new Document();
        PdfWriter docWriter = null;
        initializeFonts();

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + pdfFilename;

        try {
            docWriter = PdfWriter.getInstance(doc, new FileOutputStream(path));
            docWriter.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

            doc.addAuthor("Bytecodr");
            doc.addLanguage("ar-SA");
            doc.addCreationDate();
            doc.addProducer();
            doc.addTitle(currentEstimate.getPurchaseName());
            doc.setPageSize(PageSize.LETTER);

            doc.open();
            PdfContentByte cb = docWriter.getDirectContent();

            boolean beginPage = true;
            int y = 0;

            for (int i = 0; i < array_list_items.size(); i++) {
                if (beginPage) {
                    beginPage = false;
                    generateLayout(doc, cb);
                    generateHeader(doc, cb);
                    y = line_item_start;
                }
                generateDetail(doc, cb, i, y);
                y = y - line_item_height;
                if (y < 50) {
                    printPageNumber(cb);
                    doc.newPage();
                    beginPage = true;
                }
            }

            Tax = (currentEstimate.TaxRate * Subtotal) / 100;

            //This is the last item (notes). If it's over page, put it all on the next page.
            if ((y - 115) < 50) {
                printPageNumber(cb);
                doc.newPage();
                y = address1_start;
            }

            y -= 10;

            createText(cb, 460, y - 10, getResources().getString(R.string.subtotal), SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y - 10, helper_number.round(Subtotal), SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            createText(cb, 460, y - 35, getResources().getString(R.string.tax) + "(" + currentEstimate.TaxRate + "%)", SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y - 35, helper_number.round(Tax), SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            LineSeparator lineSeparator = new LineSeparator();

            lineSeparator.setLineColor(BaseColor.BLACK);
            lineSeparator.drawLine(cb, 375, 575, y - 50);

            createText(cb, 460, y - 70, getResources().getString(R.string.balance_due), SIZE_TEXT_TOTAL, bfBold, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y - 70, currency + helper_number.round(Tax + Subtotal), SIZE_TEXT_TOTAL, bfBold, BaseColor.BLACK, Element.ALIGN_RIGHT);

            if (currentEstimate.ClientNote.length() > 0) {
                createText(cb, 30, y - 130, getResources().getString(R.string.notes), SIZE_TEXT_NORMAL, bf, BaseColor.GRAY, Element.ALIGN_LEFT);
                createText(cb, 30, y - 145, currentEstimate.ClientNote, SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            }

            printPageNumber(cb);

        } catch (DocumentException ex) {
            ex.printStackTrace();
            return "";
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        } finally {
            if (doc != null) {
                doc.close();
            }
            if (docWriter != null) {
                docWriter.close();
            }
        }

        return path;
    }

    private void generateLayout(Document doc, PdfContentByte cb) {

        try {
            Rectangle rec = new Rectangle(30, line_heading_start + 15, 580, line_heading_start - 8);
            rec.setBackgroundColor(color_invoice_header_background);
            rec.setBorder(Rectangle.BOX);
            rec.setBorderWidth(3);
            rec.setBorderColor(color_light_grey);

            cb.rectangle(rec);

            // Estimate Detail box Text Headings
            createText(cb, 40, line_heading_start, "#", SIZE_TEXT_HEADER_TABLE, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            createText(cb, 70, line_heading_start, getResources().getString(R.string.item), SIZE_TEXT_HEADER_TABLE, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            createText(cb, 150, line_heading_start, getResources().getString(R.string.description), SIZE_TEXT_HEADER_TABLE, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            createText(cb, 350, line_heading_start, getResources().getString(R.string.rate), SIZE_TEXT_HEADER_TABLE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 460, line_heading_start, getResources().getString(R.string.quantity), SIZE_TEXT_HEADER_TABLE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, line_heading_start, getResources().getString(R.string.amount), SIZE_TEXT_HEADER_TABLE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void generateHeader(Document doc, PdfContentByte cb) {

        try {

            SharedPreferences settings = getSharedPreferences(SESSION_USER, MODE_PRIVATE);

            Integer address_startX = 30;

            String address = settings.getString(SettingActivity.KEY_ADDRESS, "");

            String[] myAddress = address.length() > 0 ? address.split("\\|\\#", -1) : new String[0]; //new String[] {settings.getString("address1", ""), settings.getString("address2", ""),settings.getString("city", ""),settings.getString("state", ""),settings.getString("postcode", ""),settings.getString("country", "")};

            createText(cb, address_startX, address1_start, settings.getString("firstname", "") + " " + settings.getString("lastname", ""), SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_LEFT);

            Integer index = 1;
            for (Integer i = 0; i < myAddress.length; i++) {
                String value = myAddress[i].trim();

                if (value.length() > 0) {
                    if (i == 3 && myAddress[4].trim().length() > 0) {
                        value += " " + myAddress[4].trim();
                        i++;
                    }

                    createText(cb, address_startX, address1_start - (index) * address_height, value, SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
                    index++;
                }
            }

            String[] clientAddress = new String[]{currentClient.Address1, currentClient.Address2, currentClient.City, currentClient.State, currentClient.Postcode, currentClient.Country};

            createText(cb, address_startX, address2_start, currentClient.Name, SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_LEFT);

            Integer clientIndex = 1;
            for (Integer i = 0; i < clientAddress.length; i++) {
                String value = clientAddress[i];

                if (value != null && value.length() > 0) {
                    if (i == 3 && clientAddress[4].length() > 0) {
                        value += " " + clientAddress[4];
                        i++;
                    }

                    createText(cb, address_startX, address2_start - clientIndex * address_height, value, SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
                    clientIndex++;
                }
            }

            clientIndex = clientIndex - 1;

            createText(cb, 350, address1_start - 10, getResources().getString(R.string.estimate_capitalized), SIZE_TEXT_TITLE_PAGE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            try (Realm realm = Realm.getDefaultInstance()) {
                StringPreference logoPreference = realm.where(StringPreference.class).equalTo("name", "logoImage").findFirst();
                if (logoPreference != null && !logoPreference.value.isEmpty()) {
                    Image image = Image.getInstance(Base64.decode(logoPreference.value, Base64.DEFAULT));
                    float width = image.getScaledWidth();
                    float height = image.getScaledHeight();
                    image.setAlignment(Element.ALIGN_RIGHT);
                    image.setAbsolutePosition(410 + (180 - width), address1_start - height + 10);
                    cb.addImage(image);
                }
            }

            createText(cb, 460, address2_start, " #" + getResources().getString(R.string.estimate), 10, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 580, address2_start, currentEstimate.getEstimateNumberFormatted() + "", SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");

            createText(cb, 460, address2_start - (address_height * 1) - 5, getResources().getString(R.string.estimate_date), SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 580, address2_start - (address_height * 1) - 5, dateFormat.format(helper_number.unixToDate(currentEstimate.EstimateDate)), SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            if (currentEstimate.EstimateDueDate > 0) {
                createText(cb, 460, address2_start - (address_height * 2) - 10, getResources().getString(R.string.estimate_due_date), SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
                createText(cb, 580, address2_start - (address_height * 2) - 10, dateFormat.format(helper_number.unixToDate(currentEstimate.EstimateDueDate)), SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void generateDetail(Document doc, PdfContentByte cb, int index, int y) {
        EstimateItem item = array_list_items.get(index);

        if (item == null) return;

        try {
            createText(cb, 40, y, String.valueOf(index + 1), SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            createText(cb, 70, y, item.Name, SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            createText(cb, 150, y, item.Description, SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            createText(cb, 350, y, helper_number.round(item.Rate), SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            createText(cb, 460, y, helper_number.round(item.Quantity), SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y, helper_number.round(item.getTotal()), SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            Subtotal += item.getTotal();

            LineSeparator lineSeparator = new LineSeparator();

            lineSeparator.setLineColor(color_light_grey);
            lineSeparator.drawLine(cb, 30, 580, y - 10);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void createHeadings(PdfContentByte cb, float x, float y, String text) {
        cb.beginText();
        cb.setFontAndSize(bf, 8);
        cb.setTextMatrix(x, y);
        cb.showText(text.trim());
        cb.endText();
    }

    private void createText(PdfContentByte cb, float x, float y, String text, Integer size, BaseFont font, BaseColor color, Integer alignment) {
        Font fnt = new Font(font, size);
        fnt.setColor(color);

        Phrase phrase = new Phrase(text, fnt);
        ColumnText.showTextAligned(cb, alignment, phrase, x, y, 0, PdfWriter.RUN_DIRECTION_RTL, AR_LIG);

    }

    private void printPageNumber(PdfContentByte cb) {
        cb.beginText();
        cb.setFontAndSize(bfBold, 8);
        cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, getResources().getString(R.string.page_no) + (pageNumber + 1), 570, 25, 0);
        cb.endText();

        pageNumber++;
    }

    private void createContent(PdfContentByte cb, float x, float y, String text, int align) {
        cb.beginText();
        cb.setFontAndSize(bf, 8);
        cb.showTextAligned(align, text.trim(), x, y, 0);
        cb.endText();
    }

    private void initializeFonts() {
        try {
            bfBold = BaseFont.createFont("res/font/tradbdo.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            bf = BaseFont.createFont("res/font/trado.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

        } catch (DocumentException ex) {
            //ex.printStackTrace();
        } catch (IOException ex) {
            //ex.printStackTrace();
        }
    }
}
