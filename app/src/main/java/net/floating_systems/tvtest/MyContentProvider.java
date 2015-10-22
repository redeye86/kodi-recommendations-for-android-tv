package net.floating_systems.tvtest;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Rating;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

public class MyContentProvider extends ContentProvider {

    private SQLiteDatabase db;

    MatrixCursor mc;
    public MyContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {

        //find MyVideosXXX.db with highest number
        //in /sdcard/Android/data/org.xbmc.kodi/files/.kodi/userdata/Database


        String dbPath = Environment.getExternalStorageDirectory() + "/Android/data/org.xbmc.kodi/files/.kodi/userdata/Database/MyVideos93.db";
        db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);


        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {


        System.out.println(uri);



        Cursor cursor = null;

        if(uri.toString().contains("/search")) {

            if (selectionArgs[0].length() < 3) {
                return null;
            }

            cursor = db.rawQuery("SELECT " +
                    "idMovie AS " + BaseColumns._ID +
                    ",idMovie AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID +
                    ", c00 AS " + SearchManager.SUGGEST_COLUMN_TEXT_1
                    + ", substr(c08, instr(c08, '<thumb>')+7, instr(c08, '</thumb>')-8) as " + SearchManager.SUGGEST_COLUMN_ICON_1
                    + ", c07 AS " + SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR
                    + ", c05*10 AS " + SearchManager.SUGGEST_COLUMN_RATING_SCORE
                    + ", '" + Rating.RATING_PERCENTAGE + "' AS " + SearchManager.SUGGEST_COLUMN_RATING_STYLE
                    + ", (strPath || strFilename) AS " + Constants.COLUMN_PATH
                    + " FROM movie " +
                    "JOIN files ON movie.idFile = files.idFile " +
                    "JOIN path ON files.idPath = path.idPath " +
                    "WHERE c00 LIKE ? LIMIT 10;", new String[]{"%" + selectionArgs[0] + "%"});


            /*
                    while(cursor.moveToNext()){
                        for(int i = 0; i < cursor.getColumnCount(); i++) {
                            Log.d(Constants.APP_NAME, i + "\t"+cursor.getColumnName(i)+ " \t:"+cursor.getString(i));
                        }
                    }
                    cursor.moveToFirst();
            */

        }else if(uri.toString().contains("/id")){
            String movieId = uri.getLastPathSegment().toString();


            cursor = db.rawQuery("SELECT "
                    + "(strPath || strFilename) AS " + Constants.COLUMN_PATH
                    + " FROM movie " +
                    "JOIN files ON movie.idFile = files.idFile " +
                    "JOIN path ON files.idPath = path.idPath " +
                    "WHERE idMovie = ?;", new String[]{movieId});
        }

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
