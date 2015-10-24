package net.floating_systems.tvtest;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class UpdateRecommendationsService extends IntentService {
    private static final int MAX_RECOMMENDATIONS = 8;

    private NotificationManager mNotificationManager;

    public UpdateRecommendationsService() {
        super("RecommendationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        Log.d(Constants.APP_NAME, "Recommender Service Intent");

        Cursor recommendationsCursor = getContentResolver().query(Uri.parse("content://net.floating_systems.tvtest.VideoContentProvider/recommendations"), null, null, null, null);



        RecommendationBuilder builder = new RecommendationBuilder()
                .setContext(getApplicationContext())
                .setSmallIcon(R.drawable.videos_icon);

        int count = 0;

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
        }

        int i =0;
        while(recommendationsCursor.moveToNext()){
            i++;
            String movieTitle = recommendationsCursor.getString(recommendationsCursor.getColumnIndex(Constants.COLUMN_TITLE));
            String movieImage = recommendationsCursor.getString(recommendationsCursor.getColumnIndex(Constants.COLUMN_IMAGE));
            String moviePath = recommendationsCursor.getString(recommendationsCursor.getColumnIndex(Constants.COLUMN_PATH));

            int movieReason = recommendationsCursor.getInt(recommendationsCursor.getColumnIndex(Constants.COLUMN_RECOMMENDATION_REASON));


            Double viewProgress = recommendationsCursor.getDouble(recommendationsCursor.getColumnIndex(Constants.COLUMN_VIEW_PROGRESS));

            //Log.d(Constants.APP_NAME,"isnull: " + (viewProgress == null));


            Notification notification = builder.setBackground(movieImage)
                    .setId(count + 1)
                    .setPriority(MAX_RECOMMENDATIONS - count)
                    .setTitle(movieTitle)
                    .setDescription(getResources().getString(movieReason))
                    .setBitmap(getBitmapFromURL(movieImage))
                    .setIntent(buildPendingIntent(moviePath))
                    .setProgress((int) (viewProgress * 1f))
                    .build();

            mNotificationManager.notify(i,notification);

            if (++count >= MAX_RECOMMENDATIONS) {
                break;
            }
        }

        recommendationsCursor.close();

        /*


        try {
            RecommendationBuilder builder = new RecommendationBuilder()
                    .setContext(getApplicationContext())
                    .setSmallIcon(R.drawable.videos_by_google_icon);

            for (Map.Entry<String, List<Movie>> entry : recommendations.entrySet()) {
                for (Movie movie : entry.getValue()) {

                    builder.setBackground(movie.getCardImageUrl())
                            .setId(count + 1)
                            .setPriority(MAX_RECOMMENDATIONS - count)
                            .setTitle(movie.getTitle())
                            .setDescription(getString(R.string.popular_header))
                            .setImage(movie.getCardImageUrl())
                            .setIntent(buildPendingIntent(movie))
                            .build();

                    if (++count >= MAX_RECOMMENDATIONS) {
                        break;
                    }
                }
                if (++count >= MAX_RECOMMENDATIONS) {
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to update recommendation", e);
        }*/



    }

    private PendingIntent buildPendingIntent(String moviePath) {
        /*Intent detailsIntent = new Intent(this, DetailsActivity.class);
        detailsIntent.putExtra("Movie", movie);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(DetailsActivity.class);
        stackBuilder.addNextIntent(detailsIntent);

        detailsIntent.setAction(Long.toString(movie.getId()));*/


        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(moviePath));
        Uri videoUri = Uri.parse(moviePath);
        intent.setDataAndType(videoUri, "video/*");
        intent.setPackage("org.xbmc.kodi");


        PendingIntent pendingIntent = PendingIntent.getActivity(this,(int) System.currentTimeMillis(),intent,0);




        //PendingIntent intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }
}