package com.example.watermonitoring;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.anychart.AnyChartView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;

import helpers.FirebaseHelper;
import helpers.MQTTHelper;
import helpers.WaterChartHelper;
import models.WaterSample;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqtt;

    FirebaseHelper db; // db contains data such as the water samples collected by the user
	String username;

	ArrayList<WaterSample> water_set;
	WaterChartHelper chart;
    AnyChartView anyChartView;

    // water quality parameters
    TextView mpH, mOrp, mTurbidity;
    String pH, orp, turbidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
			username = savedInstanceState.getString("username");
            pH = savedInstanceState.getString("pH");
            orp = savedInstanceState.getString("orp");
            turbidity = savedInstanceState.getString("pH");
            water_set = savedInstanceState.getParcelableArrayList("water_set");
        }

        setContentView(R.layout.activity_main);

        mpH = findViewById(R.id.pH);
        mOrp = findViewById(R.id.orp);
        mTurbidity = findViewById(R.id.turbidity);
        anyChartView = findViewById(R.id.any_chart_view);

        if (pH == null || orp == null || turbidity == null) { // usually this data comes in a single object, so if pH is null then so is everyone else, but I evaluate the three variables for readability
            /* IMPORTANT: the first string in all of these is always the same, I should just make to TextViews and modify the second one where the value is shown */
            mpH.setText(getString(R.string.connecting_to_server));
            mOrp.setText(getString(R.string.connecting_to_server));
            mTurbidity.setText(getString(R.string.connecting_to_server));
        }
        else {
            mpH.setText(pH);
            mOrp.setText(orp);
            mTurbidity.setText(turbidity);
        }

		// get username from intent
		if (username == null) {
			String extra_username = getIntent().getExtras().getString("username");
			if (extra_username != null) {
				username = extra_username;
			}
			else {
				// we have a problem if this ever occurs, this shouldn't happen
			}
		}
		
		initMqtt(); // start connection and receive data
        initFirebase();
        initChart();
    }

    /**
     * create mqtt connection and set callbacks for receiving data
     */
    private void initMqtt() {
        mqtt = new MQTTHelper(getApplicationContext(), username);
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.i("connection", "connection complete!");
                mpH.setText(getString(R.string.waiting_for_data));
                mOrp.setText(getString(R.string.waiting_for_data));
                mTurbidity.setText(getString(R.string.waiting_for_data));
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.i("connection", "connection lost");
                mpH.setText(getString(R.string.connection_lost));
                mOrp.setText(getString(R.string.connection_lost));
                mTurbidity.setText(getString(R.string.connection_lost));
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // get json object
                JSONObject jsonmsg = new JSONObject(new String(message.getPayload()));
                Log.i("mqtt", "message arrived" + new String(message.getPayload()));
                pH = jsonmsg.getString("pH");
                orp = jsonmsg.getString("orp");
                turbidity = jsonmsg.getString("turbidity");

                mpH.setText(pH);
                mOrp.setText(orp);
                mTurbidity.setText(turbidity);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    private void initFirebase() {
        db = new FirebaseHelper(username); // connect to db with this user
    }

    private void initChart() {
        anyChartView.setProgressBar(findViewById(R.id.progress_bar));  // display progress bar while retrieving the data to be used in the chart
        if (water_set == null) { // retrieve the data from the db to display it in the chart
            Log.i("water/initChart", "series_data is null, calling db.setInitialWaterSet");
            db.setInitialWaterSet(new FirebaseHelper.WaterSetCallback() {
                @Override
                public void onSuccess(ArrayList<WaterSample> waterSet) {
                    water_set = getDailySamplesAvg(waterSet); // get the daily averages for the time interval specified in setInitialWaterSet()
                    chart = new WaterChartHelper(getApplicationContext(), water_set); // create a chart
                    //chart.insertSeriesData(series_data); // insert values to show in the chart
                    anyChartView.setChart(chart.getCartesian()); // display chart*/
                }

                @Override
                public void onFailure() {
                }
            });
        }
        else { // display the existing chart
            Log.i("water/initChart", "series_data is not null");
            chart = new WaterChartHelper(getApplicationContext(), water_set); // create a chart
            //chart.insertSeriesData(series_data); // insert values to show in the chart, series_data might turn into ArrayList<WaterSample> water_set if I can't find a way to save the series_data list
            anyChartView.setChart(chart.getCartesian());
        }
    }

    /**
     * get the daily averages for the data set provided by FirebaseHelper.setInitialWaterSet()
     * @param waterSet
     * @return
     */
    private ArrayList<WaterSample> getDailySamplesAvg(@NotNull ArrayList<WaterSample> waterSet) {
        ArrayList<WaterSample> water_set = new ArrayList<>(); // data for the chart
        String last_date = waterSet.get(0).getStrDate("dd/MM");
        long last_created_at = waterSet.get(0).created_at;
        String current_date;
        double avg_pH = 0;
        double avg_orp = 0;
        double avg_turbidity = 0;
        int counter = 1;
        int i = 0;

        for (WaterSample sample : waterSet) { // get trimmed version of waterSet
            current_date = sample.getStrDate("dd/MM");
            if (!current_date.equals(last_date) || i == waterSet.size() - 1) {
                // if the date changed or if you reached the last element, insert the average of that day to the list
                avg_pH = avg_pH/counter;
                avg_orp = avg_orp/counter;
                avg_turbidity = avg_turbidity/counter;
                //series_data.add(new CustomDataEntry(last_date, avg_pH, avg_orp, avg_turbidity));
                WaterSample chart_sample = new WaterSample(last_created_at, avg_pH, avg_orp, avg_turbidity); // a note about this, last_created_at is only important for getting the correct "dd/MM" value when creating the chart
                chart_sample.setKey(sample.key);
                // add a setDayMonth(last_date) maybe, for processing purposes
                water_set.add(chart_sample); // this created_at is the next day one, try to save the last day one

                // reset variables for next item in the chart (water_set)
                avg_pH = 0;
                avg_orp = 0;
                avg_turbidity = 0;
                counter = 0;
            }
            // keep summing the averages
            avg_pH += sample.pH;
            avg_orp += sample.orp;
            avg_turbidity += sample.turbidity;
            counter++;
            i++;
            last_date = current_date;
            last_created_at = sample.created_at;
        }
        return water_set;
    }

    /**
     * save values
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i("water/onSaveInstanceState", "saving state...");
        super.onSaveInstanceState(outState);
		outState.putString("pH", username);
        outState.putString("pH", pH);
        outState.putString("orp", orp);
        outState.putString("turbidity", turbidity);
        outState.putParcelableArrayList("water_set", water_set);
    }
}
