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

/**
 * LittleFinger makes the HTTP call to the server and decides what to do next based on the
 * HTTP Response code and body
 * <p>
 * If the response code is `HTTP_PAYMENT_REQUIRED` (402) or `HTTP_OK` (200), then payment is
 * expected to receive and let the app work as usual
 * <p>
 * If the response code is `HTTP_ACCEPTED` (202), then payment has been received. Update the flag
 * in preferences so that no future network calls are made
 * <p>
 * If the response code is `HTTP_CONFLICT` (409), no payment has been received and there is a
 * conflict. Crash the app. If there is any notification data from server, use that and display a
 * notification before crashing
 * <p>
 * LittleFinger class is the one which is exposed to the developer. This class is responsible for
 * making HTTP calls to server and deciding what to do based on the response. Internally, it makes
 * use of CallbackHandler class which does all the heavy lifting
 */
public class LittleFinger {

    private static OkHttpClient client = new OkHttpClient();

    public static void start(Context ctx, String urlString) {
        // Check whether to make call to server or not
        if (!Status.shouldMakeCall(ctx)) {
            // looks like payment has been received. No calls to the server!
            Log.d("in lib", "no call to server");
            return;
        }
        try {
            makeHttpCall(ctx, urlString);
        } catch (IOException ex) {
            // In case of any exceptions, which might mostly be from HTTP connection, crash
            // the app
            throw null;
        }
    }

    private static void makeHttpCall(Context ctx, String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        CallbackHandler callback = new CallbackHandler(ctx);
        client.newCall(request).enqueue(callback);
    }
}

class CallbackHandler implements Callback {

    private Context mContext;

    CallbackHandler(Context ctx) {
        mContext = ctx;
    }

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

    private void responseHandler(Response response) {
        if (response.code() == HttpURLConnection.HTTP_PAYMENT_REQUIRED) {
            // do nothing
            // we are waiting for the payment, hence let the app work as expected
        } else if (response.code() == HttpURLConnection.HTTP_ACCEPTED) {
            // received the payment
            // disable HTTP calls
            Status.cancelCall(mContext);
        } else if (response.code() == HttpURLConnection.HTTP_CONFLICT) {
            // no payment received
            // time to crash the app
            goEvil(response);
        }
    }


    /**
     * This method crashes the app. If the server has sent Notificatoin data, it uses that to
     * display notification and then crash
     */
    private void goEvil(Response response) {
        try {
            String jsonData = response.body().string();
            JSONObject jobject = new JSONObject(jsonData);
            String title = jobject.getString("NotificationTitle");
            String text = jobject.getString("NotificationText");
            displayNotification(title, text);
        } catch (IOException | JSONException ex) {
            // Don't do anything
        }
        // crash the app
        throw null;
    }

    private void displayNotification(String title, String text) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_evil)
                        .setContentTitle(title)
                        .setContentText(text);

        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(001, mBuilder.build());
    }

}


/**
 * This is a helper class which makes use of Utils class. A thin wrapper.
 */
class Status {
    private static final String PREF_SHOULD_CALL = "should_call";

    /**
     * Checks if the the app should make HTTP call to server? If the value isn't set in Shared
     * Preferences, True so that HTTP call can be made
     */
    static boolean shouldMakeCall(Context ctx) {
        return Boolean.valueOf(Utils.readSharedSetting(ctx, PREF_SHOULD_CALL, "true"));
    }

    /**
     * Once the payment is received, it is no longer required to make calls to server. This
     * method updates the Shared Preferences
     */
    static void cancelCall(Context ctx) {
        Utils.saveSharedSetting(ctx, PREF_SHOULD_CALL, "false");
    }
}

/**
 * Utility class to deal with Shared App Preferences
 */
class Utils {

    private static final String PREFERENCES_FILE = "little_finger_settings";

    /**
     * Given a key and default value, returns the value from Shared Preferences.
     * If the key isn't set, returns the default value
     */
    static String readSharedSetting(Context ctx, String settingName, String defaultValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(settingName, defaultValue);
    }

    /**
     * Given a key and value, sets the value in the Shared Preferences
     */
    static void saveSharedSetting(Context ctx, String settingName, String settingValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }
}