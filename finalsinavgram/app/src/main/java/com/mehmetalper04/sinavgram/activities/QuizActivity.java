package com.mehmetalper04.sinavgram.activities; // Kendi paket adınız

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.mehmetalper04.sinavgram.R; // Kendi R dosyanız
import com.mehmetalper04.sinavgram.models.ApiResponse;
import com.mehmetalper04.sinavgram.models.AnswerSubmitRequest;
import com.mehmetalper04.sinavgram.models.AnswerSubmitResponse;
import com.mehmetalper04.sinavgram.models.Question;
import com.mehmetalper04.sinavgram.models.ReportRequest;
import com.mehmetalper04.sinavgram.network.ApiService;
import com.mehmetalper04.sinavgram.network.RetrofitClient;
import com.mehmetalper04.sinavgram.utils.Constants;
import com.mehmetalper04.sinavgram.utils.TokenManager;

// AdMob importları
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

// Glide (eğer kullanılacaksa yorum satırını kaldırın ve bağımlılığı ekleyin)
// import com.bumptech.glide.Glide;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";

    private TextView textViewQuestion, textViewQuizCourseName, textViewUserScore;
    private ImageView imageViewQuestion;
    private RadioGroup radioGroupOptions;
    private RadioButton radioButtonOption1, radioButtonOption2, radioButtonOption3, radioButtonOption4, radioButtonOption5, radioButtonOptionEmpty;
    private Button buttonSubmitAnswer;
    private ImageButton buttonReportQuestion, buttonShareQuestion;
    private ProgressBar progressBarQuiz;
    private NestedScrollView quizContentLayout;
    private View quizContainer; // Root layout for swipe gestures

    private ApiService apiService;
    private TokenManager tokenManager;

    private int currentCourseId;
    private String currentCourseName;
    private Question currentQuestion;
    private int userScore = 0;

    // Reklam değişkenleri
    private AdView mAdViewBanner;
    private InterstitialAd mInterstitialAd;
    private boolean isUserPremium = false;
    private int swipeCounter = 0;
    private final int AD_TRIGGER_SWIPE_COUNT = 5;

    // Kaydırma için GestureDetector
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        if (getIntent().getExtras() != null) {
            currentCourseId = getIntent().getIntExtra("COURSE_ID", -1);
            currentCourseName = getIntent().getStringExtra("COURSE_NAME");
            Log.d(TAG, "onCreate - Alınan Course ID: " + currentCourseId + ", Adı: " + currentCourseName);
        }

        if (currentCourseId == -1) {
            Toast.makeText(this, "Geçersiz ders ID'si.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService(getApplicationContext());
        tokenManager = TokenManager.getInstance(getApplicationContext());

        if (!tokenManager.hasToken()) {
            Toast.makeText(this, "Lütfen önce giriş yapın.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        isUserPremium = tokenManager.hasActivePremium();
        Log.d(TAG, "Kullanıcının premium durumu (isUserPremium): " + isUserPremium);


        initViews();

        if (textViewQuizCourseName != null) {
            textViewQuizCourseName.setText(currentCourseName != null ? currentCourseName : "Sınav");
        }
        updateScoreDisplay();

        mAdViewBanner = findViewById(R.id.adViewBannerQuiz);
        if (!isUserPremium) {
            if (mAdViewBanner != null) {
                AdRequest bannerAdRequest = new AdRequest.Builder().build();
                mAdViewBanner.loadAd(bannerAdRequest);
                mAdViewBanner.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "Banner AdView (adViewBannerQuiz) XML'de bulunamadı!");
            }
            loadInterstitialAd();
        } else {
            if (mAdViewBanner != null) {
                mAdViewBanner.setVisibility(View.GONE);
            }
        }

        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
        if (quizContainer != null) {
            quizContainer.setOnTouchListener((v, event) -> {
                Log.d(TAG, "quizContainer onTouch - Olay: " + MotionEvent.actionToString(event.getAction()));
                boolean consumed = gestureDetector.onTouchEvent(event);
                Log.d(TAG, "GestureDetector olayı tüketti mi?: " + consumed);
                return consumed;
            });
        } else {
            Log.e(TAG, "onCreate: quizContainer (root layout) XML'de bulunamadı veya ID'si yanlış!");
        }

        if (buttonSubmitAnswer != null) {
            buttonSubmitAnswer.setOnClickListener(v -> handleSubmitAnswer());
        } else {
            Log.e(TAG, "onCreate: buttonSubmitAnswer XML'de bulunamadı veya ID'si yanlış!");
        }

        if (buttonReportQuestion != null) {
            buttonReportQuestion.setOnClickListener(v -> handleReportQuestion());
        } else {
            Log.e(TAG, "onCreate: buttonReportQuestion XML'de bulunamadı veya ID'si yanlış!");
        }

        if (buttonShareQuestion != null) {
            buttonShareQuestion.setOnClickListener(v -> handleShareQuestion());
        } else {
            Log.e(TAG, "onCreate: buttonShareQuestion XML'de bulunamadı veya ID'si yanlış!");
        }

        fetchQuestion();
    }

    private void initViews() {
        quizContainer = findViewById(R.id.quizContainer);
        textViewQuestion = findViewById(R.id.textViewQuestion);
        imageViewQuestion = findViewById(R.id.imageViewQuestion);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        radioButtonOption1 = findViewById(R.id.radioButtonOption1);
        radioButtonOption2 = findViewById(R.id.radioButtonOption2);
        radioButtonOption3 = findViewById(R.id.radioButtonOption3);
        radioButtonOption4 = findViewById(R.id.radioButtonOption4);
        radioButtonOption5 = findViewById(R.id.radioButtonOption5);
        radioButtonOptionEmpty = findViewById(R.id.radioButtonOptionEmpty);

        buttonSubmitAnswer = findViewById(R.id.buttonSubmitAnswer);
        progressBarQuiz = findViewById(R.id.progressBarQuiz);
        quizContentLayout = findViewById(R.id.quizContentLayout);

        buttonReportQuestion = findViewById(R.id.buttonReportQuestion);
        buttonShareQuestion = findViewById(R.id.buttonShareQuestion);

        textViewQuizCourseName = findViewById(R.id.textViewCourseName);
        textViewUserScore = findViewById(R.id.textViewStatsScore);

        if (radioButtonOption1 != null) radioButtonOption1.setTag("A");
        if (radioButtonOption2 != null) radioButtonOption2.setTag("B");
        if (radioButtonOption3 != null) radioButtonOption3.setTag("C");
        if (radioButtonOption4 != null) radioButtonOption4.setTag("D");
        if (radioButtonOption5 != null) radioButtonOption5.setTag("E");
        if (radioButtonOptionEmpty != null) radioButtonOptionEmpty.setTag("F");
    }

    private void updateScoreDisplay() {
        if (textViewUserScore != null) {
            textViewUserScore.setText("Puan: " + userScore);
        }
    }

    private void fetchQuestion() {
        if (progressBarQuiz == null || quizContentLayout == null || apiService == null || tokenManager == null || !tokenManager.hasToken()) {
            Log.e(TAG, "fetchQuestion: Gerekli bileşenlerden biri null veya token yok. progressBarQuiz: " + (progressBarQuiz == null) + ", quizContentLayout: " + (quizContentLayout == null) + ", apiService: " + (apiService == null) + ", tokenManager: " + (tokenManager == null) + (tokenManager != null ? ", hasToken: " + tokenManager.hasToken() : ""));
            if(tokenManager != null && !tokenManager.hasToken()) navigateToLogin();
            return;
        }

        Log.d(TAG, "fetchQuestion çağrıldı. Course ID: " + currentCourseId);
        String token = tokenManager.getToken();
        Log.d(TAG, "Kullanılan Token (ilk 10 karakter): " + (token != null && token.length() > 10 ? token.substring(0, 10) : token));

        quizContentLayout.setVisibility(View.INVISIBLE);
        progressBarQuiz.setVisibility(View.VISIBLE);
        String authToken = "Bearer " + token;

        apiService.getQuestionForCourse(authToken, currentCourseId).enqueue(new Callback<Question>() {
            @Override
            public void onResponse(@NonNull Call<Question> call, @NonNull Response<Question> response) {
                if (!isDestroyed() && !isFinishing()) {
                    if (progressBarQuiz != null) progressBarQuiz.setVisibility(View.GONE);
                    Log.d(TAG, "fetchQuestion - onResponse. Kod: " + response.code() + ", Mesaj: " + response.message());

                    if (response.isSuccessful() && response.body() != null) {
                        currentQuestion = response.body();
                        Log.d(TAG, "Soru başarıyla alındı: ID " + currentQuestion.getId() + ", Metin: " + currentQuestion.getText());
                        displayQuestion(currentQuestion);
                        if (quizContentLayout != null) quizContentLayout.setVisibility(View.VISIBLE);
                    } else {
                        Log.e(TAG, "fetchQuestion - onResponse: Yanıt başarılı değil veya body null.");
                        handleFetchQuestionError(response);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Question> call, @NonNull Throwable t) {
                if (!isDestroyed() && !isFinishing()) {
                    if (progressBarQuiz != null) progressBarQuiz.setVisibility(View.GONE);
                    Log.e(TAG, "fetchQuestion - onFailure: API Çağrısı Başarısız. Hata: " + t.toString());
                    Toast.makeText(QuizActivity.this, "Ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void handleFetchQuestionError(Response<Question> response) {
        String message = "Bu derste uygun soru bulunamadı veya tüm soruları yanıtladınız.";
        if (response.errorBody() != null) {
            try {
                message = response.errorBody().string() + " (Kod: " + response.code() + ")";
            } catch (Exception e) { Log.e(TAG, "Error body parse failed", e); }
        } else if (response.code() != 404) {
            message = "Soru alınamadı. Hata kodu: " + response.code();
        }
        Toast.makeText(QuizActivity.this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "handleFetchQuestionError: " + message);

        if (response.code() == 401 || response.code() == 403) {
            if (tokenManager != null) tokenManager.clearToken();
            navigateToLogin();
        }
    }

    private void displayQuestion(Question question) {
        if (question == null || textViewQuestion == null || radioGroupOptions == null) {
            Log.e(TAG, "displayQuestion: Soru veya UI elemanları null.");
            return;
        }
        Log.d(TAG, "displayQuestion çağrıldı. Soru metni: " + question.getText());

        textViewQuestion.setText(question.getText());
        radioGroupOptions.clearCheck();

        Map<String, String> options = question.getOptions();
        if (options != null) {
            if (radioButtonOption1 != null) radioButtonOption1.setText(options.getOrDefault("A", ""));
            if (radioButtonOption2 != null) radioButtonOption2.setText(options.getOrDefault("B", ""));
            if (radioButtonOption3 != null) radioButtonOption3.setText(options.getOrDefault("C", ""));
            if (radioButtonOption4 != null) radioButtonOption4.setText(options.getOrDefault("D", ""));
            if (radioButtonOption5 != null) radioButtonOption5.setText(options.getOrDefault("E", ""));
        }
        if (radioButtonOptionEmpty != null) {
            try {
                radioButtonOptionEmpty.setText(getString(R.string.quiz_option_empty));
            } catch (Exception e) {
                Log.e(TAG, "getString(R.string.quiz_option_empty) hata verdi", e);
                radioButtonOptionEmpty.setText("Boş Bırak"); // Fallback
            }
        }

        if (imageViewQuestion != null) {
            if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
                imageViewQuestion.setVisibility(View.VISIBLE);
                // Glide kullanımı için:
                /*
                if (!isDestroyed() && !isFinishing()) {
                    Glide.with(this)
                         .load(question.getImageUrl())
                         .placeholder(R.drawable.ic_placeholder_image)
                         .error(R.drawable.ic_error_image)
                         .into(imageViewQuestion);
                }
                */
            } else {
                imageViewQuestion.setVisibility(View.GONE);
            }
        }
    }

    private void handleSubmitAnswer() {
        if (radioGroupOptions == null || currentQuestion == null) {
            Toast.makeText(QuizActivity.this, "Bir hata oluştu, lütfen tekrar deneyin.", Toast.LENGTH_SHORT).show();
            return;
        }
        int selectedRadioButtonId = radioGroupOptions.getCheckedRadioButtonId();
        if (selectedRadioButtonId == -1) {
            Toast.makeText(QuizActivity.this, "Lütfen bir seçenek işaretleyin.", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
        if (selectedRadioButton == null) {
            Toast.makeText(QuizActivity.this, "Seçili buton bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedOptionTag = (String) selectedRadioButton.getTag();
        if (selectedOptionTag == null) {
            Toast.makeText(QuizActivity.this, "Seçili butonun tag'i tanımsız.", Toast.LENGTH_SHORT).show();
            return;
        }
        submitAnswerLogic(currentQuestion.getId(), selectedOptionTag);
    }

    private void submitAnswerLogic(int questionId, String selectedOption) {
        if (progressBarQuiz == null || apiService == null || tokenManager == null || !tokenManager.hasToken()) {
            Log.e(TAG, "submitAnswerLogic: Gerekli bileşenlerden biri null veya token yok.");
            if(tokenManager != null && !tokenManager.hasToken()) navigateToLogin();
            return;
        }

        progressBarQuiz.setVisibility(View.VISIBLE);
        String authToken = "Bearer " + tokenManager.getToken();
        AnswerSubmitRequest request = new AnswerSubmitRequest(questionId, selectedOption);

        apiService.submitAnswer(authToken, request).enqueue(new Callback<AnswerSubmitResponse>() {
            @Override
            public void onResponse(@NonNull Call<AnswerSubmitResponse> call, @NonNull Response<AnswerSubmitResponse> response) {
                if (!isDestroyed() && !isFinishing()) {
                    if (progressBarQuiz != null) progressBarQuiz.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        AnswerSubmitResponse submitResponse = response.body();
                        Toast.makeText(QuizActivity.this, submitResponse.getMessage(), Toast.LENGTH_LONG).show();
                        userScore = submitResponse.getNewScore();
                        updateScoreDisplay();

                        Log.d(TAG, "submitAnswerLogic - show_ad_trigger: " + submitResponse.isShowAdTrigger() + ", isUserPremium: " + isUserPremium);
                        if (submitResponse.isShowAdTrigger() && !isUserPremium) {
                            showInterstitialAdIfNeeded("answer");
                        }
                        fetchQuestion();
                    } else {
                        String errorMessage = "Cevap gönderilemedi.";
                        if (response.errorBody() != null) {
                            try {
                                errorMessage = response.errorBody().string() + " (Kod: " + response.code() + ")";
                            } catch (Exception e) { Log.e(TAG, "Error body parse failed", e); }
                        }
                        Toast.makeText(QuizActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        if (response.code() == 401 || response.code() == 403) {
                            if (tokenManager != null) tokenManager.clearToken();
                            navigateToLogin();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AnswerSubmitResponse> call, @NonNull Throwable t) {
                if (!isDestroyed() && !isFinishing()) {
                    if (progressBarQuiz != null) progressBarQuiz.setVisibility(View.GONE);
                    Log.e(TAG, "Cevap gönderme API çağrısı başarısız", t);
                    Toast.makeText(QuizActivity.this, "Ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void handleReportQuestion() {
        if (currentQuestion != null) {
            showReportDialog();
        } else {
            Toast.makeText(QuizActivity.this, "Raporlanacak soru bulunamadı.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String reportTitle;
        try {
            reportTitle = getString(R.string.quiz_report_question);
        } catch (Exception e) {
            reportTitle = "Soruyu Raporla"; // Fallback
            Log.e(TAG, "getString(R.string.quiz_report_question) hata verdi, fallback kullanılıyor.", e);
        }
        builder.setTitle(reportTitle);

        builder.setMessage("Lütfen sorun olduğunu düşündüğünüz nedeni kısaca belirtin (isteğe bağlı):");

        final EditText inputReason = new EditText(this);
        inputReason.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputReason.setHint("Rapor nedeni...");
        builder.setView(inputReason);

        builder.setPositiveButton("Gönder", (dialog, which) -> {
            String reason = inputReason.getText().toString().trim();
            if (currentQuestion != null) {
                sendReport(currentQuestion.getId(), reason);
            }
        });
        builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendReport(int questionId, String reason) {
        if (progressBarQuiz == null || apiService == null || tokenManager == null || !tokenManager.hasToken()) {
            Log.e(TAG, "sendReport: Gerekli bileşenlerden biri null veya token yok.");
            if(tokenManager != null && !tokenManager.hasToken()) navigateToLogin();
            return;
        }
        progressBarQuiz.setVisibility(View.VISIBLE);
        String authToken = "Bearer " + tokenManager.getToken();
        ReportRequest reportRequest = new ReportRequest(questionId, reason);

        apiService.reportQuestion(authToken, reportRequest).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!isDestroyed() && !isFinishing()) {
                    if (progressBarQuiz != null) progressBarQuiz.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(QuizActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = "Rapor gönderilemedi.";
                        if (response.errorBody() != null) {
                            try {
                                errorMessage = response.errorBody().string() + " (Kod: " + response.code() + ")";
                            } catch (Exception e) {Log.e(TAG, "Error body parse failed for report", e);}
                        }
                        Toast.makeText(QuizActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                if (!isDestroyed() && !isFinishing()) {
                    if (progressBarQuiz != null) progressBarQuiz.setVisibility(View.GONE);
                    Log.e(TAG, "Rapor gönderme API çağrısı başarısız", t);
                    Toast.makeText(QuizActivity.this, "Ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void handleShareQuestion() {
        if (currentQuestion != null && currentQuestion.getLinkId() != null) {
            shareQuestionLink(currentQuestion.getLinkId());
        } else {
            Toast.makeText(QuizActivity.this, "Paylaşılacak soru linki bulunamadı.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQuestionLink(String linkId) {
        String baseUrl = Constants.API_BASE_URL;
        if (baseUrl == null) {
            Log.e(TAG, "API_BASE_URL tanımsız!");
            Toast.makeText(this, "Paylaşım için yapılandırma hatası.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String shareableUrl = baseUrl + "api/questions/share/" + linkId;
        String appName;
        try {
            appName = getString(R.string.app_name);
        } catch (Exception e) {
            appName = "Sınavgram"; // Fallback
            Log.e(TAG, "getString(R.string.app_name) hata verdi, fallback kullanılıyor.", e);
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, appName + " Sorusunu İncele");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Bu ilginç soruyu bir kontrol et: " + shareableUrl);

        startActivity(Intent.createChooser(shareIntent, "Soruyu Paylaş..."));
    }

    // --- AdMob Metodları ---
    private void loadInterstitialAd() {
        if (isUserPremium || mInterstitialAd != null) {
            Log.d(TAG, "loadInterstitialAd: Reklam yüklenmiyor (premium veya zaten yüklü/yükleniyor). isUserPremium: " + isUserPremium + ", mInterstitialAd null mu?: " + (mInterstitialAd == null));
            return;
        }
        Log.d(TAG, "loadInterstitialAd: Geçiş reklamı yükleme isteği başlatılıyor.");
        String adUnitId;
        try {
            adUnitId = getString(R.string.admob_interstitial_test_id);
        } catch (Exception e) {
            Log.e(TAG, "Interstitial Ad Unit ID string resource not found, using fallback.", e);
            adUnitId = "ca-app-pub-3940256099942544/1033173712"; // Fallback test ID
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, adUnitId,
                adRequest, new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "Geçiş reklamı BAŞARIYLA YÜKLENDİ.");
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Geçiş reklamı kapatıldı.");
                                if (!isUserPremium) loadInterstitialAd();
                            }
                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                Log.e(TAG, "Geçiş reklamı gösterilemedi: " + adError.getMessage());
                                mInterstitialAd = null;
                                if(!isUserPremium) loadInterstitialAd();
                            }
                            @Override
                            public void onAdShowedFullScreenContent() {
                                Log.d(TAG, "Geçiş reklamı gösterildi.");
                                mInterstitialAd = null;
                            }
                        });
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Geçiş reklamı YÜKLENEMEDİ: " + loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
    }

    private void showInterstitialAdIfNeeded(String triggerType) {
        if (isUserPremium) {
            Log.d(TAG, "showInterstitialAdIfNeeded: Reklam gösterilmiyor (kullanıcı premium). Trigger: " + triggerType);
            return;
        }

        Log.d(TAG, "showInterstitialAdIfNeeded çağrıldı, trigger: " + triggerType + ", mInterstitialAd null mu?: " + (mInterstitialAd == null));
        if (mInterstitialAd != null) {
            Log.d(TAG, "showInterstitialAdIfNeeded: Reklam gösteriliyor.");
            mInterstitialAd.show(QuizActivity.this);
        } else {
            Log.d(TAG, "Geçiş reklamı henüz hazır değil, yeniden yükleniyor.");
            loadInterstitialAd();
        }
    }

    // --- Swipe Metodları ---
    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "SwipeGestureListener: onDown çağrıldı");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null) return false;
            Log.d(TAG, "SwipeGestureListener: onScroll çağrıldı. distanceY: " + distanceY);
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) {
                Log.d(TAG, "SwipeGestureListener: onFling - eventlerden biri null");
                return false;
            }
            Log.d(TAG, "SwipeGestureListener: onFling çağrıldı. e1.Y: " + e1.getY() + ", e2.Y: " + e2.getY() + ", velocityY: " + velocityY);
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                Log.d(TAG, "SwipeGestureListener: onFling - diffY: " + diffY + ", diffX: " + diffX);

                if (Math.abs(diffX) < Math.abs(diffY)) {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) {
                            Log.d(TAG, "SwipeGestureListener: YUKARI kaydırma algılandı!");
                            onSwipeUp();
                            result = true;
                        } else {
                            Log.d(TAG, "SwipeGestureListener: AŞAĞI kaydırma algılandı (işlenmiyor).");
                        }
                    } else {
                        Log.d(TAG, "SwipeGestureListener: Dikey kaydırma eşikleri karşılamadı. abs(diffY):" + Math.abs(diffY) + ", abs(velocityY):" + Math.abs(velocityY));
                    }
                } else {
                    Log.d(TAG, "SwipeGestureListener: Yatay kaydırma öncelikli veya dikey eşikler yetersiz.");
                }
            } catch (Exception exception) {
                Log.e(TAG, "SwipeGestureListener: onFling içinde hata", exception);
            }
            Log.d(TAG, "SwipeGestureListener: onFling sonuç: " + result);
            return result;
        }
    }

    private void onSwipeUp() {
        if (currentQuestion == null) {
            Log.d(TAG, "onSwipeUp: Aktif soru yok, işlem yapılmıyor.");
            return;
        }

        Toast.makeText(this, "Soru geçildi.", Toast.LENGTH_SHORT).show();
        if (!isUserPremium) {
            swipeCounter++;
            Log.d(TAG, "Kaydırma sayısı: " + swipeCounter);
            if (swipeCounter >= AD_TRIGGER_SWIPE_COUNT) {
                Log.d(TAG, "onSwipeUp: " + AD_TRIGGER_SWIPE_COUNT + " kaydırmaya ulaşıldı, reklam tetikleniyor.");
                showInterstitialAdIfNeeded("swipe");
                swipeCounter = 0;
            }
        }
        fetchQuestion();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

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
        if (tokenManager != null) {
            boolean currentPremiumStatusUpdated = tokenManager.hasActivePremium();
            Log.d(TAG, "onResume: Mevcut premium durumu: " + isUserPremium + ", Güncel premium durumu: " + currentPremiumStatusUpdated);
            if (isUserPremium != currentPremiumStatusUpdated) {
                isUserPremium = currentPremiumStatusUpdated;
                Log.d(TAG, "onResume: Premium durumu değişti, UI güncelleniyor. Yeni durum: " + isUserPremium);
                if (mAdViewBanner != null) {
                    mAdViewBanner.setVisibility(isUserPremium ? View.GONE : View.VISIBLE);
                }
                if (!isUserPremium) {
                    loadInterstitialAd();
                } else {
                    // Eğer kullanıcı premium olduysa ve mInterstitialAd yüklüyse,
                    // onu null yaparak bir sonraki (olası olmayan) gösterimi engelleyebiliriz.
                    mInterstitialAd = null;
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