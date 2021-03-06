package com.bytecodr.invoicing.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.main.LoginActivity;
import com.bytecodr.invoicing.main.SettingActivity;
import com.bytecodr.invoicing.model.Estimate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class EstimateAdapter extends ArrayAdapter<Estimate>
{
    private final Context context;
    private final ArrayList<Estimate> values;
    private String currency;
    private SimpleDateFormat dateFormat;

    public EstimateAdapter(Context context, ArrayList<Estimate> values) {
        super(context, R.layout.layout_item_row, values);

        this.context = context;
        this.values = values;

        SharedPreferences settings = context.getSharedPreferences(LoginActivity.SESSION_USER, Context.MODE_PRIVATE);
        currency = settings.getString(SettingActivity.KEY_CURRENCY_SYMBOL, "$");
        dateFormat = new SimpleDateFormat("dd. MMM yyyy");
    }

    public Estimate getItem(int position)
    {
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

        View rowView = inflater.inflate(R.layout.layout_estimate_row, parent, false);

        Estimate estimate = values.get(position);

        TextView text_client_name = (TextView) rowView.findViewById(R.id.text_client_name);
        TextView text_estimate_number = (TextView) rowView.findViewById(R.id.text_estimate_number);
        TextView text_estimate_date = (TextView) rowView.findViewById(R.id.text_estimate_date);
        TextView text_total_amount = (TextView) rowView.findViewById(R.id.text_total_amount);
        TextView text_estimate_status = (TextView) rowView.findViewById(R.id.text_estimate_status);

        text_client_name.setText(estimate.ClientName);
        text_estimate_number.setText(estimate.getPurchaseName());

        if (estimate.EstimateDate != 0)   text_estimate_date.setText(dateFormat.format(estimate.getEstimateDate()));

        text_total_amount.setText(currency + String.format( "%.2f", estimate.TotalMoney));

        if (estimate.IsInvoiced) text_estimate_status.setText(getContext().getResources().getString(R.string.invoiced));
        return rowView;
    }
}
