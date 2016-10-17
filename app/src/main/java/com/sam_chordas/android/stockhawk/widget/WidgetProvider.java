package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.MyStockDetailActivity;

/**
 * WidgetProvider
 */
public class WidgetProvider extends android.appwidget.AppWidgetProvider {

    static void updateWidget(Context context, AppWidgetManager appWidgetManager,
                             int appWidgetId) {

        // Create the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stockhawk_widget);

        // Create an Intent to launch MyStocksActivity
        Intent adapter = new Intent(context, WidgetService.class);
        adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setRemoteAdapter(R.id.stock_list, adapter);

        //Intent to start MyStockDetailActivity
        Intent intent = new Intent(context, MyStockDetailActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setPendingIntentTemplate(R.id.stock_list, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.stock_list);
    }

    /**
     *
     * @param context
     * @param appWidgetManager
     * @param appWidgetIds
     *
     * in case of mulitple widgets - update all
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {
    }
}

