package models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class WaterSample {
    public String key; // the key given by push() in firebase
    public String day_published; // the day it was created
    public long created_at;
    public double pH;
    public double orp;
    public double turbidity;

    public WaterSample() {
        // Default constructor required for calls to DataSnapshot.getValue(WaterSample.class)
    }

    public WaterSample(long created_at, double ph, double orp, double turbidity) {
        this.created_at = created_at;
        this.pH = ph;
        this.orp = orp;
        this.turbidity = turbidity;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getStrDate(String format) {
        DateFormat formatter = new SimpleDateFormat(format);
        String str_date = formatter.format(created_at);
        return str_date;
    }
}
