package com.bytecodr.invoicing.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.main.LoginActivity;
import com.bytecodr.invoicing.main.SettingActivity;
import com.bytecodr.invoicing.model.Description;

import java.util.ArrayList;


public class DescriptionAdapter extends ArrayAdapter<Description>
{
    private final Context context;
    private final ArrayList<Description> values;
    private String currency;

    public DescriptionAdapter(Context context, ArrayList<Description> values) {
        super(context, R.layout.layout_description_row, values);

        this.context = context;
        this.values = values;

        SharedPreferences settings = context.getSharedPreferences(LoginActivity.SESSION_USER, Context.MODE_PRIVATE);
        currency = settings.getString(SettingActivity.KEY_CURRENCY_SYMBOL, "$");
    }


    @Nullable
    @Override
    public Description getItem(int position) {
        return super.getItem(position);
    }

    /**
     * Here we go and get our rowlayout.xml file and set the textview text.
     * This happens for every row in your listview.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.layout_description_row, parent, false);

        TextView tvTitle = (TextView) rowView.findViewById(R.id.tvTitle);
        TextView tvDescription = (TextView) rowView.findViewById(R.id.tvDescription);

        tvTitle.setText(values.get(position).title);
        tvDescription.setText(values.get(position).description);

        return rowView;
    }
}
