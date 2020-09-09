package com.example.watermonitoring;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.anychart.AnyChartView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import helpers.FirebaseHelper;
import helpers.MQTTHelper;
import helpers.WaterChartHelper;
import models.WaterChartItem;
import models.WaterSample;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqtt;

    FirebaseHelper db; // db contains data such as the water samples collected by the user
	String username;

	ArrayList<WaterChartItem> chart_water_set; // the water set to be processed in WaterChartHelper, distinct from water_set in FirebaseHelper (unprocessed data)
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
            chart_water_set = savedInstanceState.getParcelableArrayList("chart_water_set");
        }

        setContentView(R.layout.activity_main);

        mpH = findViewById(R.id.pH);
        mOrp = findViewById(R.id.orp);
        mTurbidity = findViewById(R.id.turbidity);
        anyChartView = findViewById(R.id.any_chart_view);

        if (pH == null || orp == null || turbidity == null) { // usually this data comes in a single object, so if pH is null then so is everyone else, but I evaluate the three variables for readability
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
                    chart = new WaterChartHelper(getApplicationContext(), chart_water_set); // create a chart
                    anyChartView.setChart(chart.getCartesian()); // display chart
                }

                @Override
                public void onFailure() {
                }
            });
        }
        else {
            Log.i("water/initChart", "chart_water_set is not null");

            // display the existing chart
            chart = new WaterChartHelper(getApplicationContext(), chart_water_set); // create a chart
            anyChartView.setChart(chart.getCartesian());
        }
    }

    /**
     * get the daily averages for the data set provided by FirebaseHelper.setInitialWaterSet()
     * @param waterSet
     * @return
     */
    private ArrayList<WaterChartItem> getDailySamplesAvg(@NotNull ArrayList<WaterSample> waterSet) {
        ArrayList<WaterChartItem> chart_water_set = new ArrayList<>(); // data for the chart
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

                WaterSample chart_sample = new WaterSample(last_created_at, avg_pH, avg_orp, avg_turbidity); // a note about this, last_created_at is only important for getting the correct "dd/MM" value when creating the chart
                chart_sample.setKey(sample.key); // this line is not needed at all
                WaterChartItem chart_item = new WaterChartItem(chart_sample, counter); // set the summarized sample and the number of samples it contained

                chart_water_set.add(chart_item);

                // reset variables for next item in the chart (chart_water_set)
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

        return chart_water_set;
    }

    public void loadpHChart(View view) {
    }

    public void loadORPChart(View view) {
    }

    public void loadTurbidityChart(View view) {
    }

    public void loadTemperatureChart(View view) {
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
        outState.putParcelableArrayList("chart_water_set", chart_water_set);
    }
}
