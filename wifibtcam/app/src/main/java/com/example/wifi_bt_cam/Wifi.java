package com.example.wifi_bt_cam;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class Wifi extends AppCompatActivity {

    private static final int WIFI_PERMISSION_CODE = 200;
    private WifiManager wifiManager;
    private TextView textWifiStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        textWifiStatus = findViewById(R.id.text_wifi_status);
        Button buttonWifiOn = findViewById(R.id.button_wifi_on);
        Button buttonWifiOff = findViewById(R.id.button_wifi_off);

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        // Request permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE},
                    WIFI_PERMISSION_CODE);
        } else {
            updateWifiStatus();
        }

        buttonWifiOn.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
                wifiManager.setWifiEnabled(true);
                updateWifiStatus();
                Toast.makeText(this, "WiFi açılıyor", Toast.LENGTH_SHORT).show();
            }
        });

        buttonWifiOff.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
                wifiManager.setWifiEnabled(false);
                updateWifiStatus();
                Toast.makeText(this, "WiFi kapatılıyor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWifiStatus() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
            boolean isWifiEnabled = wifiManager.isWifiEnabled();
            textWifiStatus.setText("WiFi Durumu: " + (isWifiEnabled ? "AÇIK" : "KAPALI"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WIFI_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateWifiStatus();
            } else {
                Toast.makeText(this, "WiFi izinleri reddedildi", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWifiStatus();
    }
}