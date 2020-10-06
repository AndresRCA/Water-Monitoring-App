package services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import androidx.annotation.Nullable;
import helpers.MQTTHelper;
import models.WaterSample;

public class MQTTService extends Service {

    public WaterSample sample;
    private MQTTHelper mqttHelper;
    public boolean isConnectedToMqttServer = false; // is this property actually useful?
    public boolean isServiceStarted = false; // property that is useful when a user is binding the service, that way they don't have to create another connection using mqttHelper
    private final IBinder binder = new MyBinder();

    public MQTTService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * onStart should only be called when the service is not started by an activity, in this case it's just a background process
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isServiceStarted = true;
        String username = intent.getStringExtra("username");

        mqttHelper = new MQTTHelper(getApplicationContext(), username);
        connectToMqttServer(new MQTTHelper.MQTTCallback() {
            @Override
            public void onSuccess() {
                isConnectedToMqttServer = true;
            }

            @Override
            public void onFailure() {
            }
        }, username);

        setMqttCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.i("water/onStart()connection", "connection complete!"); // is this the same as onSuccess? or is it just related to a reconnect?
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.i("water/onStart()connection", "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // get json object
                JSONObject jsonmsg = new JSONObject(new String(message.getPayload()));
                Log.i("mqtt", "message arrived" + new String(message.getPayload()));
                if(sample == null) {
                    sample = new WaterSample();
                }
                sample.pH = jsonmsg.getDouble("pH");
                sample.orp = jsonmsg.getInt("orp");
                sample.turbidity = jsonmsg.getDouble("turbidity");
                sample.temperature = jsonmsg.getDouble("temperature");

                // check sample parameters here for notifications
                // ...

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        return START_STICKY; // check what return values do what
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceStarted = false; // although at this point this property is useless huh
    }

    // Binder class
    public class MyBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }

    /**
     * onBind should only be called when an activity calls bindService(), the callback in bindService will handle all the
     * connection stuff related to the service itself (mqtt)
     * @param intent
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void connectToMqttServer(MQTTHelper.MQTTCallback callback, String username) {
        if (mqttHelper == null) {
            mqttHelper = new MQTTHelper(getApplicationContext(), username);
        }
        mqttHelper.connect(callback);
    }

    /**
     * set callbacks for incoming mqtt messages, it is expected that connectToMqttServer() is called before this function
     * otherwise it does nothing
     * @param mqttCallback
     */
    public void setMqttCallback(MqttCallbackExtended mqttCallback) {
        if (mqttHelper != null) {
            mqttHelper.setCallback(mqttCallback); // this callback should update the UI while using MQTTService.sample (it should also contain the same as contents as the one inside onStartCommand())
        }
    }
}
