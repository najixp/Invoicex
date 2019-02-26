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
import com.bytecodr.invoicing.adapter.ClientAdapter;
import com.bytecodr.invoicing.model.Client;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.Sort;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ClientFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ClientFragment extends Fragment {

    private ListView list;
    private ClientAdapter adapter;
    private ArrayList<Client> array_list;

    public ClientFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ClientFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ClientFragment newInstance(String param1, String param2) {
        ClientFragment fragment = new ClientFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_client, container, false);

        FloatingActionButton add_client_button = (FloatingActionButton) view.findViewById(R.id.add_button);
        add_client_button.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NewClientActivity.class);
            startActivityForResult(intent, 1);
        });

        array_list = new ArrayList<>();
        adapter = new ClientAdapter(getContext(), array_list);

        list = (ListView) view.findViewById(R.id.list);
        // Assigning the adapter to ListView
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view1, position, id) -> {
            Client item = adapter.getItem(position);

            Intent intent = new Intent(getActivity(), NewClientActivity.class);
            intent.putExtra("data", item);

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
        if (isAdded()) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_activity_clients);
            NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_clients);
        }
    }

    public void updateViews() {
        try (Realm realm = Realm.getDefaultInstance()) {
            array_list.clear();
            array_list.addAll(realm.copyFromRealm(realm.where(Client.class).equalTo("pendingDelete", false).sort("Updated", Sort.DESCENDING).findAll()));
            adapter.notifyDataSetChanged();
        } catch (Exception e) {

        }
    }
}
