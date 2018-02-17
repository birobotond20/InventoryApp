/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Allows user to create a new product entry or edit an existing one.
 */

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = DetailActivity.class.getSimpleName();

    /**
     * EditText field to enter the product's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the product's price
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the product's quantity
     */
    private EditText mQuantityEditText;

    /**
     * EditText field to enter the product's supplier
     */
    private EditText mSupplierEditText;

    /**
     * ImageView field that shows the image of the product
     */
    private ImageView mProductImageView;

    /**
     * Button to decrease the quantity in the mQuantityEditText
     */
    private Button mDecreaseButton;

    /**
     * Button to increase the quantity in the mQuantityEditText
     */
    private Button mIncreaseButton;

    /**
     * Button to initiate image selection for the product
     */
    private Button mSelectImageButton;

    /**
     * Content URI for the selected image's uri
     */
    private Uri mSelectedImageUri;

    /**
     * String to save the selected image's uri to
     */
    private String mSelectedImageUriString;

    /**
     * Uri of the placeholder image
     */
    private static final String PLACEHOLDER_IMAGE_URI =
            Uri.parse("android.resource://com.example.android.inventoryapp/drawable/placeholder").
            toString();

    /**
     * Request code for an intent to open image selector
     */
    private static final int PICK_IMAGE_REQUEST = 0;

    /**
     * Transient state for the activity instance
     */
    private static final String STATE_URI = "STATE_URI";

    /**
     * Identifier for the product loader
     */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    /**
     * Content URI for the existing product (null if it's a new product)
     */
    private Uri mCurrentProductUri;

    /**
     * Boolean flag that keeps track of whether the product has been edited (true) or not (false)
     */
    private boolean mProductHasChanged = false;

    // OnTouchListener that listens for any user touches on a View, implying that they are modifying
    // the view, and we change the mProductHasChanged boolean to true.
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();

        // Get the uri of the product that was clicked on
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a product"
            setTitle(R.string.detail_activity_title_new_product);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle(R.string.detail_activity_title_edit_product);

            // Initialize the loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mSupplierEditText = (EditText) findViewById(R.id.edit_product_supplier);
        mSelectImageButton = (Button) findViewById(R.id.select_image_button);
        mProductImageView = (ImageView) findViewById(R.id.product_image);

        // Find all relevant buttons and set onClick actions on them
        // 1. Decrease button
        mDecreaseButton = (Button) findViewById(R.id.detail_decrease_quantity_button);
        mDecreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decreaseQuantity();
            }
        });
        // 2. Increase button
        mIncreaseButton = (Button) findViewById(R.id.detail_increase_quantity_button);
        mIncreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                increaseQuantity();
            }
        });
        // 3. Select Image Button
        mSelectImageButton = (Button) findViewById(R.id.select_image_button);
        mSelectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        // Set default image for the mProductImageView
        mProductImageView.setImageResource(R.drawable.placeholder);

        // Set the placeholder drawable as tag for the mProductImageView
        mProductImageView.setTag(R.drawable.placeholder);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mSelectImageButton.setOnTouchListener(mTouchListener);

        // Setup addTextChangeListener on the mQuantityEditText field, so that we can determine if
        // the user has changed the quantity with the increase or decrease buttons.
        mQuantityEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mProductHasChanged = true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSelectedImageUri != null)
            outState.putString(STATE_URI, mSelectedImageUri.toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_URI) &&
                !savedInstanceState.getString(STATE_URI).equals("")) {
            mSelectedImageUri = Uri.parse(savedInstanceState.getString(STATE_URI));
            mSelectedImageUriString = mSelectedImageUri.toString();

            ViewTreeObserver viewTreeObserver = mProductImageView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mProductImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mProductImageView.setImageBitmap(getBitmapFromUri(mSelectedImageUri));
                }
            });
        }
    }

    /**
     * This method is called when the decrease button is clicked.
     */
    private void decreaseQuantity() {
        int quantity;
        try {
            quantity = Integer.parseInt(mQuantityEditText.getText().toString().trim());
        } catch (NumberFormatException e) {
            quantity = 0;
        }
        if (quantity == 0) {
            // Show an error message as toast
            Toast.makeText(this, getString(R.string.error_quantity_below_zero),
                    Toast.LENGTH_SHORT).show();
            // Exit this method early because there's nothing left to do
            return;
        } else {
            quantity = quantity - 1;
            displayQuantity(quantity);
        }
    }

    /**
     * This method is called when the increase button is clicked.
     */
    private void increaseQuantity() {
        int quantity;
        try {
            quantity = Integer.parseInt(mQuantityEditText.getText().toString().trim());
        } catch (NumberFormatException e) {
            quantity = 0;
        }

        quantity += 1;
        displayQuantity(quantity);
    }

    /**
     * Helper method to display the given quantity value on the screen
     */
    private void displayQuantity(int number) {
        mQuantityEditText.setText(String.valueOf(number));
    }

    /**
     * Get user input from editor and save product into database.
     */
    private void saveProduct() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        int drawableId = (Integer) mProductImageView.getTag();

        // Check if this is supposed to be a new product
        // and check if all the fields in the editor are blank
        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(quantityString) && TextUtils.isEmpty(supplierString) &&
                drawableId == R.drawable.placeholder) {
            return;
        }

        if (!nameString.isEmpty() && !priceString.isEmpty() && !quantityString.isEmpty() &&
                !supplierString.isEmpty() && mSelectedImageUri != null) {
            // Create a ContentValues object where column names are the keys,
            // and product attributes from the editor are the values.
            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
            values.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceString);

            // If the quantity is not provided by the user, don't try to parse the string into an
            // integer value. Use 0 by default.
            int quantity = 0;
            if (!TextUtils.isEmpty(quantityString)) {
                quantity = Integer.parseInt(quantityString);
            }
            values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
            values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplierString);
            values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, mSelectedImageUriString);

            // Determine if this is a new or existing product by checking if mCurrentProductUri is null or not
            if (mCurrentProductUri == null) {
                // This is a NEW product, so insert a new product into the provider,
                // returning the content URI for the new product.
                Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

                // Show a toast message depending on whether or not the insertion was successful
                if (newUri == null) {
                    // If the new content URI is null, then there was an error with insertion.
                    Toast.makeText(this, getString(R.string.detail_insert_product_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the insertion was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.detail_insert_product_successful),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentProductUri
                // and pass in the new ContentValues. Pass in null for the selection and selection args
                // because mCurrentProductUri will already identify the correct row in the database that
                // we want to modify.
                int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

                // Show a toast message depending on whether or not the update was successful.
                if (rowsAffected == 0) {
                    // If no rows were affected, then there was an error with the update.
                    Toast.makeText(this, getString(R.string.detail_update_product_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the update was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.detail_update_product_successful),
                            Toast.LENGTH_SHORT).show();
                }
            }
            // Exit activity
            finish();
        } else {
            Toast.makeText(this, getString(R.string.empty_fields_toast_message),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);

            MenuItem orderMenuItem = menu.findItem(R.id.action_send_mail);
            orderMenuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                saveProduct();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            case R.id.action_send_mail:
                // Prepare an intent with the details of the current product and fill in the
                // subject and text fields of an email
                sendMail();
                // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * The onCreateLoader method creates the CursorLoader, and so, defines the data that we want
     * to query from the content provider.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_PRODUCT_IMAGE
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,       // Parent activity context
                mCurrentProductUri,                 // Query the content URI for the current product
                projection,                         // Columns to include in the resulting Cursor
                null,                      // No selection clause
                null,                   // No selection arguments
                null);                     // Default sort order
    }

    /**
     * The onLoadFinished method is called when the loader has finished loading the data and
     * has a Cursor. The Cursor that we get back from here, can be passed into the CursorAdapter
     * using the swapCursor method. The swapCursor method allows to change the cursor that the
     * adaptor gets it's data from.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            String imageUri = cursor.getString(imageColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText(Integer.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));
            mSupplierEditText.setText(supplier);
            if (imageUri == null || TextUtils.isEmpty(imageUri)) {
                mProductImageView.setImageResource(R.drawable.placeholder);
            } else {
                mSelectedImageUri = Uri.parse(imageUri);
                mProductImageView.setImageBitmap(getBitmapFromUri(mSelectedImageUri));
            }
        }
    }

    /**
     * The onLoaderReset method is called when the current loader is destroyed and the data in the
     * most recently provided cursor is invalid
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mSupplierEditText.setSelection(0);
        mProductImageView.setImageBitmap(getBitmapFromUri(Uri.parse(PLACEHOLDER_IMAGE_URI)));
        mProductImageView.setTag(R.drawable.placeholder);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.detail_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.detail_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    public void selectImage() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Product Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                mSelectedImageUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + mSelectedImageUri.toString());

                mSelectedImageUriString = (mSelectedImageUri.toString());
                mProductImageView.setImageBitmap(getBitmapFromUri(mSelectedImageUri));
            } else {
                mSelectedImageUriString = "";
            }
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = mProductImageView.getWidth();
        int targetH = mProductImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    /**
     * This method is called when the "Order more" button is clicked from the options menu drawer
     */
    private void sendMail() {
        String productName = mNameEditText.getText().toString().trim();
        String productSupplier = mSupplierEditText.getText().toString().trim();

        String orderMessage = createOrderSummary(productSupplier, productName);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, orderMessage);
        // Verify that there is an email app that can handle sending emails,
        // and start activity only if there is one. Otherwise do nothing.
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    /**
     * Create summary of the order.
     *
     * @param supplier    of the order
     * @param productName is the name of the product to order
     * @return text summary
     */
    private String createOrderSummary(String supplier, String productName) {
        // finds and substitutes the string with the id order_summary_supplier, and inserts the
        // name variable in the blank space
        // ex. "Supplier: %s", where %s is the supplier from where the inventory can order
        String orderMessage = getString(R.string.order_summary_supplier, supplier);
        orderMessage += "\n\n" + getString(R.string.order_summary_text_body, productName);
        orderMessage += "\n\n" + getString(R.string.kind_regards);
        return orderMessage;
    }
}
