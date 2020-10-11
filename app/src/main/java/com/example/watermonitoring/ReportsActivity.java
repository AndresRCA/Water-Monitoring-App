package com.example.watermonitoring;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import models.Report;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ReportsActivity extends AppCompatActivity {

    private DatePickerDialog.OnDateSetListener mFromDateSetListener;
    private DatePickerDialog.OnDateSetListener mToDateSetListener;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Button mFromDateBtn;
    private Button mToDateBtn;
    private long from_date = 0;
    private long to_date = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        mFromDateBtn = findViewById(R.id.from_date_btn);
        mToDateBtn = findViewById(R.id.to_date_btn);
        mRecyclerView = findViewById(R.id.reports_recycler_view);

        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Date picker callback when a date is added
        mFromDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month++; // months starts with 0 (Jan = 0)
                String dateString = dayOfMonth + "/" + month + "/" + year;
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                try {
                    Date date = formatter.parse(dateString);
                    from_date = date.getTime();
                    mFromDateBtn.setText(dateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };
        // Date picker callback when a date is added
        mToDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month++; // months starts with 0 (Jan = 0)
                String dateString = dayOfMonth + "/" + month + "/" + year;
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                try {
                    Date date = formatter.parse(dateString);
                    to_date = date.getTime();
                    mToDateBtn.setText(dateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * Open 'From' date dialog
     * @param view
     */
    public void setFromDate(View view) {
        Calendar cal = Calendar.getInstance(); // instead of this, show current year, month and day from sql
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, mFromDateSetListener, year, month, day);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getDatePicker().setMaxDate(cal.getTimeInMillis());
        dialog.show();
    }

    /**
     * Open 'To' date dialog
     * @param view
     */
    public void setToDate(View view) {
        Calendar cal = Calendar.getInstance(); // instead of this, show current year, month and day from sql
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, mToDateSetListener, year, month, day);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getDatePicker().setMaxDate(cal.getTimeInMillis());
        dialog.show();
    }

    /**
     * get the reports from the alarm database
     * @param view
     */
    public void getReports(View view) {
        if (from_date == 0) {
            Toast.makeText(this, "'From' date can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if(to_date == 0) {
            Toast.makeText(this, "'To' date can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // ready list of reports
        final ArrayList<Report> userReports = new ArrayList<>();

        SharedPreferences userPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
        String user = userPrefs.getString("username", null);
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        long inclusive_toDate = to_date + 86399900;
        String url ="https://water-alarm-manager.herokuapp.com/api/" + user + "/" + from_date + "-" + inclusive_toDate;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray reports = jsonObject.getJSONArray("reports");
                    Log.d("water/onResponse", "reports: " + reports.toString());
                    if (reports.length() == 0) {
                        Toast.makeText(getApplicationContext(), "there is no data in that time interval", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (int i = 0; i < reports.length(); i++) {
                        JSONObject row = reports.getJSONObject(i);

                        int id = row.getInt("AlarmId");
                        String parameter = row.getString("parameter");
                        String str_value = row.getString("value");
                        Number value;
                        try {
                            value = Integer.parseInt(str_value);
                        } catch (NumberFormatException e) {
                            //not int
                            value = Double.parseDouble(str_value);
                        }
                        long created_at = row.getLong("created_at");

                        Report report = new Report(id, parameter, value, created_at);
                        userReports.add(report);
                    }

                    mAdapter = new ReportsAdapter(userReports);
                    mRecyclerView.setAdapter(mAdapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "error retrieving data", Toast.LENGTH_SHORT).show();
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }
}
