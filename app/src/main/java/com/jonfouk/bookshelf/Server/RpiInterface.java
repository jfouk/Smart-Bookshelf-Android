package com.jonfouk.bookshelf.Server;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.StrictMode;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.jonfouk.bookshelf.BookDetail;
import com.jonfouk.bookshelf.BookshelfMgr.Book;
import com.jonfouk.bookshelf.BookshelfMgr.BookRVAdapter;
import com.jonfouk.bookshelf.BookshelfMgr.BookShelf;
import com.jonfouk.bookshelf.R;
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
    public static final String init_route="/init";
    public static final String confirm_route="/confirm";
    public static final String add_route="/add";
    /// Connection info
    public static String base_addr = "192.168.1.141";
    public static String web_addr = "http://192.168.1.141";
    public static String port = ":5000";
    public static String request_addr = web_addr+port;
    public static String delete_addr = web_addr+port+delete_route;
    public static String checkout_addr = web_addr+port+check_out_route;
    public static String checkin_addr = web_addr+port+check_in_route;
    public static String init_addr = web_addr+port+init_route;
    public static String confirm_addr = web_addr+port+confirm_route;
    public static String add_addr = web_addr+port+add_route;

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
//            this.mBookshelf = bookShelf;
            this.mBookshelf = BookShelf.getBookShelf();
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

    // change static values stored on the bookshelf
    // only call this class if this information has changed
    // values stored:
    //      ledWidth
    //      offset
    //      rows
    //      numLeds
    public void initShelf(Context context)
    {
        // first ping bookshelf
        Boolean rc = pPingBookshelf();
        if ( rc == true ) { //no need to check for error case since messages already logged
            // get preference values
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            int rowNum = Integer.parseInt(sharedPref.getString("row_num","0"));
            Map<String, String> params = new HashMap<>();
            params.put("ledWidth", sharedPref.getString("ledWidth","0"));
            params.put("offset", sharedPref.getString("offset","0"));
            params.put("rows", sharedPref.getString("row_num","0"));
            params.put("numLeds", sharedPref.getString("numLeds","0"));
            for ( int i =0;i < rowNum; i++)
            {
                params.put("width"+i,sharedPref.getString("pref_width"+i,"0"));
                params.put("height"+i,sharedPref.getString("pref_height"+i,"0"));
            }

            SimpleHttp.post(init_addr, params, new SimpleHttpResponseHandler() {

                @Override
                public void onResponse(int responseCode, String responseBody) {
                    try {

                        if (responseCode == 200) {
                            Log.d(TAG, responseBody);
                            JSONObject json = new JSONObject(responseBody);
                            //Log.d(TAG, json.getString("ISBN"));
                            if (json.getInt("result") == 1) {   // successfully init
                                Log.i(TAG, "Bookshelf values successfully initialized");
                            } else {
                                Log.e(TAG, "Connected bookshelf failed to set values");
                                Toast.makeText(mContext, "Connected bookshelf failed to store values"
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
                }
            });
        }
    }

    // add book
    // API call to add a new book (invoke camera to scan a new barcode )
    // once book is added, update all book values again incase things have shifted
    public void addBook(final AlertDialog dialog)
    {
        // ping to check if on the same network as adapter first
        Boolean rc = pPingBookshelf();
        if ( rc == true) {
            // initialize return content values
            SimpleHttp.get(add_addr, new SimpleHttpResponseHandler() {
                @Override
                public void onResponse(int responseCode, String responseBody) {
                    Log.i(TAG, "Request Books: " + responseCode);

                    // create response dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(dialog.getContext());
                    builder.setMessage("Unable to scan book, please try again!");
                    final AlertDialog confirmDialog = builder.create();
                    confirmDialog.setButton(DialogInterface.BUTTON_POSITIVE,"Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            confirmDialog.dismiss();

                        }
                    });

                    if (responseCode == 200) {
//                        pAddBooksToDB(responseBody);
//                        dialog.dismiss();
                        ProgressBar scanProgressBar = (ProgressBar)dialog.findViewById(R.id.scan_progressbar);
                        ProgressBar getInfoProgressBar = (ProgressBar)dialog.findViewById(R.id.getinfo_progressbar);
                        TextView scanText = (TextView)dialog.findViewById(R.id.scan_text);
                        scanText.setPaintFlags(scanText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        scanProgressBar.setVisibility(View.INVISIBLE);

                        getInfoProgressBar.setVisibility(View.VISIBLE);
                        acknowledgeBookScan(getInfoProgressBar,dialog);
                    } else if (responseCode == 201){
                        Toast.makeText(mContext, "Unable to scan book, return code: " + responseCode, Toast.LENGTH_LONG).show();


                        confirmDialog.show();
                        dialog.dismiss();

                    } else {
                        Toast.makeText(mContext, "Unable to connect to bookshelf, return code: " + responseCode, Toast.LENGTH_LONG).show();

                        confirmDialog.setMessage("Unable to connect to the bookshelf, please make sure you are on the same network!");
                        confirmDialog.show();
                        dialog.dismiss();
                    }
                    mRvAdapter.notifyDataSetChanged();
                }
            });
        }else {
            dialog.dismiss();
        }
    }

    public void acknowledgeBookScan(final ProgressBar infoProgressBar, final AlertDialog dialog )
    {
        SimpleHttp.get(confirm_addr, new SimpleHttpResponseHandler() {
            @Override
            public void onResponse(int responseCode, String responseBody) {
                if (responseCode == 200)
                {
                    infoProgressBar.setProgress(100);
                    dialog.dismiss();
                    confirmBookPlaced(true,mContext);


                }
            }
        });
    }

    public void confirmBookPlaced(Boolean isPlace, Context context)
    {
        // create response dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Please place book at highlighted location on bookshelf.");
        builder.setCancelable(false);
        final AlertDialog confirmDialog = builder.create();

        confirmDialog.setButton(DialogInterface.BUTTON_POSITIVE,"Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SimpleHttp.get(confirm_addr, new SimpleHttpResponseHandler() {
                    @Override
                    public void onResponse(int responseCode, String responseBody) {
                        if ( responseCode != 200 )
                        {
                            Log.d(TAG, "Unable to confirm book placement!");
                        }
                    }
                });
                confirmDialog.dismiss();

            }
        });
        // if we are placing the book onto the shelf
        if ( isPlace )
        {
            confirmDialog.show();
        }
        else
        {
            confirmDialog.setMessage("Please take book from highlighted location on bookshelf");
            confirmDialog.show();
        }
        requestBooks(); //refresh the books while waiting for user to place book
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

                    if (responseCode == 200) {
                        pAddBooksToDB(responseBody);
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
    public void requestBooks()
    {
        // ping to check if on the same network as adapter first
        Boolean rc = pPingBookshelf();
        if ( rc == true) {
            // initialize return content values
            SimpleHttp.get(request_addr, new SimpleHttpResponseHandler() {
                @Override
                public void onResponse(int responseCode, String responseBody) {
                    Log.i(TAG, "Request Books: " + responseCode);

                    if (responseCode == 200) {
                        pAddBooksToDB(responseBody);
                    } else {
                        Toast.makeText(mContext, "Unable to connect to bookshelf, return code: " + responseCode, Toast.LENGTH_LONG).show();
                    }

                    mRvAdapter.notifyDataSetChanged();
                }
            });
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
    /// @param[in] detail   reference to BookDetail object so we can update it
    public void checkOutBook(final Book book, final Boolean checkOut, final BookDetail detail, final Context context)
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
                                    detail.updateBook();
                                    confirmBookPlaced(false,context);   //might need different context

                                }else {
                                    innerRc = mBookshelf.checkInBook(book);
                                    detail.updateBook();
                                    confirmBookPlaced(true,context);
                                }
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

    private void pAddBooksToDB(String responseBody)
    {
        JSONArray json;
        ContentValues values;
        try {
            json = new JSONArray(responseBody);
            Log.d(TAG,responseBody);

            // iterate through JSON array and save results
            for (int i = 0; i < json.length(); i++) {
                JSONObject row = json.getJSONObject(i);

                Book tempBook = new Book(row.getString("ISBN"),
                                         row.get("NAME").toString(),
                                        row.getInt("WIDTH"),
                                        row.get("AUTHOR").toString(),
                                        row.getInt("CHECKED_IN"),
                                        row.getInt("ROW"),
                                        row.getInt("POSITION"),
                                        row.get("LAST_DATE").toString(),
                                        row.get("PIC_URL").toString(),
                                        null, false);
                mBookshelf.addBook(tempBook);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();

        }

    }



}
