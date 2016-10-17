package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.HistoricalDataColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }
  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
            .url(url)
            .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null){
      mContext = this;
    }
    StringBuilder urlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
      urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
              + "in (", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
              new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
              null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(
                  URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      } else if (initQueryCursor != null){
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSymbols.append("\""+
                  initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
      initQueryCursor.close();
    } else if (params.getTag().equals("add")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    }
    // finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
            + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_SUCCESS;
    int subResult = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();
      try{
        getResponse = fetchData(urlString);
        subResult = GcmNetworkManager.RESULT_SUCCESS;
        try {
          ContentValues contentValues = new ContentValues();
          // update ISCURRENT to 0 (false) so new data is current
          if (isUpdate){
            contentValues.put(QuoteColumns.ISCURRENT, 0);
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                    null, null);
          }
          ArrayList<ContentProviderOperation> batchOperations = Utils.quoteJsonToContentVals(getResponse);
          if (batchOperations != null && batchOperations.size() > 0) {
            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, batchOperations);
          } else {
            Intent intent = new Intent();
            intent.setAction("com.sam_chordas.android.stockhawk.service.InvalidStockReceiver");
            mContext.sendBroadcast(intent);
          }
        }catch (RemoteException | OperationApplicationException e){
          Log.e(LOG_TAG, "Error applying batch insert", e);
        }
      } catch (IOException e){
        e.printStackTrace();
      }
    }
    if (subResult != GcmNetworkManager.RESULT_SUCCESS){
      result = GcmNetworkManager.RESULT_FAILURE;
    }
    subResult = GcmNetworkManager.RESULT_FAILURE;

    Cursor symbolQueryCursor;
    symbolQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
            new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
            null, null);

    if (symbolQueryCursor != null){
      DatabaseUtils.dumpCursor(symbolQueryCursor);
      symbolQueryCursor.moveToFirst();

      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
      Date currentDate = new Date();

      Calendar calEnd = Calendar.getInstance();
      calEnd.setTime(currentDate);
      calEnd.add(Calendar.DATE, 0);

      Calendar calStart = Calendar.getInstance();
      calStart.setTime(currentDate);
      calStart.add(Calendar.MONTH, -1);

      String startDate = dateFormat.format(calStart.getTime());
      String endDate = dateFormat.format(calEnd.getTime());

      for (int i = 0; i < symbolQueryCursor.getCount(); i++){

        String symbol = symbolQueryCursor.getString(symbolQueryCursor.getColumnIndex(QuoteColumns.SYMBOL));

        StringBuilder histoUrlStringBuilder = new StringBuilder();
        try{
          // URI for Historical Data
          histoUrlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
          histoUrlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol "
                  + "= ", "UTF-8"));
          histoUrlStringBuilder.append(URLEncoder.encode("\""+symbol+"\"", "UTF-8"));
          histoUrlStringBuilder.append(URLEncoder.encode(" and startDate=\"" + startDate + "\" and endDate=\"" + endDate + "\"", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }

        histoUrlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String histoUrlString;
        String getHistoResponse;

        if (histoUrlStringBuilder != null){
          histoUrlString = histoUrlStringBuilder.toString();
          try{
            getHistoResponse = fetchData(histoUrlString);
            try {
              ContentResolver resolver = mContext.getContentResolver();
              resolver.delete(QuoteProvider.HistoricalQuotation.CONTENT_URI,
                      HistoricalDataColumns.SYMBOL + " = \"" + symbol + "\"", null);
              mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                      Utils.quoteHistoJsonToContentVals(getHistoResponse));
            }catch (RemoteException | OperationApplicationException e){
              Log.e(LOG_TAG, "Error applying batch insert", e);
            }
          } catch (IOException e){
            e.printStackTrace();
          }
        }
        if (subResult != GcmNetworkManager.RESULT_SUCCESS){
          result = GcmNetworkManager.RESULT_FAILURE;
        }
        subResult = GcmNetworkManager.RESULT_FAILURE;
        symbolQueryCursor.moveToNext();
      }
    }
    symbolQueryCursor.close();

    return result;
  }

}
