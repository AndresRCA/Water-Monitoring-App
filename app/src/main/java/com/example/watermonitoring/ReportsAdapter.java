package com.example.watermonitoring;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import models.Report;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {

    private ArrayList<Report> mReportList;

    public static class ReportViewHolder extends RecyclerView.ViewHolder {

        public TextView mReportBody;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            mReportBody = itemView.findViewById(R.id.report_body);
        }
    }

    public ReportsAdapter(ArrayList<Report> reportList) {
        mReportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.report_item, parent, false);
        ReportViewHolder rvh = new ReportViewHolder(v);
        return rvh;
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report currentReport = mReportList.get(position);
        String unit = "";
        switch (currentReport.parameter) {
            case "pH":
                unit = "pH";
                break;
            case "orp":
                unit = "mV";
                break;
            case "turbidity":
                unit = "NTU";
                break;
            case "temperature":
                unit = "°C";
                break;
        }
        String body = currentReport.parameter + " has reached " + currentReport.value + " " + unit + " at " + currentReport.getStrDate("dd/MM/yyyy hh:mm a");
        holder.mReportBody.setText(body);
    }

    @Override
    public int getItemCount() {
        return mReportList.size();
    }
}
