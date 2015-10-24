package net.floating_systems.tvtest;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Rating;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoContentProvider extends ContentProvider {

    MatrixCursor mc;
    HashMap<String,String> moviePosterCache = new HashMap<String,String>();


    public VideoContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {


        SQLiteDatabase db = getDatabase();



        System.out.println(uri);

        Cursor cursor = null;

        if(uri.toString().contains("/search")) {

            cursor = db.rawQuery("SELECT "
                    + "idMovie AS " + BaseColumns._ID
                    + ",idMovie AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
                    + ", c00 AS " + SearchManager.SUGGEST_COLUMN_TEXT_1
                    + ", substr(c08, instr(c08, '<thumb>')+7, instr(c08, '</thumb>')-8) as " + SearchManager.SUGGEST_COLUMN_ICON_1
                    + ", c07 AS " + SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR
                    + ", c05*10 AS " + SearchManager.SUGGEST_COLUMN_RATING_SCORE
                    + ", " + Constants.KODI_COLUMN_IMDB_ID
                    + ", '" + Rating.RATING_PERCENTAGE + "' AS " + SearchManager.SUGGEST_COLUMN_RATING_STYLE
                    + ", strPath AS " + Constants.COLUMN_BASE_PATH
                    + ", strFilename AS " + Constants.COLUMN_FILENAME
                    + " FROM movie "
                    + "JOIN files ON movie.idFile = files.idFile "
                    + "JOIN path ON files.idPath = path.idPath "
                    + "WHERE c00 LIKE ? OR c16 LIKE ? LIMIT 10;",
                    new String[]{"%" + selectionArgs[0] + "%","%" + selectionArgs[0] + "%"}
            );

            String[] menuCols = new String[] {
                    BaseColumns._ID,
                    SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                    SearchManager.SUGGEST_COLUMN_TEXT_1,
                    SearchManager.SUGGEST_COLUMN_ICON_1,
                    SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
                    //SearchManager.SUGGEST_COLUMN_DURATION,
                    SearchManager.SUGGEST_COLUMN_RATING_SCORE,
                    SearchManager.SUGGEST_COLUMN_RATING_STYLE,
                    //SearchManager.SUGGEST_COLUMN_CONTENT_TYPE,
                    Constants.COLUMN_FULL_PATH
            };

            menuCols = new String[] {
                    BaseColumns._ID,
                    SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                    SearchManager.SUGGEST_COLUMN_TEXT_1,
                    SearchManager.SUGGEST_COLUMN_ICON_1,
                    Constants.COLUMN_FULL_PATH
            };


            mc = new MatrixCursor(menuCols);

            while(cursor.moveToNext()) {
                String movieId = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
                String movieText = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
                String moviePoster = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1));
//                String moviePath = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_PATH));
                String movieIMDB = cursor.getString(cursor.getColumnIndex(Constants.KODI_COLUMN_IMDB_ID));


                String movieBasePath = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_BASE_PATH));
                String movieFileName = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_FILENAME));

                String moviePath = getFullPath(movieBasePath, movieFileName);


                if(moviePoster.trim().equals("")) {
                    moviePoster = getPoster(movieIMDB);
                }


                mc.addRow(new Object[]{movieId, movieId, movieText, moviePoster, moviePath});

            }

            insertThumbURLs(moviePosterCache);

            cursor.close();
            cursor = mc;

        }else if(uri.toString().contains("/id")){
            String movieId = uri.getLastPathSegment().toString();

            cursor = db.rawQuery("SELECT "
                    + ", strPath AS " + Constants.COLUMN_BASE_PATH
                    + ", strFilename AS " + Constants.COLUMN_FILENAME
                    + " FROM movie " +
                    "JOIN files ON movie.idFile = files.idFile " +
                    "JOIN path ON files.idPath = path.idPath " +
                    "WHERE idMovie = ?;", new String[]{movieId});


            String[] menuCols = new String[] {
                    Constants.COLUMN_FULL_PATH
            };

            mc = new MatrixCursor(menuCols);

            while(cursor.moveToNext()) {
                mc.addRow(new Object[]{getFullPath(cursor.getString(cursor.getColumnIndex(Constants.COLUMN_BASE_PATH)), cursor.getString(cursor.getColumnIndex(Constants.COLUMN_FILENAME)))});;
            }

            cursor.close();
            cursor = mc;

        }else if(uri.toString().contains("/recommendations")){


            Date fetchInfoMinDate = new Date(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000));
            Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


/*
            Cursor cursorFetchInfos = db.rawQuery(
                    "SELECT "
                            + "idMovie AS " + BaseColumns._ID
                            + ", c00 AS " + Constants.COLUMN_TITLE
                            + ", substr(c08, instr(c08, '<thumb>')+7, instr(c08, '</thumb>')-8) as " + Constants.COLUMN_IMAGE
                            + ", (strPath || strFilename) AS " + Constants.COLUMN_PATH
                            + ", (timeInSeconds / totalTimeInSeconds) * 100 AS "+ Constants.COLUMN_VIEW_PROGRESS
                            + ", " + Constants.KODI_COLUMN_IMDB_ID
                            + ", "+R.string.recommendation_resume+" AS reason "
                            + ", 3 AS importance "
                            + "FROM movie "
                            + "JOIN files ON movie.idFile = files.idFile "
                            + "JOIN path ON files.idPath = path.idPath "
                            + "LEFT JOIN bookmark ON files.idFile = bookmark.idFile "
                            //+ "WHERE files.playCount = ? "
                            + "WHERE bookmark.type = 1 "
                            + "AND timeInSeconds / totalTimeInSeconds > 0.05 "
                            + "AND timeInSeconds / totalTimeInSeconds < 0.9 "
                            + "AND lastPlayed > ?"
                            + "ORDER BY c05 DESC "
                            + "LIMIT 2;"
                    , new String[] {formatter.format(fetchInfoMinDate)});
*/




            Date continueMinDate = new Date(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000));

            Cursor cursorStartedWatching = db.rawQuery(
                    "SELECT "
                            + "idMovie AS " + BaseColumns._ID
                            + ", c00 AS " + Constants.COLUMN_TITLE
                            + ", substr(c08, instr(c08, '<thumb>')+7, instr(c08, '</thumb>')-8) as " + Constants.COLUMN_IMAGE
                            + ", strPath AS " + Constants.COLUMN_BASE_PATH
                            + ", strFilename AS " + Constants.COLUMN_FILENAME
                            + ", (timeInSeconds / totalTimeInSeconds) * 100 AS "+ Constants.COLUMN_VIEW_PROGRESS
                            + ", " + Constants.KODI_COLUMN_IMDB_ID
                            + ", "+R.string.recommendation_resume+" AS reason "
                            + ", 3 AS importance "
                            + "FROM movie "
                            + "JOIN files ON movie.idFile = files.idFile "
                            + "JOIN path ON files.idPath = path.idPath "
                            + "LEFT JOIN bookmark ON files.idFile = bookmark.idFile "
                            + "WHERE bookmark.type = 1 "
                            + "AND timeInSeconds / totalTimeInSeconds > 0.05 "
                            + "AND timeInSeconds / totalTimeInSeconds < 0.9 "
                            + "AND lastPlayed > ?"
                            + "ORDER BY c05 DESC "
                            + "LIMIT 2;"
                    , new String[] {formatter.format(continueMinDate)});


            Cursor cursorNewContent = db.rawQuery(
                    "SELECT "
                            + "idMovie AS " + BaseColumns._ID
                            + ", c00 AS " + Constants.COLUMN_TITLE
                            + ", substr(c08, instr(c08, '<thumb>')+7, instr(c08, '</thumb>')-8) as " + Constants.COLUMN_IMAGE
                            + ", strPath AS " + Constants.COLUMN_BASE_PATH
                            + ", strFilename AS " + Constants.COLUMN_FILENAME
                            + ", -1 AS "+ Constants.COLUMN_VIEW_PROGRESS
                            + ", " + Constants.KODI_COLUMN_IMDB_ID
                            + ", "+R.string.recommendation_new+" AS reason "
                            + ", 3 AS importance "
                            + "FROM movie "
                            + "JOIN files ON movie.idFile = files.idFile "
                            + "JOIN path ON files.idPath = path.idPath "
                            + "WHERE files.playCount IS NULL "
                            + "ORDER BY CAST(strftime('YYYYMM', files.dateAdded) AS INTEGER) DESC, c05 DESC "
                            + "LIMIT 2;"
                    , null);


/*
            Cursor cursorSameActor = db.rawQuery(
                    "SELECT "
                            + "idMovie AS " + BaseColumns._ID
                            + ", c00 AS " + Constants.COLUMN_TITLE
                            + ", substr(c08, instr(c08, '<thumb>')+7, instr(c08, '</thumb>')-8) as " + Constants.COLUMN_IMAGE
                            + ", strPath AS " + Constants.COLUMN_BASE_PATH
                            + ", strFilename AS " + Constants.COLUMN_FILENAME
                            + ", -1 AS "+ Constants.COLUMN_VIEW_PROGRESS
                            + ", " + Constants.KODI_COLUMN_IMDB_ID
                            + ", "+R.string.recommendation_actor+" AS reason "
                            + ", 1 AS importance "
                            + "FROM movie "
                            + "JOIN files ON movie.idFile = files.idFile "
                            + "JOIN path ON files.idPath = path.idPath "
                            //+ "LEFT JOIN bookmark ON files.idFile = bookmark.idFile "
                            //+ "WHERE files.playCount = ? "
                            //+ "WHERE bookmark.type = '1' "
                            + "ORDER BY c05 DESC "
                            + "LIMIT 2;"
                    , null);




            Cursor cursorSameDirector = db.rawQuery(
                            "SELECT "
                            + "idMovie AS " + BaseColumns._ID
                            + ", c00 AS " + Constants.COLUMN_TITLE
                            + ", substr(c08, instr(c08, '<thumb>')+7, instr(c08, '</thumb>')-8) as " + Constants.COLUMN_IMAGE
                            + ", strPath AS " + Constants.COLUMN_BASE_PATH
                            + ", strFilename AS " + Constants.COLUMN_FILENAME
                            + ", -1 AS "+ Constants.COLUMN_VIEW_PROGRESS
                            + ", " + Constants.KODI_COLUMN_IMDB_ID
                            + ", "+R.string.recommendation_director+" AS reason "
                            + ", 2 AS importance "
                            + "FROM movie "
                            + "JOIN files ON movie.idFile = files.idFile "
                            + "JOIN path ON files.idPath = path.idPath "
                            //+ "LEFT JOIN bookmark ON files.idFile = bookmark.idFile "
                            //+ "WHERE files.playCount = ? "
                            //+ "WHERE bookmark.type = '1' "
                            + "ORDER BY c05 DESC "
                            + "LIMIT 300,2;"
                    , null);
*/

            Cursor cursorHighRating = db.rawQuery(
                    "SELECT "
                            + "idMovie AS " + BaseColumns._ID
                            + ", c00 AS " + Constants.COLUMN_TITLE
                            + ", substr(c08, instr(c08, '<thumb>')+7, instr(c08, '</thumb>')-8) as " + Constants.COLUMN_IMAGE
                            + ", strPath AS " + Constants.COLUMN_BASE_PATH
                            + ", strFilename AS " + Constants.COLUMN_FILENAME
                            + ", -1 AS "+ Constants.COLUMN_VIEW_PROGRESS
                            + ", " + Constants.KODI_COLUMN_IMDB_ID
                            + ", "+R.string.recommendation_rating+" AS reason "
                            + ", 1 AS importance "
                            + "FROM movie "
                            + "JOIN files ON movie.idFile = files.idFile "
                            + "JOIN path ON files.idPath = path.idPath "
                            + "WHERE files.playCount IS NULL "
                            + "ORDER BY c05 DESC "
                            + "LIMIT 5;"
                    , null);

            cursor = new MergeCursor(new Cursor[]{cursorStartedWatching, cursorNewContent, /*cursorSameActor, cursorSameDirector,*/ cursorHighRating});


            String[] menuCols = new String[] {
                    BaseColumns._ID,
                    Constants.COLUMN_TITLE,
                    Constants.COLUMN_IMAGE,
                    Constants.COLUMN_FULL_PATH,
                    Constants.COLUMN_VIEW_PROGRESS,
                    Constants.COLUMN_RECOMMENDATION_REASON,
                    "importance"
            };

            mc = new MatrixCursor(menuCols);


            ArrayList<String> addedToList = new ArrayList<String>();
            ArrayList<Object[]> resultList = new ArrayList<Object[]>();

            while(cursor.moveToNext()) {
                String movieId = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
                String movieText = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_TITLE));
                String moviePoster = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_IMAGE));
                String movieBasePath = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_BASE_PATH));
                String movieFileName = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_FILENAME));

                String movieIMDB = cursor.getString(cursor.getColumnIndex(Constants.KODI_COLUMN_IMDB_ID));
                int movieReason = cursor.getInt(cursor.getColumnIndex("reason"));
                int movieImportance = cursor.getInt(cursor.getColumnIndex("importance"));
                int movieProgress = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_VIEW_PROGRESS));

                String moviePath = getFullPath(movieBasePath,movieFileName);

                if(addedToList.contains(movieId)){
                    continue;
                }

                if(moviePoster.trim().equals("")) {
                    moviePoster = getPoster(movieIMDB);
                }

                resultList.add(new Object[]{movieId, movieText, moviePoster, moviePath, movieProgress, movieReason, movieImportance + ((int)Math.random()*6)});

                addedToList.add(movieId);
            }

            //TODO: Order resultList by importance

            Collections.sort(resultList,
                    new Comparator<Object[]>() {
                        @Override
                        public int compare(Object[] a, Object[] b) {
                            return ((Integer) a[6]) > ((Integer) b[6]) ? -1 : ((Integer) a[6]) == ((Integer) b[6]) ? 0 : 1;
                        }
                    }
            );



            for(Object[] o:resultList) {
                mc.addRow(o);
            }

            cursor.close();
            cursor = mc;




        }

        System.out.println("Finished Request: " + cursor.getCount());
        db.close();


        insertThumbURLs(moviePosterCache);

        return cursor;
    }


    private String getPoster(String movieTT){

        if(moviePosterCache.containsKey(movieTT)){
            return moviePosterCache.get(movieTT);
        }

        String moviePoster = "";

        if(movieTT.startsWith("tt")){
            try {
                URL url = new URL("http://www.omdbapi.com/?i=" + movieTT + "&plot=short&r=json");
                String result = readInputStreamToString((HttpURLConnection) url.openConnection());
                JSONObject jObject = new JSONObject(result);
                moviePoster = jObject.getString("Poster");
                moviePosterCache.put(movieTT, moviePoster);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return moviePoster;
    }

    private String getFullPath(String movieBasePath,String movieFileName){
        String fullPath = movieFileName;

        if(!movieBasePath.startsWith("plugin://")){
            fullPath = movieBasePath+fullPath;
        }

        return fullPath;
    }

    private void insertThumbURLs(final HashMap<String,String> moviePosterMap){

        //INFO: DB File access is read only because of android permissions, so this makes no sense. Using in memory cache instead

        /*

        if(moviePosterCache.size()>0) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    SQLiteDatabase db = getDatabase();

                    for (String key : moviePosterCache.keySet()) {
                        if (!key.startsWith("tt")) continue;

                        db.rawQuery("UPDATE movie SET c08 = ? WHERE c09 = ?", new String[]{"<thumb>" + moviePosterCache.get(key) + "</thumb>", key});
                    }

                    db.close();
                }
            }).start();
        }
        */
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private String readInputStreamToString(HttpURLConnection connection) {
        String result = null;
        StringBuffer sb = new StringBuffer();
        InputStream is = null;

        try {
            is = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        }
        catch (Exception e) {

            result = null;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {

                }
            }
        }

        return result;
    }

    private SQLiteDatabase getDatabase(){
        return getDatabase(true);
    }

    private SQLiteDatabase getDatabase(boolean readonly){
        File dbDir = new File(Environment.getExternalStorageDirectory() + "/Android/data/org.xbmc.kodi/files/.kodi/userdata/Database/");

        final String patternString = "MyVideos([0-9]+)\\.db";
        final Pattern p = Pattern.compile("MyVideos([0-9]+)\\.db");


        File[] files = dbDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().toString().matches(patternString);
            }
        });

        ArrayList<File> filesList = new ArrayList<File>(Arrays.asList(files));

        Collections.sort(filesList,
                new Comparator<File>() {
                    @Override
                    public int compare(File a, File b) {

                        Matcher mA = p.matcher(a.getName());
                        mA.find();
                        int aVersion = Integer.parseInt(mA.group(1));

                        Matcher mB = p.matcher(b.getName());
                        mB.find();
                        int bVersion = Integer.parseInt(mB.group(1));

                        Log.d(Constants.APP_NAME, aVersion + " vs " + bVersion);

                        return aVersion > bVersion ? -1 : aVersion == bVersion ? 0 : 1;
                    }
                }
        );
        String dbPath = filesList.get(0).toString();
        return SQLiteDatabase.openDatabase(dbPath, null, readonly ? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.OPEN_READWRITE);


    }
}
