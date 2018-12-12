package com.bytecodr.invoicing.main.reports;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytecodr.invoicing.R;
import com.itextpdf.text.pdf.parser.Line;

import java.text.DecimalFormat;
import java.util.List;

class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private static final DecimalFormat decimalFormat = new DecimalFormat("##.##");
    private List<Item> mItems;
    private String mIdPrefix;

    public ItemAdapter(List<Item> items, String idPrefix) {
        this.mItems = items;
        this.mIdPrefix = idPrefix;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Item item = mItems.get(position);
        holder.llHeading.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        holder.tvReportTypeTitle.setText(mIdPrefix.equals("INV") ? "Invoice#" : "Purchase#");
        holder.tvId.setText(mIdPrefix + "-" + item.id);
        holder.tvAmount.setText(String.valueOf(decimalFormat.format(item.amount)));
        holder.tvVat.setText(String.valueOf(decimalFormat.format(item.vat)));
        holder.tvTotal.setText(String.valueOf(decimalFormat.format(item.total)));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvId;
        private TextView tvAmount;
        private TextView tvVat;
        private TextView tvTotal;
        private TextView tvReportTypeTitle;
        private LinearLayout llHeading;

        ViewHolder(View itemView) {
            super(itemView);
            tvId = (TextView) itemView.findViewById(R.id.tvId);
            tvAmount = (TextView) itemView.findViewById(R.id.tvAmount);
            tvVat = (TextView) itemView.findViewById(R.id.tvVat);
            tvTotal = (TextView) itemView.findViewById(R.id.tvTotal);
            tvReportTypeTitle = (TextView) itemView.findViewById(R.id.tvReportTypeTitle);
            llHeading = (LinearLayout) itemView.findViewById(R.id.llHeading);
        }
    }
}
