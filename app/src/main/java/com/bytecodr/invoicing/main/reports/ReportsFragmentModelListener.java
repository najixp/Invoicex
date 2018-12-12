package com.bytecodr.invoicing.main.reports;

import com.bytecodr.invoicing.network.ErrorResponse;

import java.util.List;

interface ReportsFragmentModelListener {
    void onGetReportSuccess(double invoiceSum, double purchaseSum, double difference, List<Item> invoices, List<Item> purchases);
    void onGetReportFailure(ErrorResponse response);
}