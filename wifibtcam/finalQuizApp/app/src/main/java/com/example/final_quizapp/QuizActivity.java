package com.example.final_quizapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.final_quizapp.ApiInterface;
import com.example.final_quizapp.R;
import com.example.final_quizapp.RetrofitClient;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.annotation.NonNull;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";

    private ImageView questionImage;
    private RadioGroup optionsGroup;
    private Button submitButton, skipButton;
    private InterstitialAd interstitialAd;

    private String correctAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        initializeUI();
        loadAd();
        fetchNextQuestion();

        // Handle the Skip Button
        skipButton.setOnClickListener(v -> fetchNextQuestion());

        // Handle the Submit Button
        submitButton.setOnClickListener(v -> {
            int selectedOptionId = optionsGroup.getCheckedRadioButtonId();
            if (selectedOptionId == -1) {
                Toast.makeText(this, "Please select an answer or skip the question", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedOption = findViewById(selectedOptionId);
            String userAnswer = selectedOption.getText().toString();

            if (userAnswer.equals(correctAnswer)) {
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
                fetchNextQuestion();
            } else {
                Toast.makeText(this, "Incorrect!", Toast.LENGTH_SHORT).show();
                showAd(() -> fetchNextQuestion());
            }
        });
    }

    private void initializeUI() {
        questionImage = findViewById(R.id.question_image);
        optionsGroup = findViewById(R.id.options_group);
        submitButton = findViewById(R.id.submit_button);
        skipButton = findViewById(R.id.skip_button);
    }

    private void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, getString(R.string.ad_unit_id_interstitial), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        Log.d(TAG, "Ad loaded successfully.");
                    }


                });
    }


    private void showAd(Runnable onAdComplete) {
        if (interstitialAd != null) {
            interstitialAd.show(this);
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    onAdComplete.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                    onAdComplete.run();
                }
            });
        } else {
            Log.d(TAG, "Ad not ready, skipping...");
            onAdComplete.run();
        }
    }

    private void fetchNextQuestion() {
        ApiInterface apiInterface = RetrofitClient.getClient().create(ApiInterface.class);
        apiInterface.getRandomQuestion("USER_ID").enqueue(new Callback<Question>() {
            @Override
            public void onResponse(Call<Question> call, Response<Question> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayQuestion(response.body());
                } else {
                    Toast.makeText(QuizActivity.this, "No more questions available!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Question> call, Throwable t) {
                Log.e(TAG, "Failed to fetch question: " + t.getMessage());
                Toast.makeText(QuizActivity.this, "Error loading question. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayQuestion(Question question) {
        // Load question image
        Glide.with(this).load(question.getImageUrl()).into(questionImage);

        // Set answer options
        correctAnswer = question.getCorrectAnswer();
        ((RadioButton) findViewById(R.id.option_1)).setText(question.getOption1());
        ((RadioButton) findViewById(R.id.option_2)).setText(question.getOption2());
        ((RadioButton) findViewById(R.id.option_3)).setText(question.getOption3());
        ((RadioButton) findViewById(R.id.option_4)).setText(question.getOption4());
        ((RadioButton) findViewById(R.id.option_5)).setText(question.getOption5());

        // Clear previously selected options
        optionsGroup.clearCheck();
    }
}
