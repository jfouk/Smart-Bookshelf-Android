package com.jonfouk.bookshelf;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by joncf on 1/1/2018.
 */

public class PreferenceActivity extends ActionBarActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pref_layout_with_bar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // inflate settings fragment
        SettingsFragment settingsFrag = new SettingsFragment();

        getFragmentManager().beginTransaction()
                .replace(R.id.pref_content_frame, settingsFrag)
//                .addToBackStack(null)
                .commit();
    }
}

