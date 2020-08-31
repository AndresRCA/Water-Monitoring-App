package helpers;

import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import models.WaterSample;

public class FirebaseHelper {
    private FirebaseDatabase db;
    private DatabaseReference waterSamples;
    private Query monthlyWaterSamples; // previous month water samples
    public ArrayList<WaterSample> water_set; // array that is displayed on the activity in a graph

    public FirebaseHelper(String username) {
        db = FirebaseDatabase.getInstance();

        // set waterSamples
        DatabaseReference user = db.getReference("/users/" + username);
        waterSamples = user.child("waterSamples"); // get the waterSamples reference for this user, this property is used for monthlyWaterSamples, and is not really used directly

        /*--------- this code here could be situated outside the constructor -------*/
        // set monthlyWaterSamples
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        long last_month = calendar.getTimeInMillis(); // for now it's a value defined in the constructor, later it could be assigned in a function to establish the time frame to be observed in the reference
        monthlyWaterSamples = waterSamples.orderByChild("created_at").startAt(last_month);

        setEventListeners(); // listen to changes in the waterSamples ref and update the water_set in the app
        /*-------------------------------------------------------------------------*/
    }

    /**
     * set initial ArrayList of water samples.
     * @param callback
     */
    public void setInitialWaterSet(final WaterSetCallback callback) {
        if (water_set != null) {
            // if water_set was already retrieved just send it immediately to the callback as a response
            Log.i("water/setInitialWaterSet", "water_set already defined");
            callback.onSuccess(water_set);
            return;
        }

        water_set = new ArrayList<WaterSample>();
        monthlyWaterSamples.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren() && dataSnapshot.exists()) { // if /user/$user/waterSamples has children (samples)
                    Log.i("water/setInitialWaterSet", "retrieving " + dataSnapshot.getChildrenCount() + " samples");
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        WaterSample water_sample = child.getValue(WaterSample.class);
                        water_sample.setKey(child.getKey());
                        water_set.add(water_sample);
                    }
                    callback.onSuccess(water_set);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting samples failed, log a message
                Log.w("water/onCancelled", "loadSamples:onCancelled", databaseError.toException());
                callback.onFailure();
            }
        });
    }

    /**
     * set event listeners for the water_set collected from firebase
     */
    private void setEventListeners() {
        monthlyWaterSamples.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d("onChildAdded", dataSnapshot.getKey());
                // A new water sample has been added
                WaterSample sample = dataSnapshot.getValue(WaterSample.class);
                sample.setKey(dataSnapshot.getKey());
                if (water_set != null) {
                    water_set.add(sample);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d("onChildRemoved", dataSnapshot.getKey());
                // Do something like get the id (key) and compare it to something in water_set and remove it, or just remove the first element since the first element will always be the one getting removed according to cloud functions
                if (water_set != null) {
                    water_set.remove(0);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /**
     * interface for callbacks in setInitialWaterSet()
     */
    public interface WaterSetCallback {
        void onSuccess(ArrayList<WaterSample> water_set);
        void onFailure();
    }

}
