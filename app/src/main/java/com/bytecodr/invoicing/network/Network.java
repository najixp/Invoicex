package com.bytecodr.invoicing.network;

import android.content.Context;
import android.net.ConnectivityManager;

public class Network
{
    public static final String API_URL = "http://soldv.com/";
    public static final String API_KEY = "";

    public Network()
    {
        // TODO Auto-generated constructor stub
        super();
    }

    public static boolean checkStatus(Context context)
    {
        // TODO Auto-generated method stub
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr.getActiveNetworkInfo() != null
                && connMgr.getActiveNetworkInfo().isAvailable()
                && connMgr.getActiveNetworkInfo().isConnected())
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
