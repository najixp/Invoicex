package com.bytecodr.invoicing.main.reports;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

class PagerAdapter
        extends FragmentStatePagerAdapter {

    List<Item> invoices;
    List<Item> purchases;

    public PagerAdapter(FragmentManager fm, List<Item> invoices, List<Item> purchases) {
        super(fm);
        this.invoices = invoices;
        this.purchases = purchases;
    }

    @Override
    public Fragment getItem(int position) {
        return ItemListFragment.newInstance((ArrayList<Item>) (position==0?invoices:purchases), position==0?"INV":"PRC");
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return (position==0?"Invoices":"Purchases");
    }
}
