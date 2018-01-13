package com.jonfouk.bookshelf.BookshelfMgr;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.jonfouk.bookshelf.BookDetail;
import com.jonfouk.bookshelf.MainActivity;
import com.jonfouk.bookshelf.R;

import java.util.List;

/**
 * Created by joncf on 2/4/2017.
 */

public class BookRVAdapter extends RecyclerView.Adapter<BookRVAdapter.BookViewHolder>{
    public class BookViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        CardView cv;
        TextView BookName;
        TextView Author;

        BookViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cv);
            BookName = (TextView) itemView.findViewById(R.id.book_name);
            Author = (TextView)itemView.findViewById(R.id.author);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mContext, BookDetail.class);
            Toast.makeText(mContext,"Clicked book isbn = " +
                    books.get(getAdapterPosition()).getISBN(),Toast.LENGTH_SHORT);
            intent.putExtra(MainActivity.EXTRA_ISBN,
                    books.get(getAdapterPosition()).getISBN());
            mContext.startActivity(intent);
        }
    }
    List<Book> books;
    protected Context mContext;
    public static final String TAG="RVAdapter";


    public BookRVAdapter(List<Book> books, Context context)
    {
        this.books = books;
        this.mContext = context;
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    @Override
    public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_row, parent, false);
        BookViewHolder bookViewHolder = new BookViewHolder(v);
        return bookViewHolder;
    }

    @Override
    public void onBindViewHolder(BookViewHolder holder, int position) {
        holder.BookName.setText(books.get(position).name);
        holder.Author.setText(books.get(position).author);

    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    private void grabBookImage( final String ISBN )
    {
        // build url from openlibrary
        String url = "http://covers.openlibrary.org/b/isbn/" + ISBN + "-L.jpg";
        Log.i(BookShelf.TAG,"Grabbing book image from " + url);
        Glide.with(mContext).load(url).asBitmap().error(R.mipmap.ic_launcher).into(new SimpleTarget<Bitmap>(){
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                BookShelf.getBookShelf().setPicture(ISBN,resource);
            }
        });
    }
}
