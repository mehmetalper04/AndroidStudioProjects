package com.mehmetalper04.sinavgram.activities; // Kendi paket adınıza göre düzenleyin

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns; // E-posta format kontrolü için
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mehmetalper04.sinavgram.R; // Kendi R dosyanız
import com.mehmetalper04.sinavgram.models.ApiResponse;
import com.mehmetalper04.sinavgram.models.LoginRequest;
import com.mehmetalper04.sinavgram.models.ResendVerificationRequest;
import com.mehmetalper04.sinavgram.network.ApiService;
import com.mehmetalper04.sinavgram.network.RetrofitClient;
import com.mehmetalper04.sinavgram.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText editTextIdentifier;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegisterLink, textViewForgotPasswordLink; // Şifremi unuttum eklenebilir
    private TextView textViewResendVerification; // E-posta tekrar gönder linki
    private ProgressBar progressBarLogin;

    private ApiService apiService;
    private TokenManager tokenManager;
    private String emailForVerificationResend = null; // E-postası doğrulanmamış kullanıcı için

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // activity_login.xml layout'unuz

        editTextIdentifier = findViewById(R.id.editTextLoginEmail);
        editTextPassword = findViewById(R.id.editTextLoginPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegisterLink = findViewById(R.id.textViewRegisterNow);
        // textViewForgotPasswordLink = findViewById(R.id.textViewForgotPassword); // Eğer varsa
        textViewResendVerification = findViewById(R.id.textViewResendVerification);
        progressBarLogin = findViewById(R.id.progressBarLogin);

        apiService = RetrofitClient.getApiService(getApplicationContext());
        tokenManager = TokenManager.getInstance(getApplicationContext());

        if (tokenManager.hasToken()) {
            Log.d(TAG, "Token bulundu, ana uygulamaya yönlendiriliyor.");
            navigateToMainApp();
            return;
        }

        handleDeepLink(getIntent());

        if (getIntent().hasExtra("REGISTRATION_SUCCESS_EMAIL")) {
            String registeredEmail = getIntent().getStringExtra("REGISTRATION_SUCCESS_EMAIL");
            editTextIdentifier.setText(registeredEmail);
            Toast.makeText(this, "Kayıt başarılı! Lütfen giriş yapın.", Toast.LENGTH_SHORT).show();
        }

        buttonLogin.setOnClickListener(v -> loginUser());

        textViewRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        textViewResendVerification.setOnClickListener(v -> {
            String emailToResend = emailForVerificationResend;
            if (TextUtils.isEmpty(emailToResend)) {
                emailToResend = editTextIdentifier.getText().toString().trim();
            }

            if (!TextUtils.isEmpty(emailToResend) && Patterns.EMAIL_ADDRESS.matcher(emailToResend).matches()){
                resendVerificationEmail(emailToResend);
            } else {
                Toast.makeText(LoginActivity.this, "Lütfen geçerli bir e-posta girin.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null && "sorugramapp".equals(data.getScheme()) && "login".equals(data.getHost())) {
            Toast.makeText(this, "E-posta doğrulandı! Lütfen giriş yapın.", Toast.LENGTH_LONG).show();
        }
    }

    private void loginUser() {
        String identifier = editTextIdentifier.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(identifier)) {
            editTextIdentifier.setError("E-posta veya kullanıcı adı gerekli");
            editTextIdentifier.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Şifre gerekli");
            editTextPassword.requestFocus();
            return;
        }

        showLoading(true);
        textViewResendVerification.setVisibility(View.GONE);

        LoginRequest loginRequest = new LoginRequest(identifier, password);
        Call<ApiResponse> call = apiService.loginUser(loginRequest);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.getToken() != null && apiResponse.getUser() != null) {
                        tokenManager.saveUserLoginInfo(apiResponse.getUser(), apiResponse.getToken());
                        Toast.makeText(LoginActivity.this, "Giriş başarılı: " + apiResponse.getUser().getUsername(), Toast.LENGTH_LONG).show();
                        navigateToMainApp();
                    } else if (Boolean.TRUE.equals(apiResponse.isEmailNotVerified())) {
                        emailForVerificationResend = apiResponse.getEmailForVerification();
                        if (TextUtils.isEmpty(emailForVerificationResend) && Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                            emailForVerificationResend = identifier; // Giriş yapılan e-postayı kullan
                        }
                        Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                        textViewResendVerification.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(LoginActivity.this, "Giriş başarısız: " + (apiResponse.getMessage() != null ? apiResponse.getMessage() : "Bilinmeyen hata"), Toast.LENGTH_LONG).show();
                    }
                } else {
                    handleLoginError(response);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Login API çağrısı başarısız", t);
                Toast.makeText(LoginActivity.this, "Ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLoginError(Response<ApiResponse> response){
        String errorMessage = "Giriş hatası";
        if (response.errorBody() != null) {
            try {
                errorMessage = response.errorBody().string(); // Ham hata mesajını göster
            } catch (Exception e) { Log.e(TAG, "Hata mesajı parse edilemedi", e); }
        }
        if (response.code() == 401) {
            errorMessage = "Geçersiz e-posta/kullanıcı adı veya şifre.";
        } else if (response.code() == 403) {
            // Sunucudan gelen mesaj daha açıklayıcı olabilir, onu kullanmaya çalış.
            // Eğer parse edilmiş bir mesaj varsa (ApiResponse içinden):
            // errorMessage = parsedErrorMessage;
            // Yoksa genel bir mesaj:
            // errorMessage = "Hesap doğrulanmamış veya aktif değil.";
            // email_not_verified durumu zaten yukarıda isSuccessful false olmadan yakalanmalıydı.
            // Bu blok genellikle diğer 403 durumları için.
            emailForVerificationResend = editTextIdentifier.getText().toString().trim();
            textViewResendVerification.setVisibility(View.VISIBLE);
        }
        Toast.makeText(LoginActivity.this, errorMessage + " (Kod: " + response.code() + ")", Toast.LENGTH_LONG).show();
    }


    private void resendVerificationEmail(String email) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Doğrulama e-postası göndermek için geçerli bir e-posta adresi gerekli.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);

        ResendVerificationRequest request = new ResendVerificationRequest(email);
        Call<ApiResponse> call = apiService.resendVerificationEmail(request);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(LoginActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    String errorMessage = "E-posta gönderilemedi.";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string() + " (Kod: " + response.code() + ")";
                        } catch (Exception e) { Log.e(TAG, "Hata mesajı parse edilemedi (yeniden gönder)", e); }
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMainApp() {
        Intent intent = new Intent(LoginActivity.this, CourseSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBarLogin.setVisibility(View.VISIBLE);
            buttonLogin.setEnabled(false);
            textViewResendVerification.setEnabled(false);
        } else {
            progressBarLogin.setVisibility(View.GONE);
            buttonLogin.setEnabled(true);
            textViewResendVerification.setEnabled(true);
        }
    }
}