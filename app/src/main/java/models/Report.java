package models;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Report implements Parcelable {
    public int id;
    public String parameter;
    public Number value;
    public long created_at;

    public Report() {

    }

    public Report(int id, String parameter, Number value, long created_at) {
        this.id = id;
        this.parameter = parameter;
        this.value = value;
        this.created_at = created_at;
    }

    protected Report(Parcel in) {
        id = in.readInt();
        parameter = in.readString();
        if(value.getClass() == Double.class) {
            value = in.readDouble();
        }
        if(value.getClass() == Integer.class) {
            value = in.readInt();
        }
        created_at = in.readLong();
    }

    public static final Creator<Report> CREATOR = new Creator<Report>() {
        @Override
        public Report createFromParcel(Parcel in) {
            return new Report(in);
        }

        @Override
        public Report[] newArray(int size) {
            return new Report[size];
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
        dest.writeInt(id);
        dest.writeString(parameter);
        if(value.getClass() == Double.class) {
            dest.writeDouble((Double) value);
        }
        if(value.getClass() == Integer.class) {
            dest.writeInt((Integer) value);
        }
        dest.writeLong(created_at);
    }
}
