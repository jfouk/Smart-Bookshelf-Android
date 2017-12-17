package com.jonfouk.bookshelf.BookshelfMgr;

import android.graphics.Bitmap;

/**
 * Created by joncf on 2/4/2017.
 */

public class Book {
    public String getISBN() {
        return ISBN;
    }

    public String getName() {
        return name;
    }

    public float getWidth() {
        return width;
    }

    public int getCheckedIn() {
        return checkedIn;
    }

    public int getRow() {
        return row;
    }

    public float getPosition() {
        return position;
    }

    public Bitmap getCoverImage() {
        return coverImage;
    }

    public boolean isImageFetched() {
        return imageFetched;
    }

    public void setImageFetched(boolean imageFetched) {
        this.imageFetched = imageFetched;
    }

    public void setCoverImage(Bitmap coverImage) {
        this.coverImage = coverImage;
    }
    /// book info
    String ISBN;       ///< ISBN of the book
    String name;    ///< name of the book
    float width;    ///< width of the book

    /// bookshelf info
    int checkedIn;  ///< if the book is checked in or not
    int row;            ///< which row the book is on
    float position;     ///< position along the row that the book is on

    // book cover, not saved in physical bookshelf
    Bitmap coverImage;  ///< book cover, only set this on add book
    boolean imageFetched;   ///< is the book image fetched?


    ///  so not to slow things down
    /// @brief constructor
    public Book(String ISBN, String name, float width,
                int checkedIn, int row, float position,
                Bitmap coverImage, boolean imageFetched) {
        this.ISBN = ISBN;
        this.name = name;
        this.width = width;
        this.checkedIn = checkedIn;
        this.row = row;
        this.position = position;
        this.coverImage = coverImage;
        this.imageFetched = imageFetched;
    }

    @Override
    public boolean equals(Object o) {
        if (getClass() != o.getClass())
            return false;

        // set object equal to this class
        Book other = (Book)o;
        return ( other.ISBN.equals(this.ISBN) &&
        other.name.equals(this.name) && other.width == this.width);
    }

    // copy otherBook into this book, if image is already fetched for this book
    // will not copy over image
    public void copy(Book otherBook)
    {
        this.ISBN = otherBook.getISBN();
        this.name = otherBook.getName();
        this.width = otherBook.getWidth();
        this.checkedIn = otherBook.getCheckedIn();
        this.row = otherBook.getRow();
        this.position = otherBook.getPosition();

        // only copy over image if image is not already fetched
        if ( !this.imageFetched )
        {
            this.coverImage = otherBook.getCoverImage();
            this.imageFetched = otherBook.isImageFetched();
        }
        return;
    }
}
