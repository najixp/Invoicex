package com.bytecodr.invoicing.main.reports;

import com.bytecodr.invoicing.App;
import com.bytecodr.invoicing.network.ErrorResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class ReportsFragmentModel {
    private ReportsFragmentModelListener mListener;

    ReportsFragmentModel(ReportsFragmentModelListener mListener) {
        this.mListener = mListener;
    }

    void getReport(int userId) {
        App.getInstance().api.getReport(App.SERVER_KEY_HASH, userId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    try {
                        JsonObject responseJo = response.body();
                        if (responseJo.get("status").getAsString().equals("true")) {

                            JsonObject sumJo = responseJo.getAsJsonObject("sum");
                            Double invoiceSum, purchaseSum, difference;
                            try {
                                invoiceSum = sumJo.get("invoice_sum").getAsDouble();
                            } catch (Exception e) {
                                invoiceSum = 0d;
                            }

                            try {
                                purchaseSum = sumJo.get("get_esimate").getAsDouble();
                            } catch (Exception e) {
                                purchaseSum = 0d;
                            }

                            try {
                                difference = sumJo.get("diff").getAsDouble();
                            } catch (Exception e) {
                                difference = 0d;
                            }

                            JsonObject contentJo = responseJo.getAsJsonObject("content");
                            JsonArray invoicesJa = contentJo.getAsJsonArray("invoice");
                            JsonArray purchasesJa = contentJo.getAsJsonArray("estimate");

                            Type type = (new TypeToken<List<Item>>(){}).getType();
                            List<Item> invoices = (new Gson()).fromJson(invoicesJa, type);
                            List<Item> purchases = (new Gson()).fromJson(purchasesJa, type);

                            mListener.onGetReportSuccess(invoiceSum, purchaseSum, difference, invoices, purchases);
                        } else {
                            mListener.onGetReportFailure(new ErrorResponse(ErrorResponse.Code.CODE_GENERIC));
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        mListener.onGetReportFailure(new ErrorResponse(ErrorResponse.Code.CODE_UNKNOWN));
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                mListener.onGetReportFailure(new ErrorResponse(ErrorResponse.Code.CODE_BAD_NETWORK));
            }
        });
    }


}
