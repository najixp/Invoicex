package com.bytecodr.invoicing.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.model.DoublePreference;
import com.bytecodr.invoicing.model.Invoice;
import com.bytecodr.invoicing.network.MySingleton;
import com.bytecodr.invoicing.network.Network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HomeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment
{
    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    //private TextView text_paid;
    private TextView text_unpaid;

    private String currency;

    //private BarChart chart;

    private OnFragmentInteractionListener mListener;

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

        progressDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        SharedPreferences settings = getActivity().getSharedPreferences(LoginActivity.SESSION_USER, getActivity().MODE_PRIVATE);
        currency = settings.getString(SettingActivity.KEY_CURRENCY_SYMBOL, "$");

        api_parameter = new JSONObject();

        try
        {
            api_parameter.put("user_id", settings.getInt("id", 0));
        }
        catch(JSONException ex) {}

        RunGetInvoiceService();
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

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri)
    {
        if (mListener != null)
        {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
        {
            mListener = (OnFragmentInteractionListener) context;
        } else
        {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
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
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void RunGetInvoiceService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "invoices/get", api_parameter, response -> {
                    try {
                        Calendar calendar = Calendar.getInstance(); // this takes current date
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
                        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
                        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
                        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));

                        //Getting first of the last 4 months
                        long monthStartDate = calendar.getTimeInMillis() / 1000;

                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
                        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
                        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));

                        long monthEndDate = calendar.getTimeInMillis() / 1000;

                        double unpaid_total = 0;

                        JSONObject result = ((JSONObject) response.get("data"));
                        JSONArray invoices = (JSONArray) result.get("invoices");

                        for (int i = 0; i < invoices.length(); i++) {
                            JSONObject obj = invoices.getJSONObject(i);

                            Invoice invoice = new Invoice();

                            invoice.InvoiceDate = obj.optInt("invoice_date", 0);
                            invoice.TotalMoney = obj.getDouble("total");
                            invoice.IsPaid = (obj.getInt("is_paid") == 1);

                            if (invoice.InvoiceDate >= monthStartDate && invoice.InvoiceDate <= monthEndDate && !invoice.IsPaid)
                                unpaid_total += invoice.TotalMoney;
                        }

                        text_unpaid.setText(currency + String.format("%.2f", unpaid_total));
                        try (Realm realm = Realm.getDefaultInstance()) {
                            double finalUnpaid_total = unpaid_total;
                            realm.executeTransaction(realm1 -> realm1.insertOrUpdate(new DoublePreference("unpaidTotal", finalUnpaid_total)));
                        }
                    } catch (Exception ex) {
                        Toast.makeText(getContext(), R.string.offline_mode, Toast.LENGTH_LONG).show();

                        try (Realm realm = Realm.getDefaultInstance()) {
                            DoublePreference unPaidTotalPreference = realm.where(DoublePreference.class).equalTo("name", "unpaidTotal").findFirst();
                            if (unPaidTotalPreference != null)
                                text_unpaid.setText(currency + String.format("%.2f", unPaidTotalPreference.value));
                            else
                                text_unpaid.setText(R.string.not_applicable);
                        }
                    }

                    if (progressDialog != null && progressDialog.isShowing()) {
                        // If the response is JSONObject instead of expected JSONArray
                        progressDialog.dismiss();
                    }
                }, error -> {
                    // TODO Auto-generated method stub
                    if (progressDialog != null && progressDialog.isShowing()) {
                        // If the response is JSONObject instead of expected JSONArray
                        progressDialog.dismiss();
                    }

                    Toast.makeText(getContext(), R.string.offline_mode, Toast.LENGTH_LONG).show();

                    try (Realm realm = Realm.getDefaultInstance()) {
                        DoublePreference unPaidTotalPreference = realm.where(DoublePreference.class).equalTo("name", "unpaidTotal").findFirst();
                        if (unPaidTotalPreference != null)
                            text_unpaid.setText(currency + String.format("%.2f", unPaidTotalPreference.value));
                        else
                            text_unpaid.setText(R.string.not_applicable);
                    }
                })
        {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-API-KEY", MainActivity.api_key);
                return params;
            }
        };

        // Get a RequestQueue
        RequestQueue queue = MySingleton.getInstance(getContext()).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(MainActivity.TAG);

        MySingleton.getInstance(getContext()).addToRequestQueue(postRequest);
    }
}
