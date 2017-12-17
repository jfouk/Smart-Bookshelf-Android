package com.jonfouk.bookshelf;

import android.support.v4.widget.SwipeRefreshLayout;

import com.jonfouk.bookshelf.BookshelfMgr.BookRVAdapter;

/**
 * Created by joncf on 4/13/2017.
 */
/// Utility class for doing all the IO stuff we need to do constantly such as updating RV
public class MainUtility {
    private BookRVAdapter mBookRVAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public MainUtility(BookRVAdapter mBookRVAdapter, SwipeRefreshLayout mSwipeRefreshLayout) {
        this.mBookRVAdapter = mBookRVAdapter;
        this.mSwipeRefreshLayout = mSwipeRefreshLayout;
    }

    /// @brief update MainActivityView objects
    public void updateMainView( )
    {
        mSwipeRefreshLayout.setRefreshing(false);
        mBookRVAdapter.notifyDataSetChanged();
    }
}
