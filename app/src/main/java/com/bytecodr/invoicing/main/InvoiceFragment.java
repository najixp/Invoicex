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
import com.bytecodr.invoicing.adapter.InvoiceAdapter;
import com.bytecodr.invoicing.model.Invoice;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.Sort;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InvoiceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InvoiceFragment extends Fragment {

    private ListView list;
    private InvoiceAdapter adapter;
    private ArrayList<Invoice> array_list;

    InterstitialAd mInterstitialAd;

    public InvoiceFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment InvoiceFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InvoiceFragment newInstance(String param1, String param2) {
        InvoiceFragment fragment = new InvoiceFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invoice, container, false);

        FloatingActionButton add_client_button = (FloatingActionButton) view.findViewById(R.id.add_button);
        add_client_button.setOnClickListener(v -> {

            //If you want to show InterstitialAd ad, uncomment this line AND comment out the two lines below with the intent.
            //if (mInterstitialAd.isLoaded()) mInterstitialAd.show();
            Intent intent = new Intent(getActivity(), NewInvoiceActivity.class);
            startActivityForResult(intent, 1);
        });

        array_list = new ArrayList<>();
        adapter = new InvoiceAdapter(getContext(), array_list);

        list = (ListView) view.findViewById(R.id.list);
        // Assigning the adapter to ListView
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view1, position, id) -> {
            Invoice item = adapter.getItem(position);

            Intent intent = new Intent(getActivity(), NewInvoiceActivity.class);
            intent.putExtra("data", item);

            startActivity(intent);
        });

        /*AdView mAdView = (AdView) view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/

        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.interstitial_ad_unit_id));
        //You need to add your device ID here in order to see the test interstitial ad.
        mInterstitialAd.loadAd(new AdRequest.Builder().addTestDevice("22476D45F411A527CA1C8DE7DF2D8111").build());


        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                Intent intent = new Intent(getActivity(), NewInvoiceActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        updateViews();
        App.getInstance().registerOnUpdateListener(this::updateViews);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        //Checks to make sure fragment is still attached to activity
        if (isAdded()) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_activity_invoices);
            NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_invoices);
        }
    }

    public void updateViews() {
        try (Realm realm = Realm.getDefaultInstance()) {
            array_list.clear();
            array_list.addAll(realm.copyFromRealm(realm.where(Invoice.class).equalTo("pendingDelete", false).sort("Updated", Sort.DESCENDING).findAll()));
            adapter.notifyDataSetChanged();
        } catch (Exception e) {

        }
    }
}
