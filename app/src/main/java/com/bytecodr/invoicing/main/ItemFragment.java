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
import android.widget.Toast;

import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.CommonUtilities;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.adapter.ItemAdapter;
import com.bytecodr.invoicing.model.Item;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ItemFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ItemFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ItemFragment extends Fragment
{
    private ListView list;
    private ItemAdapter adapter;
    private ArrayList<Item> array_list;

    public ItemFragment()
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
    public static ItemFragment newInstance(String param1, String param2)
    {
        ItemFragment fragment = new ItemFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        FloatingActionButton add_client_button = (FloatingActionButton) view.findViewById(R.id.add_button);
        add_client_button.setOnClickListener(v -> {
            if (!CommonUtilities.isOnline(getContext())) {
                Toast.makeText(getContext(), "Disabled in offline mode", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(getActivity(), NewItemActivity.class);
            startActivityForResult(intent, 1);
        });

        array_list = new ArrayList<>();
        adapter = new ItemAdapter(getContext(), array_list);

        list = (ListView) view.findViewById(R.id.list);
        // Assigning the adapter to ListView
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view1, position, id) -> {
            if (!CommonUtilities.isOnline(getContext())) {
                Toast.makeText(getContext(), "Disabled in offline mode", Toast.LENGTH_LONG).show();
                return;
            }

            Item item = adapter.getItem(position);

            Intent intent = new Intent(getActivity(), NewItemActivity.class);
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
        if (isAdded())
        {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_activity_items);
            NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_items);
        }

        App.getInstance().updateData();
    }

    public void updateViews() {
        try (Realm realm = Realm.getDefaultInstance()) {
            array_list.clear();
            array_list.addAll(realm.copyFromRealm(realm.where(Item.class).findAll()));
            adapter.notifyDataSetChanged();
        } catch (Exception e) {

        }
    }
}
