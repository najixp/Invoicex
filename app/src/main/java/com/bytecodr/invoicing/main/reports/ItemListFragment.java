package com.bytecodr.invoicing.main.reports;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytecodr.invoicing.R;

import java.util.ArrayList;
import java.util.List;

public class ItemListFragment extends Fragment {

    private static final String ARG_ID_PREFIX = "ARG_ID_PREFIX";
    private static final String ARG_ITEMS = "ARG_ITEMS";

    private String mIdPrefix;
    private List<Item> mItems;

    private RecyclerView rv;

    public ItemListFragment() {}

    public static ItemListFragment newInstance(ArrayList<Item> items, String idPrefix) {
        ItemListFragment fragment = new ItemListFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_ITEMS, items);
        args.putString(ARG_ID_PREFIX, idPrefix);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mItems = args.getParcelableArrayList(ARG_ITEMS);
            mIdPrefix = args.getString(ARG_ID_PREFIX);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_item_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
    }

    private void initViews() {
        View view = getView();
        rv = (RecyclerView) view.findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        ItemAdapter adapter = new ItemAdapter(mItems, mIdPrefix);
        rv.setAdapter(adapter);
    }
}
