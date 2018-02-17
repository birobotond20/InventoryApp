package com.example.android.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

/**
 * {@link ProductDbHelper} class to create, open and maintain the Products database
 */

public class ProductDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = ProductDbHelper.class.getSimpleName();

    /** Name of the database file */
    private static final String DATABASE_NAME = "store.db";

    /**
     * Database version. If the database schema changes, the database version must be incremented.
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Constructs a new instance of {@link ProductDbHelper}.
     *
     * @param context of the app
     */
    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_PRODUCTS_TABLE =
                "CREATE TABLE " + ProductEntry.TABLE_NAME + "("
                + ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL,"
                + ProductEntry.COLUMN_PRODUCT_PRICE + " INTEGER NOT NULL,"
                + ProductEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER NOT NULL DEFAULT 0,"
                + ProductEntry.COLUMN_PRODUCT_SUPPLIER + " TEXT,"
                + ProductEntry.COLUMN_PRODUCT_IMAGE + " TEXT" + ");";

        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
