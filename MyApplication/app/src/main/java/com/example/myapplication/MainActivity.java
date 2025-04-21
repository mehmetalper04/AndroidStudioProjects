package com.example.myapplication;

import android.app.Activity;
import android.hardware.*;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;

    private TextView accText, gyroText, lightText, magText, compassText, proxText, pressureText, humidText, tempText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accText = findViewById(R.id.accelerometer);
        gyroText = findViewById(R.id.gyroscope);
        lightText = findViewById(R.id.light);
        magText = findViewById(R.id.magnetic);
        compassText = findViewById(R.id.orientation);
        proxText = findViewById(R.id.proximity);
        pressureText = findViewById(R.id.pressure);
        humidText = findViewById(R.id.humidity);
        tempText = findViewById(R.id.temperature);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        registerSensor(Sensor.TYPE_ACCELEROMETER);
        registerSensor(Sensor.TYPE_GYROSCOPE);
        registerSensor(Sensor.TYPE_LIGHT);
        registerSensor(Sensor.TYPE_MAGNETIC_FIELD);
        registerSensor(Sensor.TYPE_ORIENTATION);
        registerSensor(Sensor.TYPE_PROXIMITY);
        registerSensor(Sensor.TYPE_PRESSURE);
        registerSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        registerSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    private void registerSensor(int type) {
        Sensor sensor = sensorManager.getDefaultSensor(type);
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        String data = "";

        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                data = format(event.values);
                accText.setText("Accelerometer: " + data);
                break;
            case Sensor.TYPE_GYROSCOPE:
                data = format(event.values);
                gyroText.setText("Gyroscope: " + data);
                break;
            case Sensor.TYPE_LIGHT:
                lightText.setText("Light: " + event.values[0] + " lx");
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                data = format(event.values);
                magText.setText("Magnetometer: " + data);
                break;
            case Sensor.TYPE_ORIENTATION:
                data = format(event.values);
                compassText.setText("Compass: " + data);
                break;
            case Sensor.TYPE_PROXIMITY:
                proxText.setText("Proximity: " + event.values[0]);
                break;
            case Sensor.TYPE_PRESSURE:
                pressureText.setText("Pressure: " + event.values[0] + " hPa");
                break;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                humidText.setText("Humidity: " + event.values[0] + " %");
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                tempText.setText("Temperature: " + event.values[0] + " °C");
                break;
        }
    }

    private String format(float[] values) {
        return String.format("x: %.2f, y: %.2f, z: %.2f", values[0], values[1], values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onCreate(null);
    }
}
