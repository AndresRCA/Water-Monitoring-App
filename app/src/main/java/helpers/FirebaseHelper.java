package helpers;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import models.WaterSample;

public class FirebaseHelper {
    private FirebaseDatabase db;
    private DatabaseReference waterSamples;
    private Query waterSamplesToWatch;
    public ArrayList<WaterSample> water_set; // array that is displayed on the activity in a graph
    private long start_date;

    public FirebaseHelper(String username) {
        db = FirebaseDatabase.getInstance();
        DatabaseReference user = db.getReference("/users/" + username);
        waterSamples = user.child("waterSamples"); // get the waterSamples reference for this user, this property is used for monthlyWaterSamples, and is not really used directly
        start_date = 0; // initial start_date for querying
    }

    /**
     * set initial ArrayList of water samples.
     * @param callback
     */
    public void setInitialWaterSet(long start_date, final WaterSetCallback callback) {
        if (this.start_date == 0) {
            this.start_date = start_date;
        }
        waterSamplesToWatch = waterSamples.orderByChild("created_at").startAt(start_date);

        if (water_set != null) {
            // send the already defined water_set, no need to call addListenerForSingleValueEvent for retrieval
            callback.onSuccess(water_set);
            return;
        }

        water_set = new ArrayList<WaterSample>();
        waterSamplesToWatch.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren() && dataSnapshot.exists()) { // if /user/$user/waterSamples has children (samples)
                    Log.i("water/setInitialWaterSet", "retrieving " + dataSnapshot.getChildrenCount() + " samples");
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        WaterSample water_sample = child.getValue(WaterSample.class);
                        water_set.add(water_sample);
                    }
                    callback.onSuccess(water_set);
                }
                else {
                    callback.onFailure(); // there is no data
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
     * interface for callbacks in setInitialWaterSet()
     */
    public interface WaterSetCallback {
        void onSuccess(ArrayList<WaterSample> water_set);
        void onFailure();
    }
}
