package com.mehmetalper04.sinavgram.activities; // Kendi paket adınıza göre düzenleyin

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button; // Button importu
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetalper04.sinavgram.R;
import com.mehmetalper04.sinavgram.adapters.CourseAdapter;
import com.mehmetalper04.sinavgram.models.Course;
import com.mehmetalper04.sinavgram.network.ApiService;
import com.mehmetalper04.sinavgram.network.RetrofitClient;
import com.mehmetalper04.sinavgram.utils.TokenManager;

// AdMob için
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseSelectionActivity extends AppCompatActivity implements CourseAdapter.OnCourseClickListener {

    private static final String TAG = "CourseSelection";

    private RecyclerView recyclerViewCourses;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;
    private ProgressBar progressBar;

    // Yeni Butonlar
    private Button buttonGoToStatistics;
    private Button buttonGoToAddQuestion;
    private Button buttonLogout;

    private ApiService apiService;
    private TokenManager tokenManager;

    // Banner Reklam için
    private AdView mAdViewBanner;
    private boolean isUserPremium = false; // Premium durumunu saklamak için

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_selection);

        recyclerViewCourses = findViewById(R.id.recyclerViewCourses);
        progressBar = findViewById(R.id.progressBarCourseSelection);

        // Yeni Butonları Tanımla
        buttonGoToStatistics = findViewById(R.id.buttonGoToStatistics);
        buttonGoToAddQuestion = findViewById(R.id.buttonGoToAddQuestion);
        buttonLogout = findViewById(R.id.buttonLogout);

        apiService = RetrofitClient.getApiService(getApplicationContext());
        tokenManager = TokenManager.getInstance(getApplicationContext());

        if (!tokenManager.hasToken()) {
            Toast.makeText(this, "Lütfen önce giriş yapın.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        // Premium durumunu al (TokenManager'dan)
        isUserPremium = tokenManager.hasActivePremium();

        // Banner Reklamı Yükle (Premium değilse)
        mAdViewBanner = findViewById(R.id.adViewBannerCourseSelection);
        if (!isUserPremium) {
            if (mAdViewBanner != null) {
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdViewBanner.loadAd(adRequest);
                mAdViewBanner.setVisibility(View.VISIBLE);
            }
        } else {
            if (mAdViewBanner != null) {
                mAdViewBanner.setVisibility(View.GONE);
            }
        }


        setupRecyclerView();
        fetchCourses();

        // Buton Tıklama Olayları
        if (buttonGoToStatistics != null) {
            buttonGoToStatistics.setOnClickListener(v -> {
                Intent intent = new Intent(CourseSelectionActivity.this, StatisticsActivity.class);
                startActivity(intent);
            });
        }

        if (buttonGoToAddQuestion != null) {
            buttonGoToAddQuestion.setOnClickListener(v -> {
                Intent intent = new Intent(CourseSelectionActivity.this, SubmitQuestionActivity.class);
                startActivity(intent);
            });
        }

        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> {
                performLogout();
            });
        }
    }

    private void setupRecyclerView() {
        courseList = new ArrayList<>();
        recyclerViewCourses.setLayoutManager(new LinearLayoutManager(this));
        courseAdapter = new CourseAdapter(courseList, this, this);
        recyclerViewCourses.setAdapter(courseAdapter);
    }

    private void fetchCourses() {
        if (progressBar == null || apiService == null || tokenManager == null || !tokenManager.hasToken() || courseList == null || courseAdapter == null) {
            if(tokenManager != null && !tokenManager.hasToken()) navigateToLogin();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        String authToken = "Bearer " + tokenManager.getToken();

        apiService.getCourses(authToken).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(@NonNull Call<List<Course>> call, @NonNull Response<List<Course>> response) {
                if (!isDestroyed() && !isFinishing()) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        courseList.clear();
                        courseList.addAll(response.body());
                        courseAdapter.notifyDataSetChanged();

                        if (courseList.isEmpty()) {
                            Toast.makeText(CourseSelectionActivity.this, "Uygun ders bulunamadı.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Dersler alınamadı. Kod: " + response.code());
                        Toast.makeText(CourseSelectionActivity.this, "Dersler alınamadı. Hata kodu: " + response.code(), Toast.LENGTH_LONG).show();
                        if (response.code() == 401 || response.code() == 403) {
                            tokenManager.clearToken();
                            navigateToLogin();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Course>> call, @NonNull Throwable t) {
                if (!isDestroyed() && !isFinishing()) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Dersler API çağrısı başarısız", t);
                    Toast.makeText(CourseSelectionActivity.this, "Ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onCourseClick(Course course) {
        Toast.makeText(this, "Seçilen Ders: " + course.getName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(CourseSelectionActivity.this, QuizActivity.class);
        intent.putExtra("COURSE_ID", course.getId());
        intent.putExtra("COURSE_NAME", course.getName());
        startActivity(intent);
    }

    private void performLogout() {
        if (tokenManager != null) {
            tokenManager.clearToken(); // Token ve kullanıcı bilgilerini temizle
        }
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(CourseSelectionActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Geri tuşuyla bu aktiviteye dönülmesini engelle
        startActivity(intent);
        finish(); // CourseSelectionActivity'yi sonlandır
    }

    // Reklam yaşam döngüsü metodları
    @Override
    protected void onPause() {
        if (mAdViewBanner != null) {
            mAdViewBanner.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdViewBanner != null) {
            mAdViewBanner.resume();
        }
        // Premium durumu değişmiş olabilir, UI'ı güncelle
        if (tokenManager != null) {
            boolean currentPremiumStatus = tokenManager.hasActivePremium();
            if (isUserPremium != currentPremiumStatus) {
                isUserPremium = currentPremiumStatus;
                if (mAdViewBanner != null) {
                    mAdViewBanner.setVisibility(isUserPremium ? View.GONE : View.VISIBLE);
                    if (!isUserPremium) { // Eğer premium değilse ve reklam görünürse, yeniden yükle (opsiyonel)
                        AdRequest adRequest = new AdRequest.Builder().build();
                        mAdViewBanner.loadAd(adRequest);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mAdViewBanner != null) {
            mAdViewBanner.destroy();
        }
        super.onDestroy();
    }
}