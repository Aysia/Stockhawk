package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.MyStockDetailActivity;

public class WidgetRemoteViewService implements RemoteViewsService.RemoteViewsFactory {

    private Cursor mCursor;
    private Context mContext;
    int mWidgetId;

    /**
     *
     * @param context current context
     * @param intent intent to create object
     */
    public WidgetRemoteViewService(Context context, Intent intent) {
        mContext = context;
        mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {

    }

    /**
     * onDataSetChanged - get new cursor
     */
    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(
                QuoteProvider.Quotes.CONTENT_URI,
                new String[]{
                        QuoteColumns._ID,
                        QuoteColumns.SYMBOL,
                        QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE,
                        QuoteColumns.CHANGE,
                        QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    /**
     * onDestroy
     * check if mCursor is not null and close cursor
     */
    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    /**
     * @return getCount mCursor
     */
    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    /**
     *
     * @param position cursor position
     * @return object RemoteViews
     */
    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
        if (mCursor.moveToPosition(position)) {
            String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
            remoteViews.setTextViewText(R.id.stock_symbol, symbol);
            remoteViews.setTextViewText(R.id.bid_price, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE)));
            remoteViews.setTextViewText(R.id.change, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE)));

            /**
             * draw pill
             */
            if (mCursor.getInt(mCursor.getColumnIndex("is_up")) == 1) {
                remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            } else {
                remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
            }

            Intent intent = new Intent();
            intent.putExtra(MyStockDetailActivity.STOCK_SYMBOL, symbol);
            intent.putExtra(MyStockDetailActivity.PARENT, MyStockDetailActivity.PARENT_WIDGET);
            remoteViews.setOnClickFillInIntent(R.id.stock_row, intent);
        }
        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
