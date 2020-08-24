package com.example.watermonitoring;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.anychart.AnyChartView;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import helpers.FirebaseHelper;
import helpers.MQTTHelper;
import helpers.WaterChartHelper;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqtt;

    FirebaseHelper db; // db contains data such as the water samples collected by the user
	String username;

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
        anyChartView.setProgressBar(findViewById(R.id.progress_bar));
        chart = new WaterChartHelper(); // later add the ArrayList with data as an argument
        anyChartView.setChart(chart.getCartesian()); // display the chart
    }

    // save values
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
		outState.putString("pH", username);
        outState.putString("pH", pH);
        outState.putString("orp", orp);
        outState.putString("turbidity", turbidity);
    }
}
