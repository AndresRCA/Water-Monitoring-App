package helpers;

import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import models.WaterSample;

public class FirebaseHelper {
    private FirebaseDatabase db;
    private DatabaseReference waterSamples;
    public ArrayList<WaterSample> water_set;

    public FirebaseHelper(String username/*, String password*/) { // password is used only in validateUser(), delete it when moving it to standalone function
        db = FirebaseDatabase.getInstance();
        //validateUser(username, password); /* this will probably be a standalone function in the login page, no need to add it to the helper class */
        /*DatabaseReference user = db.getReference("/users").orderByChild("username").equalTo(username).getRef(); // get reference to the user with a given username value
        waterSamples = user.child("waterSamples"); // get the waterSamples reference for this user
        setInitialWaterSet(); // initialize water_set
        setEventListeners(); // listen to changes in the waterSamples ref and update the water_set in the app
        */
    }

    /* this will probably be a standalone function in the login page, no need to add it to the helper class */
	/*private void validateUser(String user, String password) {
		// ...
	}*/

    // retrieve initial array of water samples
    private void setInitialWaterSet() {
        water_set = new ArrayList<WaterSample>();
        waterSamples.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i("onDataChange", dataSnapshot.toString());
                if (dataSnapshot.hasChildren()) { // if /user/$user/waterSamples has children (samples)
                    /* try to limit the number of children beforehand, in ref */
                    // check the benefit of adding .iterator() to .getChildren(), since there could be unneeded data (only need a month's time of data):
                    // benefit: If you are using for loop you cannot update(add/remove) the Collection whereas with the help of an iterator you can easily update Collection
                    // this benefit is probably useless in this case since I'm creating a new array, not reusing the getChildren() iterable
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        WaterSample water_sample = child.getValue(WaterSample.class);
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
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d("onChildAdded", "onChildAdded:" + dataSnapshot.getKey());
                // A new water sample has been added
                WaterSample sample = dataSnapshot.getValue(WaterSample.class);
                water_set.add(sample);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d("onChildRemoved", "onChildRemoved:" + dataSnapshot.getKey());
                // Do something like get the id (key) and compare it to something in water_set and remove it, or just remove the first element since the first element will always be the one getting removed according to cloud functions
                water_set.remove(0);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        waterSamples.addChildEventListener(childEventListener);
    }

}
