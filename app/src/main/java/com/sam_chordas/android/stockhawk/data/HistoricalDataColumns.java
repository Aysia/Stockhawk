package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * define columns to hold historical quotations for display in graph
 */

public class HistoricalDataColumns {

    @DataType(DataType.Type.INTEGER) @PrimaryKey
    @AutoIncrement
    public static final String _ID = "_id";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String SYMBOL = "symbol";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String OPENPRICE = "open_price";
    @DataType(DataType.Type.TEXT)
    public static final String DATE = "date";
}