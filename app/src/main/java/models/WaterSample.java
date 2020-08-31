package models;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class WaterSample implements Parcelable {
    public String key; // the key given by push() in firebase
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

    protected WaterSample(@org.jetbrains.annotations.NotNull Parcel in) {
        key = in.readString();
        created_at = in.readLong();
        pH = in.readDouble();
        orp = in.readDouble();
        turbidity = in.readDouble();
    }

    public static final Creator<WaterSample> CREATOR = new Creator<WaterSample>() {
        @NotNull //...
        @Contract("_ -> new") //...
        @Override
        public WaterSample createFromParcel(Parcel in) {
            return new WaterSample(in);
        }

        @NotNull //...
        @Contract(value = "_ -> new", pure = true) //...
        @Override
        public WaterSample[] newArray(int size) {
            return new WaterSample[size];
        }
    };

    public void setKey(String key) {
        this.key = key;
    }

    public String getStrDate(String format) {
        DateFormat formatter = new SimpleDateFormat(format);
        String str_date = formatter.format(created_at);
        return str_date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // this method might not be needed?
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeLong(created_at);
        dest.writeDouble(pH);
        dest.writeDouble(orp);
        dest.writeDouble(turbidity);
    }
}
