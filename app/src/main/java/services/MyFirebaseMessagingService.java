package services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.watermonitoring.LoginActivity;
import com.example.watermonitoring.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import helpers.FirebaseHelper;

import static com.example.watermonitoring.App.CHANNEL_ID;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        Log.i("water/onNewToken", "refreshed token: " + token);
        SharedPreferences userPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
        String username = userPrefs.getString("username", null);
        if (username != null) {
            Log.i("water/onNewToken", "sending token to database");
            // if user is logged in essentially (remember that when user logs out, userPrefs is cleared)
            sendTokenToDB(token, username);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("water/onMessageReceived", "received message from: " + remoteMessage.getFrom());
        SharedPreferences userPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
        String username = userPrefs.getString("username", null);
        if (username != null) {
            // if user is logged in essentially (remember that when user logs out, userPrefs is cleared)
            if (remoteMessage.getData().size() > 0) {
                String parameter = remoteMessage.getData().get("parameter");
                String value = remoteMessage.getData().get("value");
                long time = Long.parseLong(remoteMessage.getData().get("time"));
                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a");
                String str_date = formatter.format(time);
                String unit = "";
                switch (parameter) {
                    case "pH":
                        unit = "pH";
                        break;
                    case "orp":
                        unit = "mV";
                        break;
                    case "turbidity":
                        unit = "NTU";
                        break;
                    case "temperature":
                        unit = "°C";
                        break;
                }
                if (unit.isEmpty()) {
                    return; // there had to have been an error from the server sending the notification
                }

                Log.i("water/onMessageReceived", "remoteMessage with parameter " + parameter);

                // set up intent for notification
                Intent intent = new Intent(this, LoginActivity.class); // LoginActivity checks whether user is logged in (isLoggedIn in shared prefs)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // this is probably not needed, I don't check flags
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

                // build notification
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.water_drop)
                        .setContentTitle(parameter + " alert")
                        .setContentText(parameter + " has reached " + value + " " + unit + " at " + str_date)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(0, notificationBuilder.build());
            }
        }
    }

    private void sendTokenToDB(String token, String username) {
        FirebaseHelper db = new FirebaseHelper(username);
        db.setRegistrationToken(token);
    }
}
