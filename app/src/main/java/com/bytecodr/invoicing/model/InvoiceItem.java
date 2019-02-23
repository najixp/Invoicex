package com.bytecodr.invoicing.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by GuriSingh on 08/05/2016.
 */
public class InvoiceItem extends RealmObject implements Serializable {
    @PrimaryKey
    public long Id;
    public long UserId;
    public long InvoiceId;

    public int Updated;
    public int Created;

    @SerializedName(value = "Name", alternate = {"name"})
    public String Name;
    @SerializedName(value = "Description", alternate = {"description"})
    public String Description;
    @SerializedName(value = "Rate", alternate = {"rate"})
    public double Rate;
    @SerializedName(value = "Quantity", alternate = {"quantity"})
    public double Quantity;

    public double getTotal() {
        return Quantity * Rate;
    }
}
