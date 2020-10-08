package services;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.annotation.NonNull;
import helpers.FirebaseHelper;

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
        super.onMessageReceived(remoteMessage);
    }

    private void sendTokenToDB(String token, String username) {
        FirebaseHelper db = new FirebaseHelper(username);
        db.setRegistrationToken(token);
    }
}
