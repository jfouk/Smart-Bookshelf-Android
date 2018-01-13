package com.jonfouk.bookshelf;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.jonfouk.bookshelf.BookshelfMgr.Book;
import com.jonfouk.bookshelf.BookshelfMgr.BookShelf;
import com.jonfouk.bookshelf.Server.RpiInterface;

public class BookDetail extends AppCompatActivity {

    public static final String TAG="BookDetail";
    private Book mBook;
    public final BookDetail mRef = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        Toolbar toolbar = (Toolbar)findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String bookIsbn = intent.getStringExtra(MainActivity.EXTRA_ISBN);

        // get temporary bookshelf
        BookShelf bookshelf = BookShelf.getBookShelf();
        if ( bookshelf.needsInit())
        {
            bookshelf.init(this, Glide.with(this));
        }

        // get the book and check if it's null
        Book book = bookshelf.getBook(bookIsbn);
        if (book != null)
        {
            // save a copy of the current book
            mBook = book;
            printBookInfo(book);
        }
        else    // if can't find book, exit now
        {
            Log.e(TAG,"Unable to find book " + bookIsbn);
            Toast.makeText(this,"Unable to find book " + bookIsbn,Toast.LENGTH_LONG);
            finish();
        }

        // @note after this point, it is assumed that book has been found

        // setup buttons

        // checked out button. IF book is checked out, this is a check in button, otherwise
        // it is a checked out button
        final Button checkOutButton = (Button) findViewById(R.id.checkout_button);
        // change text to approriate text
        checkOutButton.setText( (mBook.getCheckedIn()==1)?"Checkout":"Check-in");
        checkOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                RpiInterface rpiInterface = RpiInterface.getRpiInterface();
                rpiInterface.checkOutBook(mBook,mBook.getCheckedIn()==1, mRef,BookDetail.this);
            }
        });



    }

    // update checked in button text with current info
    protected void updateCheckInButton() {
        final Button checkOutButton = (Button) findViewById(R.id.checkout_button);
        checkOutButton.setText( (mBook.getCheckedIn()==1)?"Checkout":"Check-in");

        final CheckedTextView checkedInInfo = (CheckedTextView)findViewById(R.id.checkedin_check);
        checkedInInfo.setChecked(mBook.getCheckedIn()==1);  // set check mark if book is checked in
        if ( checkedInInfo.isChecked())
        {
            checkedInInfo.setCheckMarkDrawable(android.R.drawable.checkbox_on_background);
        }
        else
        {
            checkedInInfo.setCheckMarkDrawable(android.R.drawable.checkbox_off_background);
        }
        return;
    }

    // update the book values (in case anything changed, since we can't hold a pointer to the object)
    public void updateBook() {
        // get temporary bookshelf
        BookShelf bookshelf = BookShelf.getBookShelf();
        if ( bookshelf.needsInit())
        {
            bookshelf.init(this, Glide.with(this));
        }

        Book book = bookshelf.getBook(mBook.getISBN());
        if (book != null)
        {
            // save a copy of the current book
            mBook = book;
            Log.i(TAG,"is Checked in?" + mBook.getCheckedIn());
            printBookInfo(book);
        }
        else    // if can't find book, exit now
        {
            Log.e(TAG,"Unable to find book " + mBook.getISBN());
            Toast.makeText(this,"Unable to find book " + mBook.getISBN(),Toast.LENGTH_LONG);
            finish();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_book_detail,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_delete:
                // user chose to delete book, show delete dialog
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                RpiInterface rpiInterface = RpiInterface.getRpiInterface();
                                rpiInterface.deleteBook(mBook);
                                finish();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                // No button clicked, do nothing
                                break;
                        }
                    }
                };

                // display warning message
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Deleting this book will remove it permanently from the library." +
                        " Are you sure?").setPositiveButton("Delete book",dialogClickListener).setNegativeButton("No",dialogClickListener).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        // on pause, we will save the current book list to the database
        // @todo recovery actions if this fails
        BookShelf.getBookShelf().writeBookDb();
        Log.d(TAG,"On Pause");
        super.onPause();
    }

    public void printBookInfo(Book book )
   {
       final TextView book_title = (TextView) findViewById(R.id.book_title);
       final TextView book_author = (TextView) findViewById(R.id.author);
       final ImageView book_image = (ImageView) findViewById(R.id.book_image);
       final TextView last_date = (TextView) findViewById(R.id.last_date);

       String author = "";
       author = author + book.getAuthor();
       book_title.setText(book.getName());
       book_author.setText(author);
       book_image.setImageBitmap(book.getCoverImage());

       // set last date text
       if (book.getCheckedIn() == 1)
       {
           // if book is checked in, last checked in date
           last_date.setText("Last checked in: "+book.getLastDate());
       }
       else
       {
           // otherwise, last checked out date
           last_date.setText("Last checked out: " + book.getLastDate());
       }
       updateCheckInButton();


       // build url from openlibrary
       //String url = "http://covers.openlibrary.org/b/isbn/" + book.getISBN()+ "-M.jpg";
      // String url = "http://covers.openlibrary.org/b/isbn/0545010225-M.jpg";
       //Glide.with(this).load(url).into(book_image);
   }
}
