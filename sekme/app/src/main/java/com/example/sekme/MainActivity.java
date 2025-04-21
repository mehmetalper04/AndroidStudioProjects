package com.example.sekme;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    public int rate1=0,rate2=0,rate3=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        SeekBar sb1=(SeekBar) findViewById(R.id.seekBar);
        SeekBar sb2=(SeekBar) findViewById(R.id.seekBar4);
        SeekBar sb3=(SeekBar) findViewById(R.id.seekBar5);
        TextView txt1=(TextView) findViewById(R.id.textView0);
        TextView txt2=(TextView) findViewById(R.id.textView);
        TextView txt3=(TextView) findViewById(R.id.textView2);
        ConstraintLayout arkarenk = findViewById(R.id.main);
        sb1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sb1.setMax(255);
                int rate = android.graphics.Color.rgb(rate3, rate2, 1);
                arkarenk.setBackgroundColor(rate1);
                txt1.setText(String.valueOf(i));
                rate1=i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sb2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sb2.setMax(255);
                int rate = android.graphics.Color.rgb(rate3, 1, rate1);
                arkarenk.setBackgroundColor(rate2);
                txt2.setText(String.valueOf(i));
                rate2=i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sb3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sb3.setMax(255);
                int rate = android.graphics.Color.rgb(1, rate2, rate1);
                arkarenk.setBackgroundColor(rate3);
                txt3.setText(String.valueOf(i));
                rate3=i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}