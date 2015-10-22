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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.net.URI;

public class SearchableActivity extends Activity{


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.result_layout);



        handleIntent(getIntent());
    }

    public void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        Log.d(Constants.APP_NAME, "NEW INTENT: " + intent.getAction());

        String movieId = intent.getData().getLastPathSegment();
        Cursor c = getContentResolver().query(Uri.parse("content://net.floating_systems.tvtest.MyContentProvider/id/" + movieId), null, null, null, null);

        if(c.getCount() > 0) {
            c.moveToFirst();
            String moviePath = c.getString(c.getColumnIndex(Constants.COLUMN_PATH));

            try {
                Intent intent2 = new Intent(Intent.ACTION_VIEW, Uri.parse(moviePath));
                Uri videoUri = Uri.parse(moviePath);
                intent2.setDataAndType(videoUri, "video/*");
                intent2.setPackage("org.xbmc.kodi");
                startActivity(intent2);
            } catch (Exception e) {
                Log.e(Constants.APP_NAME, "ERROR EXECUTING KODI");
                e.printStackTrace();
            }

            finishActivity(Activity.RESULT_OK);
        }

    }
}