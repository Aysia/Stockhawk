package com.sam_chordas.android.stockhawk.ui;

import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalDataColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;


public class MyStockDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    public static final String STOCK_SYMBOL = "stock_symbol";
    public static final String PARENT = "parent";
    public static final String PARENT_ACTIVITY = "activity";
    public static final String PARENT_WIDGET = "widget";
    private static final int CURSOR_LOADER_ID = 0;

    private String stockSymbol;
    private LineChartView mChart;

    private TextView graph_begin;
    private TextView graph_end;
    private TextView graph_evolution;
    private TextView graph_max;
    private TextView graph_min;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        stockSymbol = getIntent().getExtras().getString(STOCK_SYMBOL);

        //Obtailn linechartview withid linechart from activity_lin_graph.xml
        mChart = (LineChartView) findViewById(R.id.linechart);

        graph_begin = (TextView) findViewById(R.id.graph_begin);
        graph_end = (TextView) findViewById(R.id.graph_end);
        graph_evolution = (TextView) findViewById(R.id.graph_evolution);
        graph_max = (TextView) findViewById(R.id.graph_max);
        graph_min = (TextView) findViewById(R.id.graph_min);

        this.setTitle(stockSymbol);

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = new String[]{
                HistoricalDataColumns._ID,
                HistoricalDataColumns.SYMBOL,
                HistoricalDataColumns.DATE,
                HistoricalDataColumns.OPEN_PRICE};

        String selection = HistoricalDataColumns.SYMBOL + " = ?";

        String[] selectionArgs = new String[]{stockSymbol};

        String sortOrder = HistoricalDataColumns.DATE + " ASC";

        return new CursorLoader(this,
                QuoteProvider.HistoricalQuotation.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        /**
         * Create Chart - tutorial found here
         * http://nipunswritings.blogspot.com/2016/06/hellocharts-for-android-example.html
         */

        int x = 0;

        if (data.moveToFirst()){

            Float maxValue = 0f;
            Float minValue = 0f;

            List<AxisValue> axisValuesX = new ArrayList<>();
            List<PointValue> pointValues = new ArrayList<>();

            do {
                String date = data.getString(data.getColumnIndex(HistoricalDataColumns.DATE));
                String sPrice = data.getString(data.getColumnIndex(HistoricalDataColumns.OPEN_PRICE));
                Float fPrice = Float.valueOf(sPrice);

                if (maxValue == 0f || fPrice > maxValue){
                    maxValue = fPrice;
                }
                if (minValue == 0f || fPrice < minValue){
                    minValue = fPrice;
                }

                PointValue pointValue = new PointValue(x, fPrice);
                pointValues.add(pointValue);

                if (x == (data.getCount() / 3) || x == (data.getCount() / 3 * 2)) {
                    AxisValue axisValueX = new AxisValue(x);
                    axisValueX.setLabel(date);
                    axisValuesX.add(axisValueX);
                }

                x++;
            } while (data.moveToNext());

            // Draw Lines
            Line line = new Line(pointValues).setColor(Color.GREEN).setCubic(false);
            List<Line> lines = new ArrayList<>();
            lines.add(line);

            LineChartData lineChartData = new LineChartData();
            lineChartData.setLines(lines);

            // Define x-axis
            Axis axisX = new Axis(axisValuesX);
            axisX.setHasLines(true);
            axisX.setMaxLabelChars(4);
            lineChartData.setAxisXBottom(axisX);

            // Define y-axis
            Axis axisY = new Axis();
            axisY.setAutoGenerated(true);
            axisY.setHasLines(true);
            axisY.setMaxLabelChars(4);
            lineChartData.setAxisYLeft(axisY);

            // provide chart data to linechartview (@mChart)
            mChart.setInteractive(false);
            mChart.setLineChartData(lineChartData);

            // Define start date and end date, and evolution of price on this period
            data.moveToFirst();
            String startDate = data.getString(data.getColumnIndex(HistoricalDataColumns.DATE));
            String sStartPrice = data.getString(data.getColumnIndex(HistoricalDataColumns.OPEN_PRICE));
            Float fStartPrice = Float.valueOf(sStartPrice);
            data.moveToLast();
            String endDate = data.getString(data.getColumnIndex(HistoricalDataColumns.DATE));
            String sEndPrice = data.getString(data.getColumnIndex(HistoricalDataColumns.OPEN_PRICE));
            Float fEndPrice = Float.valueOf(sEndPrice);
            @SuppressLint("DefaultLocale") String evolution = String.format("%.4f",(fEndPrice-fStartPrice)*100/fStartPrice) + " %";

            // Update details information
            graph_begin.setText(startDate);
            graph_end.setText(endDate);
            graph_evolution.setText(evolution);
            graph_max.setText(String.valueOf(maxValue));
            graph_min.setText(String.valueOf(minValue));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (getIntent().getExtras().getString(PARENT) == PARENT_WIDGET) {
            Intent intent = new Intent(this, MyStocksActivity.class);
            startActivity(intent);
        } else {
            finish();
        }
    }
}
