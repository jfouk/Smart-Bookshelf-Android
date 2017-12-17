package com.jonfouk.bookshelf.BookshelfMgr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.jonfouk.bookshelf.Server.RpiInterface;
import com.jonfouk.bookshelf.database.BookDatabaseContract;
import com.jonfouk.bookshelf.database.BookDatabaseHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joncf on 2/4/2017.
 */

/// Main book shelf class, holds the database and books
public class BookShelf {
    // member data
    public static final String TAG="Bookshelf";

    private static BookShelf mTheBookShelf;              ///< the instance of the bookshelf
    private BookDatabaseHelper mBookDatabase;     ///< book database object
    private ArrayList<Book> mBookList;                  ///< dynamic array of books
    private int mNumBooks;                              ///< number of books
    private boolean mIsInit;                    ///< true if already initialized

    // functions
    private BookShelf()
    {
        mNumBooks = 0;
        mIsInit = false;
    }
    public void init(Context context) {
        mBookDatabase = new BookDatabaseHelper(context);
        mBookList = new ArrayList<Book>();
        readBookDb();

        // now that our booklist is initialized, we can get the number of books
        mNumBooks = mBookList.size();
        mIsInit = true;
    }

    public static BookShelf getBookShelf()
    {
        if (mTheBookShelf == null)
        {
            mTheBookShelf = new BookShelf();
        }
        return mTheBookShelf;
    }

    public boolean needsInit()
    {
        return !mIsInit;
    }

    public List<Book> getBookList()
    {
        return mBookList;
    }

    /// @brief getBook with the right ISBN number
    public Book getBook( String ISBN )
    {
        for ( Book book: mBookList)
        {
            if (book.ISBN.equals(ISBN))
            {
                return book;
            }
        }
        // if book was never found, then return null
        return null;
    }

    public boolean setPicture(String ISBN, Bitmap image )
    {
        boolean rc = true;
        // find book
          Book book = getBook(ISBN);
          // if we have found the book
          if (book != null)
          {
              // if image has not already been fetched
              if ( !book.isImageFetched())
              {
                  book.setCoverImage(image);
                  book.setImageFetched(true);
              }
          }
          else
          {
              rc = false;
          }
        return rc;
    }

    /// @brief add book to the book shelf/ updates the check in status
    /// First checks if book is already there, if so just updates info
    /// if not, adds in a new book
    /// @note db will not be updated till onpause is called
    public boolean addBook( Book book )
    {
        boolean rc = true;
        // check if book is already in list
        int bookIndex = 0;
        if ( -1 != ( bookIndex = mBookList.indexOf(book)))  // if found book
        {
            // update book with new info
            // if picture is not there don't wipe the picture
            mBookList.get(bookIndex).copy(book);
        }
        else    // else don't have book yet
        {
            // add to booklist
            Log.d(TAG,"New Book\n" +
                    "ISBN: " + book.getISBN() +
                    " Name: " + book.getName());
            mBookList.add( book );
        }
        return rc;
    }

    /// @brief delete book
    public boolean deleteBook( Book book )
    {
        boolean rc = true;
        // check if book is already in list
        int bookIndex = 0;
        if ( -1 != ( bookIndex = mBookList.indexOf(book)))  // if found book
        {
            // delete the book from database
            rc = pDeleteBook(book);

            if ( rc ) {
                // if successful, update book list with deleted book
                Book deletedBook = mBookList.remove(bookIndex);

                if (deletedBook != book)
                {
                    Log.e(TAG, "Unable to remove " + book.ISBN + " from book list!");
                    rc = false;
                }
            }

        }
        return rc;
    }

    // @brief Check in book
    // If book is checked out, check it in
    // otherwise, log that book is already checked in and return false
    public boolean checkInBook( Book book )
    {
        Boolean rc = false;

        // if book is checked out
        if (book.getCheckedIn() == 0)
        {
            book.checkedIn = 1;
            rc = addBook(book);
        }
        else
        {
            Log.d(TAG, "Book " + book.getISBN() + " is already checked in..");
        }
        return rc;
    }

    public boolean checkOutBook( Book book )
    {
        Boolean rc = false;
        // check if book is already in list
        int bookIndex = 0;
        if ( -1 != ( bookIndex = mBookList.indexOf(book)))  // if found book
        {
            // check if book is already checked out
            if ( mBookList.get(bookIndex).checkedIn == 1)
            {
                // if book is checked in we can check it out
                //@todo call api call to check out book
                RpiInterface rpiInterface = RpiInterface.getRpiInterface();
                // @note we pass in True if book is checked in because we want to check out
                rpiInterface.checkOutBook(book,book.getCheckedIn()==1);
                rc = true;
            }
            else
            {
                Log.d(TAG,"Book is already checkedout:" + book.getISBN());
            }
        }
        if ( rc == true )
        {
            // update list and db with the new checked in status
            book.checkedIn = 0; // book was just checked out!
            rc = addBook(book);
        }
        return rc;

    }

    ///@brief write to db
    /// This function writes the entire booklist array into the db
    public boolean writeBookDb()
    {
        boolean rc = true;
        for( Book book:mBookList)
        {
            // as long as we are not failing, keep updating the db
            if ( rc )
            {
                rc = pUpdateDbBook(book);
            }
            else
            {
                Log.d(TAG,"Write to db failed at book ISBN: " + book.getISBN());
            }
        }
        return rc;
    }

    /// @brief grab books from database and init into booklist
    /// This will assume that the booklist needs to be cleared, and will reset it with
    /// all the db data
    public void readBookDb(){
        int rc = 0;

        // clear the current list, since we are blind copying in the db
        mBookList.clear();
        // grab books from database
        SQLiteDatabase db = mBookDatabase.getReadableDatabase();

        String[] projection = {
                BookDatabaseContract.BookEntry._ID,
                BookDatabaseContract.BookEntry.COL_NAME_ISBN,
                BookDatabaseContract.BookEntry.COL_NAME_NAME,
                BookDatabaseContract.BookEntry.COL_NAME_WIDTH,
                BookDatabaseContract.BookEntry.COL_NAME_CHECKED_IN,
                BookDatabaseContract.BookEntry.COL_NAME_ROW,
                BookDatabaseContract.BookEntry.COL_NAME_POSITION,
                BookDatabaseContract.BookEntry.COL_NAME_COVER_IMAGE,
                BookDatabaseContract.BookEntry.COL_NAME_IMAGE_FETCHED,
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


        // Add books into booklist
        while( cursor.moveToNext()) {
            // extract picture
            Bitmap picture = null;
            Boolean imageFetched =
                    cursor.getInt(cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_IMAGE_FETCHED)) == 1;
            if (imageFetched) {
                picture = pGetImage(cursor.getBlob(cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_COVER_IMAGE)));
            }

            mBookList.add(
                    new Book(cursor.getString( cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_ISBN)),
                            cursor.getString(cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_NAME)),
                            cursor.getFloat(cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_WIDTH)),
                            cursor.getInt(cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_CHECKED_IN)),
                            cursor.getInt(cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_ROW)),
                            cursor.getFloat(cursor.getColumnIndex(BookDatabaseContract.BookEntry.COL_NAME_POSITION)),
                            picture,
                            imageFetched));
        }
    }

    // private functions
    ///@brief update database with new book info
    /// This is designed to be called onPause and onDestroy
    private boolean pUpdateDbBook( Book book )
    {
        boolean rc = true;
        byte[] image = null; // byte array to hold image
        // grab info from book
        ContentValues values = new ContentValues();

        // convert image to byte string if image is fetched
        if ( book.isImageFetched()) {
            image = pGetBytes(book.getCoverImage());
        }

        values.put(BookDatabaseContract.BookEntry.COL_NAME_ISBN,book.ISBN);
        values.put(BookDatabaseContract.BookEntry.COL_NAME_NAME,book.name);
        values.put(BookDatabaseContract.BookEntry.COL_NAME_WIDTH,book.width);
        values.put(BookDatabaseContract.BookEntry.COL_NAME_CHECKED_IN,book.checkedIn);
        values.put(BookDatabaseContract.BookEntry.COL_NAME_ROW,book.row);
        values.put(BookDatabaseContract.BookEntry.COL_NAME_POSITION,book.position);
        values.put(BookDatabaseContract.BookEntry.COL_NAME_COVER_IMAGE, image);
        values.put(BookDatabaseContract.BookEntry.COL_NAME_IMAGE_FETCHED, book.isImageFetched());

        SQLiteDatabase mDb = mBookDatabase.getWritableDatabase();
        // update row with new info
        int numRowsAffected = 0;
        numRowsAffected = mDb.update(BookDatabaseContract.BookEntry.TABLE_NAME,values,
                BookDatabaseContract.BookEntry.COL_NAME_ISBN + "=?",new String[] {book.ISBN});

        // if no rows were affected, that means we need to add a new book!
        long newBookId = 0;
        if (numRowsAffected == 0)
        {
            Log.d(TAG, "New book added to database: " + book.ISBN);
            newBookId =
                    mDb.insert(BookDatabaseContract.BookEntry.TABLE_NAME,
                            null, values );
        }

        // if return code is -1, throw error
        if ( -1 == newBookId )
        {
            Log.e(TAG,"Unable to add book " + book.ISBN + " to database!");
            rc = false;
        }
        mDb.close();
        return rc;
    }

    private boolean pDeleteBook( Book book )
    {
        boolean rc = true;

        SQLiteDatabase mDb = mBookDatabase.getWritableDatabase();
        // try to delete the book
        int numRowsAffected = 0;
        try{
            numRowsAffected = mDb.delete(BookDatabaseContract.BookEntry.TABLE_NAME,
                    BookDatabaseContract.BookEntry.COL_NAME_ISBN + " = ?",new String[]{ book.ISBN });
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        // if we didn't delete a row, log an error
        if ( numRowsAffected == 0)
        {
            Log.e(TAG,"Unable to delete book " + book.ISBN + " in database!");
            rc = false;
        }
        mDb.close();
        return rc;
    }

    // convert from bitmap to byte array
    private byte[] pGetBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    // convert from byte array to bitmap
    private Bitmap pGetImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

}
