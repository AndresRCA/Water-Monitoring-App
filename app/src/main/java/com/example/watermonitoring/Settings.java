package com.example.watermonitoring;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import helpers.FirebaseHelper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

public class Settings extends AppCompatActivity {

    private SharedPreferences userPreferences;
    private SharedPreferences settingsPreferences;
    private SharedPreferences.Editor settingsEditor;
    private FirebaseHelper db;

    private EditText pHMin;
    private EditText pHMax;
    private EditText orpMin;
    private EditText orpMax;
    private EditText turbidityMin;
    private EditText turbidityMax;
    private EditText temperatureMin;
    private EditText temperatureMax;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        pHMin = findViewById(R.id.ph_min);
        pHMax = findViewById(R.id.ph_max);
        orpMin = findViewById(R.id.orp_min);
        orpMax = findViewById(R.id.orp_max);
        turbidityMin = findViewById(R.id.turbidity_min);
        turbidityMax = findViewById(R.id.turbidity_max);
        temperatureMin = findViewById(R.id.temperature_min);
        temperatureMax = findViewById(R.id.temperature_max);
        setMinsAndMaxs();
        userPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
    }

    /**
     * set the values in the EditTexts if there is a value set in settingsPreferences
     */
    private void setMinsAndMaxs() {
        settingsPreferences = getSharedPreferences("settings", MODE_PRIVATE);

        float pH_min = settingsPreferences.getFloat("pH_min", -1);
        if(pH_min != -1) { // if there is a saved value
            pHMin.setText(String.valueOf(pH_min));
        }
        float pH_max = settingsPreferences.getFloat("pH_max", -1);
        if(pH_max != -1) { // if there is a saved value
            pHMax.setText(String.valueOf(pH_max));
        }

        int orp_min = settingsPreferences.getInt("orp_min", -1);
        if(orp_min != -1) { // if there is a saved value
            orpMin.setText(String.valueOf(orp_min));
        }
        int orp_max = settingsPreferences.getInt("orp_max", -1);
        if(orp_max != -1) { // if there is a saved value
            orpMax.setText(String.valueOf(orp_max));
        }

        float turbidity_min = settingsPreferences.getFloat("turbidity_min", -1);
        if(turbidity_min != -1) { // if there is a saved value
            turbidityMin.setText(String.valueOf(turbidity_min));
        }
        float turbidity_max = settingsPreferences.getFloat("turbidity_max", -1);
        if(turbidity_max != -1) { // if there is a saved value
            turbidityMax.setText(String.valueOf(turbidity_max));
        }

        float temperature_min = settingsPreferences.getFloat("temperature_min", -1);
        if(temperature_min != -1) { // if there is a saved value
            temperatureMin.setText(String.valueOf(temperature_min));
        }
        float temperature_max = settingsPreferences.getFloat("temperature_max", -1);
        if(temperature_max != -1) { // if there is a saved value
            temperatureMax.setText(String.valueOf(temperature_max));
        }
    }

    /**
     * save settingsPreferences values to firebase, those that were set that is.
     * @param v
     */
    public void applySettings(View v) {
        // start connection to firebase
        db = new FirebaseHelper(userPreferences.getString("username", null));

        // get settingsPreferences editor
        settingsEditor = settingsPreferences.edit();

        String pH_min_text = pHMin.getText().toString();
        float pH_min;
        if (!pH_min_text.isEmpty()) {
            pH_min = Float.parseFloat(pH_min_text);
            db.setPrefs("pH_min", pH_min, getCompletionListener("pH_min", pH_min));
        }
        String pH_max_text = pHMax.getText().toString();
        float pH_max;
        if (!pH_max_text.isEmpty()) {
            pH_max = Float.parseFloat(pH_max_text);
            db.setPrefs("pH_max", pH_max, getCompletionListener("pH_max", pH_max));
        }

        String orp_min_text = orpMin.getText().toString();
        int orp_min;
        if (!orp_min_text.isEmpty()) {
            orp_min = Integer.parseInt(orp_min_text);
            db.setPrefs("orp_min", orp_min, getCompletionListener("orp_min", orp_min));
        }
        String orp_max_text = orpMax.getText().toString();
        int orp_max;
        if (!orp_max_text.isEmpty()) {
            orp_max = Integer.parseInt(orp_max_text);
            db.setPrefs("orp_max", orp_max, getCompletionListener("orp_max", orp_max));
        }

        String turbidity_min_text = turbidityMin.getText().toString();
        float turbidity_min;
        if (!turbidity_min_text.isEmpty()) {
            turbidity_min = Float.parseFloat(turbidity_min_text);
            db.setPrefs("turbidity_min", turbidity_min, getCompletionListener("turbidity_min", turbidity_min));
        }
        String turbidity_max_text = turbidityMax.getText().toString();
        float turbidity_max;
        if (!turbidity_max_text.isEmpty()) {
            turbidity_max = Float.parseFloat(turbidity_max_text);
            db.setPrefs("turbidity_max", turbidity_max, getCompletionListener("turbidity_max", turbidity_max));
        }

        String temperature_min_text = temperatureMin.getText().toString();
        float temperature_min;
        if (!temperature_min_text.isEmpty()) {
            temperature_min = Float.parseFloat(temperature_min_text);
            db.setPrefs("temperature_min", temperature_min, getCompletionListener("temperature_min", temperature_min));
        }
        String temperature_max_text = temperatureMax.getText().toString();
        float temperature_max;
        if (!temperature_max_text.isEmpty()) {
            temperature_max = Float.parseFloat(temperature_max_text);
            db.setPrefs("temperature_max", temperature_max, getCompletionListener("temperature_max", temperature_max));
        }
        //finish();
    }

    private DatabaseReference.CompletionListener getCompletionListener(final String key, final Number value) {
        return new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    // proceed with saving the values to the settingsPreference
                    if (value.getClass() == Float.class) {
                        settingsEditor.putFloat(key, value.floatValue());
                    }
                    if (value.getClass() == Integer.class) {
                        settingsEditor.putInt(key, value.intValue());
                    }
                    settingsEditor.apply();
                }
                else {
                    Toast.makeText(getApplicationContext(), key + " value could not be set, there was an error in the database", Toast.LENGTH_SHORT).show();
                    //finish();
                }
            }
        };
    }
}
