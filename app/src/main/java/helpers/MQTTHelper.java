package helpers;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MQTTHelper {
    public MqttAndroidClient mqttAndroidClient;

    private final String server_uri = "tcp://tailor.cloudmqtt.com:12475";

    final String client_id; // the client_id is the username of the client (username in the database)
    final String topic; // topic = "water/" + client_id

    private final String username = "qgdwogpe";
    private final String password = "o01oeATc9wc2";

    public MQTTHelper(Context context, String client_id) {
		this.client_id = client_id;
		topic = "water/" + client_id;
        mqttAndroidClient = new MqttAndroidClient(context, server_uri, client_id);
    }

    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    public void connect() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("water/Mqtt", "Connected to server!");
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("water/Mqtt", "Failed to connect to: " + server_uri + exception.toString());
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }


    private void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("water/Mqtt","Subscribed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("water/Mqtt", "Subscription error");
                }
            });
        } catch (MqttException ex) {
            System.err.println("Exception while subscribing");
            ex.printStackTrace();
        }
    }
}

