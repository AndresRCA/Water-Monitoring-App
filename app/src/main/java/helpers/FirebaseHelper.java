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

        // set monthlyWaterSamples
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        long last_month = calendar.getTimeInMillis();
        monthlyWaterSamples = waterSamples.orderByChild("created_at").startAt(last_month);

        setInitialWaterSet(); // initialize water_set
        setEventListeners(); // listen to changes in the waterSamples ref and update the water_set in the app
    }

    // retrieve initial array of water samples
    private void setInitialWaterSet() {
        water_set = new ArrayList<WaterSample>();
        monthlyWaterSamples.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren() && dataSnapshot.exists()) { // if /user/$user/waterSamples has children (samples)
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Log.i("onDataChange", dataSnapshot.toString());
                        WaterSample water_sample = child.getValue(WaterSample.class);
                        water_sample.setKey(child.getKey());
                        water_set.add(water_sample);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting samples failed, log a message
                Log.w("onCancelled", "loadSamples:onCancelled", databaseError.toException());
            }
        });
    }

    // set event listeners for the water_set used in the graph
    private void setEventListeners() {
        monthlyWaterSamples.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d("onChildAdded", dataSnapshot.getKey());
                // A new water sample has been added
                WaterSample sample = dataSnapshot.getValue(WaterSample.class);
                sample.setKey(dataSnapshot.getKey());
                water_set.add(sample);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d("onChildRemoved", dataSnapshot.getKey());
                // Do something like get the id (key) and compare it to something in water_set and remove it, or just remove the first element since the first element will always be the one getting removed according to cloud functions
                water_set.remove(0);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

}
