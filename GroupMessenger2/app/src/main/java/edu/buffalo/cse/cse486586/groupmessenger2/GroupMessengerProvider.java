package edu.buffalo.cse.cse486586.groupmessenger2;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * <p/>
 * Please read:
 * <p/>
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * <p/>
 * before you start to get yourself familiarized with ContentProvider.
 * <p/>
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 */
public class GroupMessengerProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        ContentValues insert = new ContentValues();
        String key = values.get("key").toString();
        insert.put("key", key);
        String val = values.get("value").toString();
        insert.put("value", val);
        Uri uri1 = uri.withAppendedPath(uri, key);
        Log.i("Inserting " + key, val);
        try {
            FileOutputStream fos = getContext().openFileOutput(key, Context.MODE_PRIVATE);
            fos.write(val.getBytes());
            fos.close();

        } catch (FileNotFoundException e) {
            //e.printStackTrace();

        } catch (IOException e) {
            // e.printStackTrace();
        }

        Log.v("insert", values.toString());

        return uri1;

    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */

        String key = selection;
        MatrixCursor matrixCursor = null;

        try {

            FileInputStream fis = getContext().openFileInput(key);
            StringBuffer fileContent = new StringBuffer("");

            byte[] buffer = new byte[1024];
            int n = fis.read(buffer);

            fileContent.append(new String(buffer, 0, n));

            String val = fileContent.toString();
            String arr[] = {"key", "value"};
            String rows[] = {key, val};
            matrixCursor = new MatrixCursor(arr);
            matrixCursor.addRow(rows);
            matrixCursor.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {

        }

        Log.v("query", selection);
        return matrixCursor;

    }
}
