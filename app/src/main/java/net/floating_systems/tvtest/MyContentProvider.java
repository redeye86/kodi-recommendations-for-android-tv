package net.floating_systems.tvtest;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class MyContentProvider extends ContentProvider {
    MatrixCursor mc;
    public MyContentProvider() {



    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Log.d(Constants.APP_NAME, "CP Queried");

        String[] menuCols = new String[] {  BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_ICON_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_CONTENT_TYPE,
                SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
                SearchManager.SUGGEST_COLUMN_DURATION,
                "video_url",
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

        mc = new MatrixCursor(menuCols);
        mc.addRow(new Object[]{1,"How much is the fish?","https://upload.wikimedia.org/wikipedia/en/a/a4/How_much_is_the_fish.jpg", "Scooter", "audio","2002","3:57","https://www.youtube.com/watch?v=BRb78VMFMHQ",1});
        mc.addRow(new Object[]{2,"Nessaja","https://upload.wikimedia.org/wikipedia/en/b/b1/Nessaja.jpg", "Scooter", "audio","2003","3:21","https://www.youtube.com/watch?v=u9odvl3tfEU",2});
        mc.addRow(new Object[]{3, "Fire", "http://eil.com/images/main/Scooter-Fire-509935.jpg", "Scooter", "audio", "1998", "3:54", "https://youtu.be/tn9Q_wiJZYM", 3});


        return mc;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
