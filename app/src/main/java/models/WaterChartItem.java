package models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * this is essentially the object that gets processed to be inserted in the chart
 */
public class WaterChartItem implements Parcelable {
    public WaterSample sample;
    public int n_of_samples;

    public WaterChartItem(WaterSample sample, int n_of_samples) {
        this.sample = sample;
        this.n_of_samples = n_of_samples;
    }

    protected WaterChartItem(Parcel in) {
        sample = in.readParcelable(WaterSample.class.getClassLoader());
        n_of_samples = in.readInt();
    }

    public static final Creator<WaterChartItem> CREATOR = new Creator<WaterChartItem>() {
        @Override
        public WaterChartItem createFromParcel(Parcel in) {
            return new WaterChartItem(in);
        }

        @Override
        public WaterChartItem[] newArray(int size) {
            return new WaterChartItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(sample, flags);
        dest.writeInt(n_of_samples);
    }
}

