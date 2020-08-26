package com.example.watermonitoring;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import helpers.FirebaseHelper;
import helpers.MQTTHelper;
import helpers.WaterChartHelper;
import models.WaterSample;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqtt;

    FirebaseHelper db; // db contains data such as the water samples collected by the user
	String username;

    List<DataEntry> series_data;
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
        }

        setContentView(R.layout.activity_main);

        mpH = findViewById(R.id.pH);
        mOrp = findViewById(R.id.orp);
        mTurbidity = findViewById(R.id.turbidity);
        anyChartView = findViewById(R.id.any_chart_view);

        if (pH == null || orp == null || turbidity == null) { // usually this data comes in a single object, so if pH is null then so is everyone else, but I evaluate the three variables for readability
            mpH.setText("Nivel de pH: conectando a servidor...");
            mOrp.setText("Nivel de orp: conectando a servidor...");
            mTurbidity.setText("Nivel de turbidez: conectando a servidor...");
        }
        else {
            mpH.setText("Nivel de pH: " + pH);
            mOrp.setText("Nivel de orp: " + orp);
            mTurbidity.setText("Nivel de turbidez: " + turbidity);
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

    private void initMqtt() {
        mqtt = new MQTTHelper(getApplicationContext());
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.i("connection", "connection complete!");
                mpH.setText("Nivel de pH: conectado! esperando datos...");
                mOrp.setText("Nivel de orp: conectado! esperando datos...");
                mTurbidity.setText("Nivel de turbidez: conectado! esperando datos...");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.i("connection", "connection lost");
                mpH.setText("Nivel de pH: conexiÃ³n perdida");
                mOrp.setText("Nivel de orp: conexiÃ³n perdida");
                mTurbidity.setText("Nivel de turbidez: conexiÃ³n perdida");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // get json object
                JSONObject jsonmsg = new JSONObject(new String(message.getPayload()));
                Log.i("mqtt", "message arrived" + new String(message.getPayload()));
                pH = jsonmsg.getString("pH");
                orp = jsonmsg.getString("orp");
                turbidity = jsonmsg.getString("turbidity");

                mpH.setText("Nivel de pH: " + pH);
                mOrp.setText("Nivel de orp: " + orp);
                mTurbidity.setText("Nivel de turbidez: " + turbidity);
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
        if (series_data == null) { // retrieve the data from the db to display it in the chart
            Log.i("initChart", "series_data is null, calling db.setInitialWaterSet");

            /* this progress bar is not showing at all, in fact, when this line is all this function does, the application crashes */
            anyChartView.setProgressBar(findViewById(R.id.progress_bar)); // display progress bar while retrieving the data to be used in the chart

            db.setInitialWaterSet(new FirebaseHelper.WaterSetCallback() {
                @Override
                public void onSuccess(ArrayList<WaterSample> waterSet) {
                    series_data = new ArrayList<>(); // data for the chart
                    String last_date = waterSet.get(0).getStrDate("dd/MM");
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
                            series_data.add(new CustomDataEntry(last_date, avg_pH, avg_orp, avg_turbidity));

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
                    }

                    chart = new WaterChartHelper(); // create a chart
                    Log.i("initChart", "inserting data to chart...");
                    chart.insertSeriesData(series_data); // insert values to show in the chart
                    anyChartView.setChart(chart.getCartesian()); // display chart
                }

                @Override
                public void onFailure() {
                }
            });
        }
        else { // display the existing chart
            chart = new WaterChartHelper(); // create a chart
            chart.insertSeriesData(series_data); // insert values to show in the chart, series_data might turn into ArrayList<WaterSample> water_set if I can't find a way to save the series_data list
            anyChartView.setChart(chart.getCartesian());
        }
    }

    // save values
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
		outState.putString("pH", username);
        outState.putString("pH", pH);
        outState.putString("orp", orp);
        outState.putString("turbidity", turbidity);
        //outState.putParcelableArrayList("series_data", series_data); // an ArrayList works as well if the object extends Parceable (or something like that)
    }

    // class used for inserting data to the chart
    private class CustomDataEntry extends ValueDataEntry {

        CustomDataEntry(String x, Number value, Number value2, Number value3) {
            super(x, value);
            setValue("value2", value2);
            setValue("value3", value3);
        }
    }
}
