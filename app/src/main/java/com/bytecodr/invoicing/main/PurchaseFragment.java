package com.bytecodr.invoicing.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bytecodr.invoicing.CommonUtilities;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.adapter.EstimateAdapter;
import com.bytecodr.invoicing.helper.helper_string;
import com.bytecodr.invoicing.model.Estimate;
import com.bytecodr.invoicing.network.MySingleton;
import com.bytecodr.invoicing.network.Network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PurchaseFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PurchaseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PurchaseFragment extends Fragment
{
    private OnFragmentInteractionListener mListener;

    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

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
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        progressDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_purchase, container, false);

        FloatingActionButton add_client_button = (FloatingActionButton) view.findViewById(R.id.add_button);
        add_client_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!CommonUtilities.isOnline(getContext())) {
                    Toast.makeText(getContext(), "Disabled in offline mode", Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(getActivity(), NewPurchaseActivity.class);
                startActivityForResult(intent , 1);
            }
        });

        SharedPreferences settings = getActivity().getSharedPreferences(LoginActivity.SESSION_USER, getActivity().MODE_PRIVATE);

        array_list = new ArrayList<Estimate>();
        adapter = new EstimateAdapter(getContext(), array_list);

        list = (ListView) view.findViewById(R.id.list);
        // Assigning the adapter to ListView
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (!CommonUtilities.isOnline(getContext())) {
                    Toast.makeText(getContext(), "Disabled in offline mode", Toast.LENGTH_LONG).show();
                    return;
                }

                Estimate estimate = adapter.getItem(position);

                Intent intent = new Intent(getActivity(), NewPurchaseActivity.class);
                intent.putExtra("data", estimate);

                startActivity(intent);
            }
        });

        /*AdView mAdView = (AdView) view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/

        api_parameter = new JSONObject();

        try
        {
            api_parameter.put("user_id", settings.getInt("id", 0));
        }
        catch(JSONException ex) {}

        RunGetEstimateService();

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
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_activity_purchases);
            NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_purchases);
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

    public void RunGetEstimateService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "estimates/get", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            VolleyLog.d("Response/Purchase: " + response.toString());

                            JSONObject result = ((JSONObject)response.get("data"));
                            JSONArray estimates = (JSONArray)result.get("estimates");

                            array_list.clear();
                            try (Realm realm = Realm.getDefaultInstance()) {
                                realm.executeTransaction(realm1 -> realm1.where(Estimate.class).findAll().deleteAllFromRealm());

                                for (int i = 0; i < estimates.length(); i++) {
                                    JSONObject obj = estimates.getJSONObject(i);

                                    Estimate estimate = new Estimate();

                                    estimate.Id = obj.optInt("id");
                                    estimate.UserId = obj.optInt("user_id");

                                    estimate.EstimateNumber = obj.getInt("estimate_number");
                                    estimate.ClientName = helper_string.optString(obj, "client_name");
                                    estimate.ClientId = obj.getInt("client_id");
                                    estimate.ClientNote = helper_string.optString(obj, "notes");
                                    estimate.EstimateDate = obj.optInt("estimate_date", 0);
                                    estimate.EstimateDueDate = obj.optInt("due_date", 0);
                                    estimate.TaxRate = obj.getDouble("tax_rate");
                                    estimate.TotalMoney = obj.getDouble("total");
                                    estimate.IsInvoiced = (obj.getInt("is_invoiced") == 1);

                                    estimate.Created = obj.optInt("created_on", 0);
                                    estimate.Updated = obj.optInt("updated_on", 0);

                                    array_list.add(estimate);
                                    realm.executeTransaction(realm12 -> realm12.insertOrUpdate(estimate));
                                }
                            }

                            adapter.notifyDataSetChanged();
                        }
                        catch(Exception ex)
                        {
                            Toast.makeText(getContext(), R.string.offline_mode, Toast.LENGTH_LONG).show();

                            try (Realm realm = Realm.getDefaultInstance()) {
                                array_list.clear();
                                array_list.addAll(realm.copyFromRealm(realm.where(Estimate.class).findAll()));
                                adapter.notifyDataSetChanged();
                            }
                        }

                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }
                    }
                }, new Response.ErrorListener()
                {

                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        // TODO Auto-generated method stub
                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }

                        Toast.makeText(getContext(), R.string.offline_mode, Toast.LENGTH_LONG).show();

                        try (Realm realm = Realm.getDefaultInstance()) {
                            array_list.clear();
                            array_list.addAll(realm.copyFromRealm(realm.where(Estimate.class).findAll()));
                            adapter.notifyDataSetChanged();
                        }
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
