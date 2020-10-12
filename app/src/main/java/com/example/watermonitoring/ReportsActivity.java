package com.example.watermonitoring;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import models.Report;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ReportsActivity extends AppCompatActivity {

    private DatePickerDialog.OnDateSetListener mFromDateSetListener;
    private DatePickerDialog.OnDateSetListener mToDateSetListener;

    private RecyclerView mRecyclerView;
    ArrayList<Report> userReports;
    private JSONObject jsonReports;
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

        if (savedInstanceState != null) {
            from_date = savedInstanceState.getLong("from_date");
            to_date = savedInstanceState.getLong("to_date");
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            if (from_date != 0) {
                String str_fromDate = formatter.format(from_date);
                mFromDateBtn.setText(str_fromDate);
            }
            if (to_date != 0) {
                String str_toDate = formatter.format(to_date);
                mToDateBtn.setText(str_toDate);
            }

            userReports = savedInstanceState.getParcelableArrayList("userReports");
        }
        else {
            userReports = new ArrayList<>();
        }

        mRecyclerView = findViewById(R.id.reports_recycler_view);

        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new ReportsAdapter(userReports);
        mRecyclerView.setAdapter(mAdapter);

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
        userReports.clear();
        mAdapter.notifyDataSetChanged();

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
                    jsonReports = new JSONObject(response);
                    JSONArray reports = jsonReports.getJSONArray("reports");
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
                    mAdapter.notifyDataSetChanged();
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

    public void exportData(View v) {
        if (userReports.size() == 0) {
            Toast.makeText(this, "there is no data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FileOutputStream out = openFileOutput("report.json", Context.MODE_PRIVATE);
            out.write(jsonReports.toString().getBytes());
            out.close();

            Context context = getApplicationContext();
            File fileLocation = new File(getFilesDir(), "report.json");
            Uri path = FileProvider.getUriForFile(context, "com.example.watermonitoring.fileprovider", fileLocation);
            Intent fileIntent =new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/json");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Report");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Send mail"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("from_date", from_date);
        outState.putLong("to_date", to_date);
        outState.putParcelableArrayList("userReports", userReports);
    }
}
