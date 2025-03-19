package com.example.final_quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NoInternetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        Button retryButton = findViewById(R.id.retry_button);
        retryButton.setOnClickListener(v -> {
            if (NetworkUtils.isInternetAvailable(this)) {
                startActivity(new Intent(NoInternetActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Still no internet connection!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}