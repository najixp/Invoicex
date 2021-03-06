package com.bytecodr.invoicing.model;

import java.io.Serializable;
import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by GuriSingh on 08/05/2016.
 */
public class Estimate extends RealmObject implements Serializable {
    @PrimaryKey
    public long Id;
    public long UserId;

    public int Updated;
    public int Created;

    public long ClientId;
    public String ClientName;
    public long EstimateNumber;

    public int EstimateDate;
    public int EstimateDueDate;

    public String ClientNote;
    public boolean IsInvoiced;

    public double TaxRate;
    public double TotalMoney;

    public boolean pendingUpdate = false;
    public boolean pendingDelete = false;

    public String getEstimateName()
    {
        return "EST-" + getEstimateNumberFormatted();
    }

    public String getPurchaseName()
    {
        return "PRC-" + getEstimateNumberFormatted();
    }

    public String getEstimateNumberFormatted()
    {
        return String.format("%04d", EstimateNumber);
    }

    public Date getEstimateDate()
    {
        return EstimateDate == 0 ? null : new Date(EstimateDate * 1000L);
    }

    public Date getEstimateDueDate()
    {
        return EstimateDueDate == 0 ? null : new Date(EstimateDueDate * 1000L);
    }
}
