package com.example.android.inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import java.text.DecimalFormat;

/**
 * {@link ProductCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */

public class ProductCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = ProductCursorAdapter.class.getSimpleName();

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        Button saleButton = (Button) view.findViewById(R.id.sale_button);

        // Find the columns of product attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        final int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);

        // Read the product attributes from the Cursor for the current product
        String productName = cursor.getString(nameColumnIndex);
        int productQuantity = cursor.getInt(quantityColumnIndex);
        String textProductQuantity;
        double productPrice = cursor.getDouble(priceColumnIndex);
        String textProductPrice = formatPriceAmount(productPrice);
        int currentProductId = cursor.getInt(idColumnIndex);
        final Uri contentUri = Uri.withAppendedPath(ProductEntry.CONTENT_URI,
                Integer.toString(currentProductId));

        // If the product quantity is empty string or null, then use some default text
        // that says "Out of stock", so the TextView isn't blank.
        if (productQuantity == 0) {
            textProductQuantity = context.getString(R.string.out_of_stock);
        } else {
            textProductQuantity = Integer.toString(productQuantity);
        }

        // Update the TextViews with the attributes for the current product
        nameTextView.setText(productName);
        quantityTextView.setText(textProductQuantity);
        priceTextView.setText(textProductPrice);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantity;
                try {
                    quantity = Integer.parseInt(quantityTextView.getText().toString().trim());
                } catch (NumberFormatException e) {
                    quantity = 0;
                }
                if (quantity > 0){
                    quantity = quantity - 1;
                }
                ContentValues values = new ContentValues();
                values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

                context.getContentResolver().update(contentUri, values, null, null);
            }
        });
    }

    /**
     * Return the formatted price showing 2 decimal places (i.e. "$12.99")
     * from a decimal rating value.
     */
    private String formatPriceAmount(double price) {
        DecimalFormat priceFormat;
        if (price > 0.0) {
            priceFormat = new DecimalFormat("$#,###.00");
            return priceFormat.format(price / 100.00);
        } else {
            return "Price unavailable";
        }
    }
}
