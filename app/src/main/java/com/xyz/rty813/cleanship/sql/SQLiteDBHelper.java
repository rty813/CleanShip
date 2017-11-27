package com.xyz.rty813.cleanship.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by zhang on 2017/11/27.
 */

public class SQLiteDBHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "route.db";
    public static final String TABLE_NAME1 = "STORE";
    public static final String TABLE_NAME2 = "HISTORY";

    public SQLiteDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "CREATE TABLE IF NOT EXISTS" + TABLE_NAME1 + " (ID INTEGER PRIMARY KEY, LATITUDE VARCHAR(500), LONGITUDE VARCHAR(500))";
        sqLiteDatabase.execSQL(sql);
        sql = "CREATE TABLE IF NOT EXISTS" + TABLE_NAME2 + " (ID INTEGER PRIMARY KEY, LATITUDE VARCHAR(500), LONGITUDE VARCHAR(500))";
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME1;
        sqLiteDatabase.execSQL(sql);
        sql = "DROP TABLE IF EXISTS " + TABLE_NAME2;
        sqLiteDatabase.execSQL(sql);
        onCreate(sqLiteDatabase);
    }
}
