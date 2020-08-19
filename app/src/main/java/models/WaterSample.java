package models;

public class WaterSample {
    //public String key; // the key given by push() in firebase
    public String pH;
    public String orp;
    public String turbidity;

    public WaterSample() {
        // Default constructor required for calls to DataSnapshot.getValue(WaterSample.class)
    }

    public WaterSample(/*String key, */String ph, String orp, String turbidity) {
        //this.key = key;
        this.pH = ph;
        this.orp = orp;
        this.turbidity = turbidity;
    }

}
