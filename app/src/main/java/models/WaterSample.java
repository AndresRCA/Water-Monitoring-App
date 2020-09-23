package models;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class WaterSample implements Parcelable {
    public long created_at;
    public double pH;
    public int orp;
    public double turbidity;
    public double temperature;

    public WaterSample() {
        // Default constructor required for calls to DataSnapshot.getValue(WaterSample.class)
    }

    public WaterSample(long created_at, double ph, int orp, double turbidity, double temperature) {
        this.created_at = created_at;
        this.pH = ph;
        this.orp = orp;
        this.turbidity = turbidity;
        this.temperature = temperature;
    }

    protected WaterSample(Parcel in) {
        created_at = in.readLong();
        pH = in.readDouble();
        orp = in.readInt();
        turbidity = in.readDouble();
        temperature = in.readDouble();
    }

    public static final Creator<WaterSample> CREATOR = new Creator<WaterSample>() {
        @Override
        public WaterSample createFromParcel(Parcel in) {
            return new WaterSample(in);
        }

        @Override
        public WaterSample[] newArray(int size) {
            return new WaterSample[size];
        }
    };

    public String getStrDate(String format) {
        DateFormat formatter = new SimpleDateFormat(format);
        String str_date = formatter.format(created_at);
        return str_date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(created_at);
        dest.writeDouble(pH);
        dest.writeInt(orp);
        dest.writeDouble(turbidity);
        dest.writeDouble(temperature);
    }

}
