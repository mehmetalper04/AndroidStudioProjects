package com.mehmetalper04.sinavgram.activities; // Kendi paket adınıza göre düzenleyin

import android.content.Intent;
import android.net.Uri; // Web sayfasını açmak için
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button; // Premium butonu için
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.mehmetalper04.sinavgram.R; // Kendi R dosyanız
import com.mehmetalper04.sinavgram.activities.LoginActivity;
import com.mehmetalper04.sinavgram.models.UserStats;
import com.mehmetalper04.sinavgram.network.ApiService;
import com.mehmetalper04.sinavgram.network.RetrofitClient;
import com.mehmetalper04.sinavgram.utils.TokenManager;

import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {

    private static final String TAG = "StatisticsActivity";

    private TextView textViewStatsUsername, textViewStatsScore, textViewStatsTotalAnswered;
    private TextView textViewStatsCorrect, textViewStatsIncorrect, textViewStatsBlank;
    private TextView textViewStatsAccuracy, textViewStatsTrueRate, textViewStatsFalseRate, textViewStatsBlankRate;
    private TextView textViewStatsEmailVerified, textViewStatsIsPremium, textViewStatsPremiumExpiration;
    private Button buttonGoToPremiumWeb; // Premium web sayfasına yönlendirme butonu
    private ProgressBar progressBarStats;
    private NestedScrollView statsContentLayout;

    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics); // activity_statistics.xml layout'unuz

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("İstatistiklerim");
        }

        apiService = RetrofitClient.getApiService(getApplicationContext());
        tokenManager = TokenManager.getInstance(getApplicationContext());

        if (!tokenManager.hasToken()) {
            Toast.makeText(this, "Lütfen önce giriş yapın.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        initViews(); // initViews'ı token kontrolünden sonra çağır

        if (buttonGoToPremiumWeb != null) {
            buttonGoToPremiumWeb.setOnClickListener(v -> {
                String premiumWebUrl = "https://www.siteniz.com/premium"; // KENDİ URL'NİZLE DEĞİŞTİRİN
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(premiumWebUrl));
                try {
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(StatisticsActivity.this, "Web sayfası açılamadı.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Web sayfası açılırken hata: ", e);
                }
            });
        }

        fetchUserStats();
    }

    private void initViews() {
        statsContentLayout = findViewById(R.id.statsContentLayout);
        progressBarStats = findViewById(R.id.progressBarStats);

        textViewStatsUsername = findViewById(R.id.textViewStatsUsername);
        textViewStatsScore = findViewById(R.id.textViewStatsScore);
        textViewStatsTotalAnswered = findViewById(R.id.textViewStatsTotalAnswered);
        textViewStatsCorrect = findViewById(R.id.textViewStatsCorrect);
        textViewStatsIncorrect = findViewById(R.id.textViewStatsIncorrect);
        textViewStatsBlank = findViewById(R.id.textViewStatsBlank);
        textViewStatsAccuracy = findViewById(R.id.textViewStatsAccuracy);
        textViewStatsTrueRate = findViewById(R.id.textViewStatsTrueRate);
        textViewStatsFalseRate = findViewById(R.id.textViewStatsFalseRate);
        textViewStatsBlankRate = findViewById(R.id.textViewStatsBlankRate);
        textViewStatsEmailVerified = findViewById(R.id.textViewStatsEmailVerified);
        textViewStatsIsPremium = findViewById(R.id.textViewStatsIsPremium);
        textViewStatsPremiumExpiration = findViewById(R.id.textViewStatsPremiumExpiration);
        //buttonGoToPremiumWeb = findViewById(R.id.buttonGoToPremiumWeb); // XML'deki ID ile eşleşmeli
    }

    private void fetchUserStats() {
        if(statsContentLayout == null || progressBarStats == null) return;

        statsContentLayout.setVisibility(View.INVISIBLE);
        progressBarStats.setVisibility(View.VISIBLE);
        String authToken = "Bearer " + tokenManager.getToken();

        Call<UserStats> call = apiService.getUserStats(authToken);
        call.enqueue(new Callback<UserStats>() {
            @Override
            public void onResponse(Call<UserStats> call, Response<UserStats> response) {
                progressBarStats.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    displayStats(response.body());
                    statsContentLayout.setVisibility(View.VISIBLE);
                } else {
                    Log.e(TAG, "İstatistikler alınamadı. Kod: " + response.code());
                    Toast.makeText(StatisticsActivity.this, "İstatistikler alınamadı. Hata kodu: " + response.code(), Toast.LENGTH_LONG).show();
                    if (response.code() == 401 || response.code() == 403) {
                        tokenManager.clearToken(); // TokenManager'daki clear metodunu kullanın
                        navigateToLogin();
                    }
                }
            }

            @Override
            public void onFailure(Call<UserStats> call, Throwable t) {
                progressBarStats.setVisibility(View.GONE);
                Log.e(TAG, "İstatistik API çağrısı başarısız", t);
                Toast.makeText(StatisticsActivity.this, "Ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayStats(UserStats stats) {
        textViewStatsUsername.setText(stats.getUsername());
        textViewStatsScore.setText(String.valueOf(stats.getScore()));
        textViewStatsTotalAnswered.setText(String.valueOf(stats.getTotalAnsweredQuestions()));
        textViewStatsCorrect.setText(String.valueOf(stats.getCorrectlyAnsweredQuestions()));
        textViewStatsIncorrect.setText(String.valueOf(stats.getIncorrectlyAnsweredQuestions()));
        textViewStatsBlank.setText(String.valueOf(stats.getBlankAnsweredQuestions()));

        textViewStatsAccuracy.setText(String.format(Locale.getDefault(), "%.2f%%", stats.getAccuracyRateExcludingBlanks()));

        Map<String, Double> rates = stats.getRates();
        if (rates != null) {
            textViewStatsTrueRate.setText(String.format(Locale.getDefault(), "%.2f%%", rates.getOrDefault("true", 0.0)));
            textViewStatsFalseRate.setText(String.format(Locale.getDefault(), "%.2f%%", rates.getOrDefault("false", 0.0)));
            textViewStatsBlankRate.setText(String.format(Locale.getDefault(), "%.2f%%", rates.getOrDefault("blank", 0.0)));
        }

        textViewStatsEmailVerified.setText(stats.isEmailVerified() ? "Evet" : "Hayır");
        // hasActivePremium durumunu TokenManager'dan da alabiliriz veya UserStats'tan gelenle senkronize edebiliriz.
        // Şimdilik UserStats'a güveniyoruz.
        boolean isActivePremium = stats.hasActivePremium(); // UserStats modelinden gelen değer
        textViewStatsIsPremium.setText(isActivePremium ? "Evet (Aktif)" : (stats.isPremium() ? "Evet (Süresi Dolmuş)" : "Hayır"));

        if (isActivePremium && stats.getPremiumExpirationDate() != null) {
            textViewStatsPremiumExpiration.setText("Bitiş: " + stats.getPremiumExpirationDate().substring(0, 10));
            textViewStatsPremiumExpiration.setVisibility(View.VISIBLE);
        } else {
            textViewStatsPremiumExpiration.setVisibility(View.GONE);
        }

        // Premium butonunun metnini güncelle
        if (buttonGoToPremiumWeb != null) {
            if (isActivePremium) {
                buttonGoToPremiumWeb.setText("Premium Üyeliğimi Yönet");
            } else {
                buttonGoToPremiumWeb.setText("Premium Üyeliği İncele");
            }
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(StatisticsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}