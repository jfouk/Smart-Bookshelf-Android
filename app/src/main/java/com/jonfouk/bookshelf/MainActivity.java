package com.jonfouk.bookshelf;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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

        // create dialogue for Floating Action Button
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialogue_progress_bar_staged, null));
//        builder.setMessage("Please scan book barcode on bookshelf camera until camera light turns off..");
        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // add a book
                dialog.show();
                ProgressBar getInfoBar = (ProgressBar)dialog.findViewById(R.id.getinfo_progressbar);
                getInfoBar.setVisibility(View.INVISIBLE);
                mRpiInterface.addBook(dialog);

            }
        });

        PreferenceManager.setDefaultValues(this,R.xml.preferences,false);
        // initialize variables
        mBookShelf = BookShelf.getBookShelf();
        if ( mBookShelf.needsInit() )
        {
            mBookShelf.init(this, Glide.with(this));
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
            // inflate settings fragment
//            SettingsFragment settingsFrag = new SettingsFragment();
//
//            getFragmentManager().beginTransaction()
//                    .replace(android.R.id.content, settingsFrag)
//                    .addToBackStack(null)
//                    .commit();
            Intent intent = new Intent(this,PreferenceActivity.class);
            startActivity(intent);
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
