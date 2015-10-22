package net.floating_systems.tvtest;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.TextView;

import java.net.URI;

public class SearchableActivity extends Activity {

    TextView tv;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.result_layout);
        tv = (TextView) findViewById(R.id.textView);

        handleIntent(getIntent());
    }

    public void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        Log.d(Constants.APP_NAME, "NEW INTENT: " + intent.getAction());

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query =
                    intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
        }else  /*if (Intent.ACTION_VIEW.equals(intent.getAction()))*/ {



            String songId = intent.getData().getLastPathSegment();

            Log.d(Constants.APP_NAME,"Play Song #"+songId);

            tv.setText("Play Song #"+songId);
        }
    }

    private void doSearch(String queryStr) {
        // get a Cursor, prepare the ListAdapter
        // and set it

        Log.d(Constants.APP_NAME,"Query:" + queryStr);

    }

}