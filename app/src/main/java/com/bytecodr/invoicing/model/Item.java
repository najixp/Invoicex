package com.bytecodr.invoicing.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by GuriSingh on 08/05/2016.
 */
public class Item extends BaseModel implements Serializable
{
    @SerializedName(value = "Name", alternate = {"name"})
    public String Name;
    @SerializedName(value ="Description", alternate = {"description"})
    public String Description;
    @SerializedName(value = "Rate", alternate = {"rate"})
    public double Rate;
    @SerializedName(value = "Quantity", alternate = {"quantity"})
    public double Quantity;

    public double getTotal()
    {
        return Quantity * Rate;
    }
}
