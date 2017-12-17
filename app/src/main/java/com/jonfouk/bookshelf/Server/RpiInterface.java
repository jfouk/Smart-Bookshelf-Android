package com.jonfouk.bookshelf.Server;

import android.content.ContentValues;
import android.content.Context;
import android.os.StrictMode;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.widget.Toast;

import com.jonfouk.bookshelf.BookshelfMgr.Book;
import com.jonfouk.bookshelf.BookshelfMgr.BookRVAdapter;
import com.jonfouk.bookshelf.BookshelfMgr.BookShelf;
import com.jonfouk.bookshelf.database.BookDatabaseContract;
import com.jonfouk.bookshelf.simplehttp.SimpleHttp;
import com.jonfouk.bookshelf.simplehttp.SimpleHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by joncf on 4/11/2017.
 */

/// @brief Raspberry Pi Interface
/// This class handles all the communicating with the rpi bookshelf
public class RpiInterface {
    public static final String TAG = RpiInterface.class.getName();

    /// routing info
    public static final String delete_route="/delete";
    public static final String check_out_route="/checkout";
    public static final String check_in_route="/checkin";

    /// Connection info
    public static String base_addr = "192.168.1.141";
    public static String web_addr = "http://192.168.1.141";
    public static String port = ":5000";
    public static String request_addr = web_addr+port;
    public static String delete_addr = web_addr+port+delete_route;
    public static String checkout_addr = web_addr+port+check_out_route;
    public static String checkin_addr = web_addr+port+check_in_route;

    private static RpiInterface mRpiInterface;  ///< single instance of the interface
    private Context mContext;
    private BookShelf mBookshelf;
    private BookRVAdapter mRvAdapter;
    private boolean mIsInit;                    ///< is this already initialized

    /// functions
    private RpiInterface() {
        mContext = null;
        mBookshelf = null;
        mRvAdapter = null;
        mIsInit = false;
    }
    public void init(Context context, BookShelf bookShelf, BookRVAdapter rvAdapter)
    {
        this.mContext = context;
        if (bookShelf != null) {
            this.mBookshelf = bookShelf;
        }
        else
        {
            Log.e(TAG,"Init: Bookshelf not initialized!");
        }
        this.mRvAdapter = rvAdapter;
        mIsInit = true;
    }

    public static RpiInterface getRpiInterface()
    {
        if (mRpiInterface == null )
        {
            mRpiInterface = new RpiInterface();
        }
        return mRpiInterface;
    }

    public static String getBase_addr() {
        return base_addr;
    }

    public static void setBase_addr(String base_addr) {
        RpiInterface.base_addr = base_addr;
    }

    ///@brief Request books from the rpi
    /// This will update the passed in bookshelf with everything that's in the
    /// rpi database. If the book already exists, it will update the info, if not
    /// it will create a new copy. No books are deleted from the android bookshelf
    /// @note this will only be called by main activity
    /// @param[in] final bookshelf      bookshelf by reference that we want to write to
    /// @param[in] final swiperefreshlayout      refreshLayout that we will notify when complete
    /// @param[in] final rvAdapter      rvAdapter that we will notify when complete
    public void requestBooks(final SwipeRefreshLayout refreshLayout)
    {
        // ping to check if on the same network as adapter first
        Boolean rc = pPingBookshelf();
        if ( rc == true) {
            // initialize return content values
            SimpleHttp.get(request_addr, new SimpleHttpResponseHandler() {
                @Override
                public void onResponse(int responseCode, String responseBody) {
                    Log.i(TAG, "Request Books: " + responseCode);

                    JSONArray json = null;
                    ContentValues values;
                    if (responseCode == 200) {
                        try {
                            json = new JSONArray(responseBody);
                            values = new ContentValues();
                            String result = "";
                            // iterate through JSON array and print out results
                            for (int i = 0; i < json.length(); i++) {
                                JSONObject row = json.getJSONObject(i);
                                values.put(BookDatabaseContract.BookEntry.COL_NAME_ISBN, row.get("ISBN").toString());
                                values.put(BookDatabaseContract.BookEntry.COL_NAME_NAME, row.get("NAME").toString());
                                //values.put(BookDatabaseContract.BookEntry.COL_NAME_WIDTH, row.get("WIDTH").toString());
                                values.put(BookDatabaseContract.BookEntry.COL_NAME_WIDTH, 1);
                                values.put(BookDatabaseContract.BookEntry.COL_NAME_CHECKED_IN, row.get("CHECKED_IN").toString());
                                values.put(BookDatabaseContract.BookEntry.COL_NAME_ROW, row.get("ROW").toString());
                                values.put(BookDatabaseContract.BookEntry.COL_NAME_POSITION, row.get("POSITION").toString());
                                //addContentToDb(values);
                                Book tempBook = new Book(row.getString("ISBN"), row.get("NAME").toString(), 1, row.getInt("CHECKED_IN"),
                                        row.getInt("ROW"), row.getInt("POSITION"), null, false);
                                mBookshelf.addBook(tempBook);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(mContext, "Unable to connect to bookshelf, return code: " + responseCode, Toast.LENGTH_LONG).show();
                    }
                    refreshLayout.setRefreshing(false);
                    mRvAdapter.notifyDataSetChanged();
                }
            });
        }
        else
        {
            // error toast should already be logged
            refreshLayout.setRefreshing(false);

        }
    }

    public void deleteBook( final Book book ) {
        // first ping bookshelf
        Boolean rc = pPingBookshelf();
        if ( rc == true ) { //no need to check for error case since messages already logged
            Map<String, String> params = new HashMap<>();
            params.put("ISBN", book.getISBN());
            SimpleHttp.post(delete_addr, params, new SimpleHttpResponseHandler() {
            //SimpleHttp.post("http://httpbin.org/post", params, new SimpleHttpResponseHandler() {
                @Override
                public void onResponse(int responseCode, String responseBody) {
                    try {

                        if (responseCode == 200) {
                            Log.d(TAG, responseBody);
                            JSONObject json = new JSONObject(responseBody);
                            //Log.d(TAG, json.getString("ISBN"));
                            if (json.getInt("result") == 1) {   // successfully deleted
                                mBookshelf.deleteBook(book);
                            } else {
                                Log.e(TAG, "Connected bookshelf failed to delete book " + book.getISBN());
                                Toast.makeText(mContext, "Connected bookshelf failed to delete book " + book.getISBN()
                                        , Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e(TAG, "Failed to connect to the bookshelf, return code: " + responseCode);
                            Toast.makeText(mContext, "Unable to connect to bookshelf, return code: " + responseCode, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                    mRvAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    /// interface to check in/out the book
    /// @param[in] book     the book object to checkin/out
    /// @param[in] checkOut if true, check out, if false, check in
    public void checkOutBook( final Book book, final Boolean checkOut)
    {
        // first ping bookshelf
        Boolean rc = pPingBookshelf();
        if ( rc == true )
        {
            String url = checkOut?checkout_addr:checkin_addr;   // choose the right address
            Map<String, String> params = new HashMap<>();
            params.put("ISBN", book.getISBN());
            SimpleHttp.post(url, params, new SimpleHttpResponseHandler() {
                @Override
                public void onResponse(int responseCode, String responseBody) {
                    try {

                        Boolean innerRc = true;
                        if (responseCode == 200) {
                            Log.d(TAG, responseBody);
                            JSONObject json = new JSONObject(responseBody);
                            if (json.getInt("result") == 1) {   // successfully checked in /out
                                if (checkOut){
                                    innerRc  = mBookshelf.checkOutBook(book);
                                }else {
                                    innerRc = mBookshelf.checkInBook(book);
                                }
                                // update button text
//                                if ( innerRc == true )
 //                               {
  //                                  button.setText( (!checkOut)?"Checkout":"Check-in");
   //                             }
                            } else {
                                Log.e(TAG, "Connected bookshelf failed to checkout book " + book.getISBN());
                                Toast.makeText(mContext, "Connected bookshelf failed to checkout book " + book.getISBN()
                                        , Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e(TAG, "Failed to connect to the bookshelf, return code: " + responseCode);
                            Toast.makeText(mContext, "Unable to connect to bookshelf, return code: " + responseCode, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                    mRvAdapter.notifyDataSetChanged();
                }
            });

        }

    }


    private Boolean pPingBookshelf()
    {
        Boolean rc = false;
        // ping to check if network is reachable
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            //if (InetAddress.getByName(mRpiInterface.getBase_addr()).isReachable(1000))
            if (InetAddress.getByName(base_addr).isReachable(500))
            {
                rc = true;
            }
            else
            {
                Toast.makeText(mContext,"Unable to connect to the bookshelf, not on the same network!", Toast.LENGTH_LONG).show();
            }
        }catch (UnknownHostException e) //not connected to the bookshelf
        {
            Log.e(TAG,e.getMessage());
            Toast.makeText(mContext,"Unable to connect to the bookshelf, not on the same network!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
        }
        return rc;
    }

}
