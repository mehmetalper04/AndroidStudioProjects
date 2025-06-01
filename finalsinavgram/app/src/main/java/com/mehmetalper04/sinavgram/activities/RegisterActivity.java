package com.mehmetalper04.sinavgram.activities;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mehmetalper04.sinavgram.R;
import com.mehmetalper04.sinavgram.models.ApiResponse;
import com.mehmetalper04.sinavgram.models.RegisterRequest;
import com.mehmetalper04.sinavgram.network.ApiService;
import com.mehmetalper04.sinavgram.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewLoginLink;
    private ProgressBar progressBarRegister;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // activity_register.xml layout'unuz

        editTextUsername = findViewById(R.id.editTextRegisterUsername);
        editTextEmail = findViewById(R.id.editTextRegisterEmail);
        editTextPassword = findViewById(R.id.editTextRegisterPassword);
        editTextConfirmPassword = findViewById(R.id.editTextRegisterConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLoginLink = findViewById(R.id.textViewLoginNow); // XML'deki ID'ye göre güncelleyin
        //progressBarRegister = findViewById(R.id.progressBarRegister); // XML'inize ekleyin

        apiService = RetrofitClient.getApiService(getApplicationContext());

        buttonRegister.setOnClickListener(v -> registerUser());

        textViewLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError("Kullanıcı adı gerekli");
            editTextUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("E-posta gerekli");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Geçerli bir e-posta girin");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Şifre gerekli");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Şifre en az 6 karakter olmalı");
            editTextPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPassword.setError("Şifreyi onaylayın");
            editTextConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Şifreler eşleşmiyor");
            editTextConfirmPassword.requestFocus();
            return;
        }

        showLoading(true);

        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        Call<ApiResponse> call = apiService.registerUser(registerRequest);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    // Kullanıcıyı LoginActivity'e yönlendir veya bir "doğrulama e-postası gönderildi" mesajı göster
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.putExtra("REGISTRATION_SUCCESS_EMAIL", email); // Login sayfasında göstermek için
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Kayıt başarısız oldu.";
                    if (response.errorBody() != null) {
                        try {
                            // Hata mesajını daha spesifik almak için JSON parse edilebilir
                            // ApiResponse errorResponse = new Gson().fromJson(response.errorBody().charStream(), ApiResponse.class);
                            // errorMessage = errorResponse.getMessage();
                            errorMessage = response.errorBody().string() + " (Kod: " + response.code() + ")";
                        } catch (Exception e) {
                            // Log error
                        }
                    } else if (response.code() == 409) { // Conflict
                        errorMessage = "Bu kullanıcı adı veya e-posta zaten alınmış.";
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(RegisterActivity.this, "Ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBarRegister.setVisibility(View.VISIBLE);
            buttonRegister.setEnabled(false);
        } else {
            progressBarRegister.setVisibility(View.GONE);
            buttonRegister.setEnabled(true);
        }
    }
}