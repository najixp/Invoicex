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
import com.bytecodr.invoicing.adapter.DescriptionAdapter;
import com.bytecodr.invoicing.model.Description;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DescriptionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DescriptionFragment extends Fragment {

    private ListView list;
    private DescriptionAdapter adapter;
    private ArrayList<Description> array_list;

    public DescriptionFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ItemFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DescriptionFragment newInstance(String param1, String param2)
    {
        DescriptionFragment fragment = new DescriptionFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_description, container, false);

        FloatingActionButton add_client_button = (FloatingActionButton) view.findViewById(R.id.add_button);
        add_client_button.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NewDescriptionActivity.class);
            startActivityForResult(intent, 1);
        });

        array_list = new ArrayList<>();
        adapter = new DescriptionAdapter(getContext(), array_list);

        list = (ListView) view.findViewById(R.id.list);
        // Assigning the adapter to ListView
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view1, position, id) -> {
            Description description = adapter.getItem(position);

            Intent intent = new Intent(getActivity(), NewDescriptionActivity.class);
            intent.putExtra("data", description);

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
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Descriptions");
            NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.navDescriptions);
        }

        App.getInstance().updateData();
    }

    public void updateViews() {
        try (Realm realm = Realm.getDefaultInstance()) {
            array_list.clear();
            array_list.addAll(realm.copyFromRealm(realm.where(Description.class).equalTo("pendingDelete", false).findAll()));
            adapter.notifyDataSetChanged();
        } catch (Exception e) {

        }
    }
}
