package com.jonfouk.bookshelf;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.jonfouk.bookshelf.Server.RpiInterface;

/**
 * Created by joncf on 1/1/2018.
 */

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals("row_num")){
            Log.i("PREF","row_num changed");
            createRows();
        }
        changeIP();
    }

    @Override
    public void onStop() {
        RpiInterface.getRpiInterface().initShelf(this.getActivity());
        super.onStop();
    }

    private void createRows()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        int rowNum = Integer.parseInt(sharedPref.getString("row_num","0"));
        if ( rowNum != 0)
        {
            PreferenceScreen screen = this.getPreferenceScreen();
            PreferenceCategory prefShelf = (PreferenceCategory)screen.findPreference("pref_row_dim");
            // first remove all preferences from this category
            prefShelf.removeAll();
            for ( int i = 0; i < rowNum; i++)
            {
                // width
                EditTextPreference width = new EditTextPreference(this.getActivity());
                width.setTitle("Row Width " + i);
                width.setKey("pref_width"+i);
                width.setSummary("Width of row "+i);
                EditText widthtext = width.getEditText();
                widthtext.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                prefShelf.addPreference(width);

                EditTextPreference height = new EditTextPreference(this.getActivity());
                height.setTitle("Row Height " + i);
                height.setKey("pref_height"+i);
                height.setSummary("Height of row "+i);
                EditText heighttext = height.getEditText();
                heighttext.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                prefShelf.addPreference(height);
            }
        }
    }

    private void changeIP()
    {
        EditTextPreference ipPref = (EditTextPreference)this.getPreferenceScreen().findPreference("ip_addr");
        ipPref.setSummary(ipPref.getText());
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        createRows();
        changeIP();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(Color.WHITE);
//        view.setPadding(0,(int)getResources().getDimension(R.dimen.activity_vertical_margin),0,0);

        return view;
    }
}
