package com.bytecodr.invoicing.model;

import java.io.Serializable;
import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by GuriSingh on 08/05/2016.
 */
public class Invoice extends RealmObject implements Serializable {
    @PrimaryKey
    public long Id;
    public long UserId;

    public int Updated;
    public int Created;

    public long ClientId;
    public String ClientName;
    public long InvoiceNumber;

    public int InvoiceDate;
    public int InvoiceDueDate;

    public String ClientNote;
    public boolean IsPaid;

    public double TaxRate;
    public double TotalMoney;

    public boolean pendingUpdate = false;
    public boolean pendingDelete = false;

    public String getInvoiceName()
    {
        return "INV-" + getInvoiceNumberFormatted();
    }

    public String getInvoiceNumberFormatted()
    {
        return String.format("%04d", InvoiceNumber);
    }

    public Date getInvoiceDate()
    {
        return InvoiceDate == 0 ? null : new Date(InvoiceDate * 1000L);
    }

    public Date getInvoiceDueDate()
    {
        return InvoiceDueDate == 0 ? null : new Date(InvoiceDueDate * 1000L);
    }
}
