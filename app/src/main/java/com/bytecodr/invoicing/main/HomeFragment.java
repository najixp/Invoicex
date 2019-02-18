package com.bytecodr.invoicing.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.model.DoublePreference;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment
{

    //private TextView text_paid;
    private TextView text_unpaid;

    private String currency;

    //private BarChart chart;

    public HomeFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2)
    {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        SharedPreferences settings = getActivity().getSharedPreferences(LoginActivity.SESSION_USER, getActivity().MODE_PRIVATE);
        currency = settings.getString(SettingActivity.KEY_CURRENCY_SYMBOL, "$");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        //text_paid = (TextView) view.findViewById(R.id.text_paid);
        text_unpaid    = (TextView) view.findViewById(R.id.text_unpaid);

//        chart = (BarChart) view.findViewById(R.id.chart);
//        chart.getLegend().setEnabled(false);
//        chart.getAxisRight().setDrawLabels(false);
//        chart.setDescription("");

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
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_activity_home);
            NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_home);
        }

        App.getInstance().updateData();
    }

    public void updateViews()
    {
        try (Realm realm = Realm.getDefaultInstance()) {
            DoublePreference unPaidTotalPreference = realm.where(DoublePreference.class).equalTo("name", "unpaidTotal").findFirst();
            if (unPaidTotalPreference != null)
                text_unpaid.setText(currency + String.format("%.2f", unPaidTotalPreference.value));
            else
                text_unpaid.setText(R.string.not_applicable);
        } catch (Exception e) {

        }
    }
}
