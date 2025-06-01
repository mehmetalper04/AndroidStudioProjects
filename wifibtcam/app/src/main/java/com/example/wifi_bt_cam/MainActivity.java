package com.example.wifi_bt_cam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonBluetooth = findViewById(R.id.button_bluetooth);
        Button buttonCamera = findViewById(R.id.button_camera);
        Button buttonWifi = findViewById(R.id.button_wifi);

        buttonBluetooth.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Bluetooth.class);
            startActivity(intent);
        });

        buttonCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Camera.class);
            startActivity(intent);
        });

        buttonWifi.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Wifi.class);
            startActivity(intent);
        });
    }
}