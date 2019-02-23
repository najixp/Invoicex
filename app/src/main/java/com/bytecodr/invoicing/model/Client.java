package com.bytecodr.invoicing.model;

import java.io.Serializable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by GuriSingh on 08/05/2016.
 */
public class Client extends RealmObject implements Serializable {
    @PrimaryKey
    public long Id;
    public long UserId;

    public int Updated;
    public int Created;

    public String Name;
    public String Email;

    public String Address1;
    public String Address2;
    public String City;
    public String State;
    public String Postcode;
    public String Country;

    public double TotalMoney;

    public boolean pendingUpdate = false;
    public boolean pendingDelete = false;
}
