package models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Report {
    public int id;
    public String parameter;
    public Number value;
    public long created_at;

    public Report(int id, String parameter, Number value, long created_at) {
        this.id = id;
        this.parameter = parameter;
        this.value = value;
        this.created_at = created_at;
    }

    public String getStrDate(String format) {
        DateFormat formatter = new SimpleDateFormat(format);
        String str_date = formatter.format(created_at);
        return str_date;
    }
}
