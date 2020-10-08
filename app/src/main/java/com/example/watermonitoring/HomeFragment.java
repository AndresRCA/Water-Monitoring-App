package com.example.watermonitoring;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.anychart.APIlib;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import helpers.FirebaseHelper;
import helpers.MQTTHelper;
import helpers.WaterChartHelper;
import models.WaterChartItem;
import models.WaterSample;
import services.MQTTService;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {

    IBinder serviceBinder;
    MQTTService mqttService;
    // callback used when receiving messages from mqtt server
    MqttCallbackExtended mqttCallback = new MqttCallbackExtended() {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Log.i("water/connection", "connection complete!");
            mpH.setText(getString(R.string.waiting_for_data));
            mOrp.setText(getString(R.string.waiting_for_data));
            mTurbidity.setText(getString(R.string.waiting_for_data));
            mTemperature.setText(getString(R.string.waiting_for_data));
            mChlorine.setText("pH: " + getString(R.string.waiting_for_data) + ", ORP: " + getString(R.string.waiting_for_data));
        }

        @Override
        public void connectionLost(Throwable throwable) {
            Log.i("water/connection", "connection lost");
            mpH.setText(getString(R.string.connection_lost));
            mOrp.setText(getString(R.string.connection_lost));
            mTurbidity.setText(getString(R.string.connection_lost));
            mTemperature.setText(getString(R.string.connection_lost));
            mChlorine.setText("pH: " + getString(R.string.connection_lost) + ", ORP: " + getString(R.string.connection_lost));
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            // get json object
            JSONObject jsonmsg = new JSONObject(new String(message.getPayload()));
            Log.i("water/mqtt", "message arrived" + new String(message.getPayload()));
            if(mqttService.sample == null) {
                mqttService.sample = new WaterSample();
            }

            mqttService.sample.pH = jsonmsg.getDouble("pH");
            mqttService.sample.orp = jsonmsg.getInt("orp");
            mqttService.sample.turbidity = jsonmsg.getDouble("turbidity");
            mqttService.sample.temperature = jsonmsg.getDouble("temperature");

            // get String values for the TextViews
            pH = String.valueOf(mqttService.sample.pH);
            orp = String.valueOf(mqttService.sample.orp);
            turbidity = String.valueOf(mqttService.sample.turbidity);
            temperature = String.valueOf(mqttService.sample.temperature);

            // set the text for the TextViews
            mpH.setText(pH + " pH");
            mOrp.setText(orp + " mV");
            mTurbidity.setText(turbidity + " NTU");
            mTemperature.setText(temperature + " °C");
            mChlorine.setText("pH: " + pH + " pH, ORP: " + orp + " mV");

            // check sample parameters here for notifications
            // ...
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {}
    };
    // callbacks for the service connection on bind
    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("water/mServerconn", "connected to service");
            isServiceBound = true;
            serviceBinder = service;
            MQTTService.MyBinder binder = (MQTTService.MyBinder) service;
            mqttService = binder.getService();

            // when binding, connect to mqtt server if the service wasn't started beforehand
            if (!mqttService.isServiceStarted) {
                mqttService.connectToMqttServer(new MQTTHelper.MQTTCallback() {
                    @Override
                    public void onSuccess() {
                        if (pH == null || orp == null || turbidity == null || temperature == null) { // meaning if there hasn't been any data before this reconnection (in the case it's not the first time a connection has been established)
                            mpH.setText("connected! waiting for data...");
                            mOrp.setText("connected! waiting for data...");
                            mTurbidity.setText("connected! waiting for data...");
                            mTemperature.setText("connected! waiting for data...");
                            mChlorine.setText("pH: connected! waiting for data..., ORP: connected! waiting for data...");
                        }
                        else {
                            mpH.setText(pH + " pH");
                            mOrp.setText(orp + " mV");
                            mTurbidity.setText(turbidity + " NTU");
                            mTemperature.setText(temperature + " °C");
                            mChlorine.setText("pH: " + pH + " pH, ORP: " + orp + " mV");
                        }
                    }

                    @Override
                    public void onFailure() {
                        mpH.setText("failed to connect to server"); // or should I say connection lost? i just subtituted the onFailure from the mqtt callback to here
                        mOrp.setText("failed to connect to server");
                        mTurbidity.setText("failed to connect to server");
                        mTemperature.setText("failed to connect to server");
                        mChlorine.setText("pH: failed to connect to server, ORP: failed to connect to server");
                    }
                }, username);
            }

            // when binding, set callbacks for mqtt server incoming data
            mqttService.setMqttCallback(mqttCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };
    boolean isServiceBound; // watch out for this, might need to declare it or save it

    FirebaseHelper db; // db contains data such as the water samples collected by the user
    String username;

    ArrayList<WaterChartItem> chart_water_set; // the water set to be processed in WaterChartHelper, distinct from water_set in FirebaseHelper (unprocessed data)
    WaterChartHelper chartHelper;
    TextView mNoData;
    AnyChartView phChartView, orpChartView, turbidityChartView, temperatureChartView;
    ImageButton phBtn, orpBtn, turbidityBtn, temperatureBtn;
    // selected chart status
    int selected_chart = PH;
    public static final int PH = 1;
    public static final int ORP = 2;
    public static final int TURBIDITY = 3;
    public static final int TEMPERATURE = 4;

    // water quality parameters
    TextView mpH, mOrp, mTurbidity, mTemperature, mChlorine;
    String pH, orp, turbidity, temperature;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // mqtt values TextView
        mpH = rootView.findViewById(R.id.pH);
        mOrp = rootView.findViewById(R.id.orp);
        mTurbidity = rootView.findViewById(R.id.turbidity);
        mTemperature = rootView.findViewById(R.id.temperature);
        mChlorine = rootView.findViewById(R.id.chlorine);

        // charts
        mNoData = rootView.findViewById(R.id.no_data_view);
        phChartView = rootView.findViewById(R.id.ph_chart_view);
        orpChartView = rootView.findViewById(R.id.orp_chart_view);
        turbidityChartView = rootView.findViewById(R.id.turbidity_chart_view);
        temperatureChartView = rootView.findViewById(R.id.temperature_chart_view);

        // chart buttons
        phBtn = rootView.findViewById(R.id.ph_btn);
        orpBtn = rootView.findViewById(R.id.orp_btn);
        turbidityBtn = rootView.findViewById(R.id.turbidity_btn);
        temperatureBtn = rootView.findViewById(R.id.temperature_btn);

        phBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showpHChart();
            }
        });
        orpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showORPChart();
            }
        });
        turbidityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTurbidityChart();
            }
        });
        temperatureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTemperatureChart();
            }
        });

        SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences("user_data", MODE_PRIVATE);
        username = sharedPreferences.getString("username", null); // get username from shared preferences
        initFirebase();

        if (savedInstanceState != null) {
            serviceBinder = savedInstanceState.getBinder("serviceBinder");
            MQTTService.MyBinder binder = (MQTTService.MyBinder) serviceBinder;
            mqttService = binder.getService(); // get service from binder

            pH = savedInstanceState.getString("pH");
            orp = savedInstanceState.getString("orp");
            turbidity = savedInstanceState.getString("pH");
            temperature = savedInstanceState.getString("temperature");
            chart_water_set = savedInstanceState.getParcelableArrayList("chart_water_set");
            selected_chart = savedInstanceState.getInt("selected_chart");
        }
        else {
            // essentially initialize everything the first time the activity starts (since there is an onSaveInstanceState method declared)
            // pH, orp, turbidity, and temperature should be null as well, and that means that the service is not bound or data has not been received yet (since the connection hasn't been established yet)
            mpH.setText(getString(R.string.connecting_to_server));
            mOrp.setText(getString(R.string.connecting_to_server));
            mTurbidity.setText(getString(R.string.connecting_to_server));
            mTemperature.setText(getString(R.string.connecting_to_server));
            mChlorine.setText("pH: " + getString(R.string.connecting_to_server) + ", ORP: " + getString(R.string.connecting_to_server));

            // set token when user logs in
            FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        Log.w("water/initFirebase", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.i("water/initFirebase", "registration token: " + token);
                    db.setRegistrationToken(token);
                }
            });
        }

        initMqttService();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initChart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            getActivity().unbindService(mServiceConn);
        }
    }

    /**
     * create mqtt connection and set callbacks for receiving data
     */
    private void initMqttService() {
        /* should maybe check if serviceBinder is null? I mean, if I save the binder that means the service is still bound right? */
        if (isServiceBound) {
            return; // service is already bound and working its magic (I think? I mean, is the mServerConn callback still working when I'm using fragment properties inside? don't the references get destroyed or something?)
        }

        // prepare service intent
        Intent service_intent = new Intent(getActivity(), MQTTService.class);
        getActivity().bindService(service_intent, mServiceConn, Context.BIND_AUTO_CREATE); // Context.BIND_AUTO_CREATE: automatically create the service as long as the binding exists, does not call onStartCommand()
    }

    private void initFirebase() {
        db = new FirebaseHelper(username); // connect to db with this user
    }

    private void initChart() {
        chartHelper = new WaterChartHelper();
        if (chart_water_set == null) { // retrieve the data from the db to display it in the chart
            Log.i("water/initChart", "series_data is null, calling db.setInitialWaterSet");

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            long last_month = calendar.getTimeInMillis(); // get the last month date in milliseconds

            // set the water set to be retrieved from last month to onwards
            db.setInitialWaterSet(last_month, new FirebaseHelper.WaterSetCallback() {
                @Override
                public void onSuccess(ArrayList<WaterSample> waterSet) {
                    chart_water_set = getDailySamplesAvg(waterSet); // get the daily averages for the time interval specified in setInitialWaterSet()
                    loadpHChart(); // initial chart to show is pH, that's why I don't turn it invisible
                    loadORPChart();
                    loadTurbidityChart();
                    loadTemperatureChart();

                    orpChartView.setVisibility(View.INVISIBLE);
                    turbidityChartView.setVisibility(View.INVISIBLE);
                    temperatureChartView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onFailure() {
                    Log.i("water/initChart", "there is no data");
                    phChartView.setVisibility(View.INVISIBLE);
                    orpChartView.setVisibility(View.INVISIBLE);
                    turbidityChartView.setVisibility(View.INVISIBLE);
                    temperatureChartView.setVisibility(View.INVISIBLE);
                    mNoData.setVisibility(View.VISIBLE);
                }
            });
        }
        else {
            Log.i("water/initChart", "chart_water_set is not null");

            // load charts
            loadpHChart();
            loadORPChart();
            loadTurbidityChart();
            loadTemperatureChart();
            // set every chart to invisible
            phChartView.setVisibility(View.INVISIBLE);
            orpChartView.setVisibility(View.INVISIBLE);
            turbidityChartView.setVisibility(View.INVISIBLE);
            temperatureChartView.setVisibility(View.INVISIBLE);
            // make visible the current selected chart
            switch (selected_chart) {
                case PH:
                    phChartView.setVisibility(View.VISIBLE);
                    break;
                case ORP:
                    orpChartView.setVisibility(View.VISIBLE);
                    break;
                case TURBIDITY:
                    turbidityChartView.setVisibility(View.VISIBLE);
                    break;
                case TEMPERATURE:
                    temperatureChartView.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    /**
     * get the daily averages for the data set provided by FirebaseHelper.setInitialWaterSet()
     * @param waterSet
     * @return
     */
    private ArrayList<WaterChartItem> getDailySamplesAvg(@NotNull ArrayList<WaterSample> waterSet) {
        ArrayList<WaterChartItem> chart_water_set = new ArrayList<>(); // data for the chart

        Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.DATE, 1);
        String next_date = getStrDate(calendar.get(Calendar.DATE), calendar.get(Calendar.MONTH) + 1);

        calendar.add(Calendar.DATE, -1);
        calendar.add(Calendar.MONTH, -1);
        String previous_date = getStrDate(calendar.get(Calendar.DATE), calendar.get(Calendar.MONTH) + 1);

        while (!next_date.equals(previous_date)) {
            double avg_pH = 0;
            double avg_orp = 0;
            double avg_turbidity = 0;
            double avg_temperature = 0;
            int counter = 0;

            for (WaterSample sample : waterSet) {
                if (previous_date.equals(sample.getStrDate("dd/MM"))) {
                    avg_pH += sample.pH;
                    avg_orp += sample.orp;
                    avg_turbidity += sample.turbidity;
                    avg_temperature += sample.temperature;
                    counter++;
                }
            }

            if (counter != 0) {
                avg_pH = avg_pH/counter;
                avg_orp = avg_orp/counter;
                avg_turbidity = avg_turbidity/counter;
                avg_temperature = avg_temperature/counter;
            }
            WaterSample chart_sample = new WaterSample(calendar.getTimeInMillis(), avg_pH, (int) avg_orp, avg_turbidity, avg_temperature);
            WaterChartItem chart_item = new WaterChartItem(chart_sample, counter);
            chart_water_set.add(chart_item);

            calendar.add(Calendar.DATE, 1);
            previous_date = getStrDate(calendar.get(Calendar.DATE), calendar.get(Calendar.MONTH) + 1);
        }

        return chart_water_set;
    }

    private String getStrDate(int day, int month) {
        String next_day = String.valueOf(day);
        if (day < 10) {
            next_day = "0" + next_day;
        }
        String next_month = String.valueOf(month);
        if (month < 10) {
            next_month = "0" + next_month;
        }

        String next_date = next_day + "/" + next_month;
        return next_date;
    }

    public void loadpHChart() {
        APIlib.getInstance().setActiveAnyChartView(phChartView);
        phChartView.setProgressBar(this.getView().findViewById(R.id.ph_progress_bar)); // 'this' is null here when i rotate the phone

        // create data from main water_set
        List<DataEntry> series_data = new ArrayList<>();
        for (WaterChartItem item : chart_water_set) {
            series_data.add(new ValueDataEntry(item.sample.getStrDate("dd/MM"), item.sample.pH));
        }

        // create chart
        Cartesian ph_chart = chartHelper.createChart("pH Levels", "pH", "pH", "#74cc62", series_data);
        phChartView.setChart(ph_chart);
    }

    public void loadORPChart() {
        APIlib.getInstance().setActiveAnyChartView(orpChartView);
        orpChartView.setProgressBar(this.getView().findViewById(R.id.orp_progress_bar));

        // create data from main water_set
        List<DataEntry> series_data = new ArrayList<>();
        for (WaterChartItem item : chart_water_set) {
            series_data.add(new ValueDataEntry(item.sample.getStrDate("dd/MM"), item.sample.orp));
        }

        // create chart
        Cartesian orp_chart = chartHelper.createChart("ORP Levels", "ORP", "mV", "#c798bc", series_data);
        orpChartView.setChart(orp_chart);
    }

    public void loadTurbidityChart() {
        APIlib.getInstance().setActiveAnyChartView(turbidityChartView);
        turbidityChartView.setProgressBar(this.getView().findViewById(R.id.turbidity_progress_bar));

        // create data from main water_set
        List<DataEntry> series_data = new ArrayList<>();
        for (WaterChartItem item : chart_water_set) {
            series_data.add(new ValueDataEntry(item.sample.getStrDate("dd/MM"), item.sample.turbidity));
        }

        // create chart
        Cartesian turbidity_chart = chartHelper.createChart("Turbidity Levels", "Turbidity", "NTU", "#b1710f", series_data);
        turbidityChartView.setChart(turbidity_chart);
    }

    public void loadTemperatureChart() {
        APIlib.getInstance().setActiveAnyChartView(temperatureChartView);
        temperatureChartView.setProgressBar(this.getView().findViewById(R.id.temperature_progress_bar));

        // create data from main water_set
        List<DataEntry> series_data = new ArrayList<>();
        for (WaterChartItem item : chart_water_set) {
            series_data.add(new ValueDataEntry(item.sample.getStrDate("dd/MM"), item.sample.temperature));
        }

        // create chart
        Cartesian temperature_chart = chartHelper.createChart("Temperature Levels", "Temperature", "Â°C", "#cc4d29", series_data);
        temperatureChartView.setChart(temperature_chart);
    }

    public void showpHChart() {
        if (mNoData.getVisibility() == View.VISIBLE) {
            // if the no data message is display, disable the buttons
            return;
        }
        orpChartView.setVisibility(View.INVISIBLE);
        turbidityChartView.setVisibility(View.INVISIBLE);
        temperatureChartView.setVisibility(View.INVISIBLE);
        phChartView.setVisibility(View.VISIBLE);
        selected_chart = PH;
    }

    public void showORPChart() {
        if (mNoData.getVisibility() == View.VISIBLE) {
            // if the no data message is display, disable the buttons
            return;
        }
        phChartView.setVisibility(View.INVISIBLE);
        turbidityChartView.setVisibility(View.INVISIBLE);
        temperatureChartView.setVisibility(View.INVISIBLE);
        orpChartView.setVisibility(View.VISIBLE);
        selected_chart = ORP;
    }

    public void showTurbidityChart() {
        if (mNoData.getVisibility() == View.VISIBLE) {
            // if the no data message is display, disable the buttons
            return;
        }
        phChartView.setVisibility(View.INVISIBLE);
        orpChartView.setVisibility(View.INVISIBLE);
        temperatureChartView.setVisibility(View.INVISIBLE);
        turbidityChartView.setVisibility(View.VISIBLE);
        selected_chart = TURBIDITY;
    }

    public void showTemperatureChart() {
        if (mNoData.getVisibility() == View.VISIBLE) {
            // if the no data message is display, disable the buttons
            return;
        }
        phChartView.setVisibility(View.INVISIBLE);
        orpChartView.setVisibility(View.INVISIBLE);
        turbidityChartView.setVisibility(View.INVISIBLE);
        temperatureChartView.setVisibility(View.VISIBLE);
        selected_chart = TEMPERATURE;
    }

    /**
     * save values
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i("water/onSaveInstanceState", "saving state...");
        super.onSaveInstanceState(outState);
        outState.putBinder("serviceBinder", serviceBinder);
        outState.putString("pH", pH);
        outState.putString("orp", orp);
        outState.putString("turbidity", turbidity);
        outState.putString("temperature", temperature);
        outState.putParcelableArrayList("chart_water_set", chart_water_set);
        outState.putInt("selected_chart", selected_chart);
    }
}
