package com.example.watermonitoring;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import helpers.MQTTHelper;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqtt;
    MqttAndroidClient mqttAndroidClient;

    // water quality parameters
    TextView mpH, mOrp, mTurbidity;
    String pH, orp, turbidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            pH = savedInstanceState.getString("pH");
            orp = savedInstanceState.getString("orp");
            turbidity = savedInstanceState.getString("pH");
        }

        /* Create an MqttAndroidClient object and set a callback interface. */
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), "tcp://tailor.cloudmqtt.com:12475", "cliente-andres");

        setContentView(R.layout.activity_main);

        mpH = findViewById(R.id.pH);
        mOrp = findViewById(R.id.orp);
        mTurbidity = findViewById(R.id.turbidity);

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

        initMqtt(); // start connection and receive data
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
                // test case
                String msg = new String(message.getPayload());
                mpH.setText("Nivel de pH: " + msg);
                Log.i("mqtt", "message arrived: " + msg);

                // real case
                // get json object
                /*JSONObject jsonmsg = new JSONObject(new String(message.getPayload()));
                pH = jsonmsg.getString("pH");
                orp = jsonmsg.getString("orp");
                turbidity = jsonmsg.getString("turbidity");

                mpH.setText("Nivel de pH: " + pH);
                mOrp.setText("Nivel de orp: " + orp);
                mTurbidity.setText("Nivel de turbidez: " + turbidity);*/
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    // save values
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("pH", pH);
        outState.putString("orp", orp);
        outState.putString("turbidity", turbidity);
    }
}
