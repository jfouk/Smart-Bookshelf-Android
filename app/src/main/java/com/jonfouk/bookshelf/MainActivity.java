package com.jonfouk.bookshelf;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.jonfouk.bookshelf.BookshelfMgr.Book;
import com.jonfouk.bookshelf.BookshelfMgr.BookRVAdapter;
import com.jonfouk.bookshelf.BookshelfMgr.BookShelf;
import com.jonfouk.bookshelf.Server.RpiInterface;
import com.jonfouk.bookshelf.database.BookDatabaseContract;
import com.jonfouk.bookshelf.database.BookDatabaseHelper;
import com.jonfouk.bookshelf.simplehttp.SimpleHttp;
import com.jonfouk.bookshelf.simplehttp.SimpleHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    public static final String TAG="MainActivity";
    public static final String EXTRA_ISBN = "BOOK_ISBN";
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private BookRVAdapter mBookRVAdapter;
    private BookShelf mBookShelf;
    private RpiInterface mRpiInterface; //@todo remove this once we figure out where we want to put this
    private MainUtility mUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accessUrl();
            }
        });

        // initialize variables
        mBookShelf = BookShelf.getBookShelf();
        if ( mBookShelf.needsInit() )
        {
            mBookShelf.init(this);
        }
        mLayoutManager = new LinearLayoutManager(this);
        mBookRVAdapter = new BookRVAdapter(mBookShelf.getBookList(),this);

        // initialize recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.rv);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mBookRVAdapter);

        // initialize rpiinterface
        mRpiInterface = RpiInterface.getRpiInterface();
        mRpiInterface.init(this,mBookShelf,mBookRVAdapter);
        // initialize swipe to refresh
        final SwipeRefreshLayout refreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_swipe_refresh_layout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //try to update bookshelf
                pRefreshBookshelf(refreshLayout);
            }
        });

        mUtility = new MainUtility(mBookRVAdapter, refreshLayout); //@todo remove if not needed
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            printDb();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        // on pause, we will save the current book list to the database
        // @todo recovery actions if this fails
        mBookShelf.writeBookDb();
        Log.d(TAG,"On Pause");
        super.onPause();
    }

    // access server and grab db in content values
    public void accessUrl() {
        //final TextView ourText = (TextView) findViewById(R.id.textView);

        // initialize return content values
        SimpleHttp.get("http://192.168.1.141:5000", new SimpleHttpResponseHandler() {
            @Override
            public void onResponse(int responseCode, String responseBody) {
                Toast.makeText(MainActivity.this, "Url received " + responseCode, Toast.LENGTH_SHORT).show();
                JSONArray json = null;
                ContentValues values;
                if ( responseCode == 200 ) {
                    try {
                        json = new JSONArray(responseBody);
                        values = new ContentValues();
                        String result = "";
                        // iterate through JSON array and print out results
                        for ( int i = 0; i < json.length(); i++)
                        {
                            JSONObject row = json.getJSONObject(i);
                            /*
                            result = result + "ISBN: " + row.get("ISBN").toString() + "\n";
                            result = result + "NAME: " + row.get("NAME").toString() + "\n";
                            result = result + "\n";
                            */
                            values.put(BookDatabaseContract.BookEntry.COL_NAME_ISBN, row.get("ISBN").toString());
                            values.put(BookDatabaseContract.BookEntry.COL_NAME_NAME, row.get("NAME").toString());
                            //values.put(BookDatabaseContract.BookEntry.COL_NAME_WIDTH, row.get("WIDTH").toString());
                            values.put(BookDatabaseContract.BookEntry.COL_NAME_WIDTH, 1);
                            values.put(BookDatabaseContract.BookEntry.COL_NAME_CHECKED_IN, row.get("CHECKED_IN").toString());
                            values.put(BookDatabaseContract.BookEntry.COL_NAME_ROW, row.get("ROW").toString());
                            values.put(BookDatabaseContract.BookEntry.COL_NAME_POSITION, row.get("POSITION").toString());
                            //addContentToDb(values);
                            Book tempBook = new Book(row.getString("ISBN"),row.get("NAME").toString(),1,row.getInt("CHECKED_IN"),
                                    row.getInt("ROW"),row.getInt("POSITION"),null,false);
                            mBookShelf.addBook(tempBook);
                        }
                        //ourText.setText(result);

                        // add this stuff to the db!
                    } catch (JSONException e) {
                        Log.e(SimpleHttp.TAG, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void addContentToDb( ContentValues values ) {
        // initialize database
        final BookDatabaseHelper mDbHelper = new BookDatabaseHelper(this);
        // create db to read
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        long newRowId = db.insert(BookDatabaseContract.BookEntry.TABLE_NAME,null,values);

    }

    public void printDb() {
        // initialize database
        final BookDatabaseHelper mDbHelper = new BookDatabaseHelper(this);
        // create db to read
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                BookDatabaseContract.BookEntry._ID,
                BookDatabaseContract.BookEntry.COL_NAME_ISBN,
                BookDatabaseContract.BookEntry.COL_NAME_NAME,
                BookDatabaseContract.BookEntry.COL_NAME_WIDTH,
                BookDatabaseContract.BookEntry.COL_NAME_CHECKED_IN,
                BookDatabaseContract.BookEntry.COL_NAME_ROW,
                BookDatabaseContract.BookEntry.COL_NAME_POSITION,
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                BookDatabaseContract.BookEntry.COL_NAME_NAME + " DESC";

        Cursor cursor = db.query(
                BookDatabaseContract.BookEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        // set up text view to update
        //final TextView ourText = (TextView) findViewById(R.id.textView);
        String result = "";

        // iterate through db
        while( cursor.moveToNext()) {
            result = result + BookDatabaseContract.BookEntry.COL_NAME_ISBN + ": "
                    + cursor.getInt( cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_ISBN)) + "\n";
            result = result + BookDatabaseContract.BookEntry.COL_NAME_NAME + ": "
                    + cursor.getString( cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_NAME)) + "\n";
            result = result + BookDatabaseContract.BookEntry.COL_NAME_WIDTH + ": "
                    + cursor.getFloat( cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_WIDTH)) + "\n";
            result = result + BookDatabaseContract.BookEntry.COL_NAME_CHECKED_IN + ": "
                    + cursor.getInt( cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_CHECKED_IN)) + "\n";
            result = result + BookDatabaseContract.BookEntry.COL_NAME_ROW + ": "
                    + cursor.getInt( cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_ROW)) + "\n";
            result = result + BookDatabaseContract.BookEntry.COL_NAME_POSITION + ": "
                    + cursor.getFloat( cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_POSITION)) + "\n";
            result = result + "\n";
        }
        //ourText.setText(result);

    }
    private void pRefreshBookshelf(SwipeRefreshLayout refreshLayout)
    {
        mRpiInterface.requestBooks(refreshLayout);
    }
}
