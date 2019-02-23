package com.bytecodr.invoicing.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.adapter.EstimateAdapter;
import com.bytecodr.invoicing.model.Estimate;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PurchaseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PurchaseFragment extends Fragment
{
    private ListView list;
    private EstimateAdapter adapter;
    private ArrayList<Estimate> array_list;

    public PurchaseFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment EstimateFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PurchaseFragment newInstance(String param1, String param2)
    {
        PurchaseFragment fragment = new PurchaseFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_purchase, container, false);

        FloatingActionButton add_client_button = (FloatingActionButton) view.findViewById(R.id.add_button);
        add_client_button.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NewPurchaseActivity.class);
            startActivityForResult(intent, 1);
        });

        array_list = new ArrayList<>();
        adapter = new EstimateAdapter(getContext(), array_list);

        list = (ListView) view.findViewById(R.id.list);
        // Assigning the adapter to ListView
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view1, position, id) -> {
            Estimate estimate = adapter.getItem(position);

            Intent intent = new Intent(getActivity(), NewPurchaseActivity.class);
            intent.putExtra("data", estimate);

            startActivity(intent);
        });

        /*AdView mAdView = (AdView) view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/

        updateViews();
        App.getInstance().registerOnUpdateListener(this::updateViews);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        //Checks to make sure fragment is still attached to activity
        if (isAdded())
        {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_activity_purchases);
            NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_purchases);
        }

        App.getInstance().updateData();
    }

    public void updateViews() {
        try (Realm realm = Realm.getDefaultInstance()) {
            array_list.clear();
            array_list.addAll(realm.copyFromRealm(realm.where(Estimate.class).equalTo("pendingDelete", false).findAll()));
            adapter.notifyDataSetChanged();
        } catch (Exception e) {

        }
    }
}
