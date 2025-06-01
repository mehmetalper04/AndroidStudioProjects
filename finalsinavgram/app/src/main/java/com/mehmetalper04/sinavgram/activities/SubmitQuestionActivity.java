package com.mehmetalper04.sinavgram.activities;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.mehmetalper04.sinavgram.R;
import com.mehmetalper04.sinavgram.models.ApiResponse;
import com.mehmetalper04.sinavgram.models.Course;
import com.mehmetalper04.sinavgram.network.ApiService;
import com.mehmetalper04.sinavgram.network.RetrofitClient;
import com.mehmetalper04.sinavgram.utils.TokenManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubmitQuestionActivity extends AppCompatActivity {

    private static final String TAG = "SubmitQuestion";

    private Spinner spinnerSelectCourseSubmit, spinnerCorrectAnswerSubmit;
    private EditText editTextQuestionTextSubmit, editTextOptionASubmit, editTextOptionBSubmit,
            editTextOptionCSubmit, editTextOptionDSubmit, editTextOptionESubmit;
    private CheckBox checkboxIsPremiumOnly;
    private Button buttonAddPhotoSubmit, buttonSubmitQuestionActual;
    private ImageView imageViewSelectedPhotoPreviewSubmit;
    private ProgressBar progressBarSubmitQuestion;

    private ApiService apiService;
    private TokenManager tokenManager;

    private List<Course> courseList = new ArrayList<>();
    private ArrayAdapter<String> courseSpinnerAdapter;
    private ArrayList<String> courseNames = new ArrayList<>();

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_question);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Yeni Soru Gönder");
        }

        initViews();

        apiService = RetrofitClient.getApiService(getApplicationContext());
        tokenManager = TokenManager.getInstance(getApplicationContext());

        if (!tokenManager.hasToken()) {
            Toast.makeText(this, "Lütfen önce giriş yapın.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        setupSpinners();
        loadCourses();

        // Resim seçiciyi başlat
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imageViewSelectedPhotoPreviewSubmit.setImageURI(selectedImageUri);
                        imageViewSelectedPhotoPreviewSubmit.setVisibility(View.VISIBLE);
                    }
                });

        buttonAddPhotoSubmit.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        buttonSubmitQuestionActual.setOnClickListener(v -> submitQuestion());
    }

    private void initViews() {
        spinnerSelectCourseSubmit = findViewById(R.id.spinnerSelectCourse);
        spinnerCorrectAnswerSubmit = findViewById(R.id.spinnerCorrectAnswer);
        editTextQuestionTextSubmit = findViewById(R.id.editTextQuestionText);
        editTextOptionASubmit = findViewById(R.id.editTextOptionA);
        editTextOptionBSubmit = findViewById(R.id.editTextOptionB);
        editTextOptionCSubmit = findViewById(R.id.editTextOptionC);
        editTextOptionDSubmit = findViewById(R.id.editTextOptionD);
        editTextOptionESubmit = findViewById(R.id.editTextOptionE);
        checkboxIsPremiumOnly = findViewById(R.id.checkboxIsPremiumOnly);
        buttonAddPhotoSubmit = findViewById(R.id.buttonAddPhoto);
        buttonSubmitQuestionActual = findViewById(R.id.buttonSubmitQuestion);
        imageViewSelectedPhotoPreviewSubmit = findViewById(R.id.imageViewSelectedPhotoPreviewSubmit);
       progressBarSubmitQuestion = findViewById(R.id.progressBarSubmitQuestion);
    }

    private void setupSpinners() {
        // Dersler için Spinner Adapter
        courseSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNames);
        courseSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelectCourseSubmit.setAdapter(courseSpinnerAdapter);

        // Doğru cevap seçenekleri için Spinner Adapter
        ArrayAdapter<String> correctAnswerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"A", "B", "C", "D", "E"});
        correctAnswerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCorrectAnswerSubmit.setAdapter(correctAnswerAdapter);
    }

    private void loadCourses() {
        progressBarSubmitQuestion.setVisibility(View.VISIBLE);
        String authToken = "Bearer " + tokenManager.getToken();

        apiService.getCourses(authToken).enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                progressBarSubmitQuestion.setVisibility(View.GONE); // Kurslar yüklendikten sonra gizle
                if (response.isSuccessful() && response.body() != null) {
                    courseList.clear();
                    courseList.addAll(response.body());
                    courseNames.clear();
                    for (Course course : courseList) {
                        courseNames.add(course.getName());
                    }
                    courseSpinnerAdapter.notifyDataSetChanged();
                    if (courseNames.isEmpty()) {
                        Toast.makeText(SubmitQuestionActivity.this, "Ders bulunamadı. Lütfen daha sonra tekrar deneyin.", Toast.LENGTH_LONG).show();
                        buttonSubmitQuestionActual.setEnabled(false); // Göndermeyi engelle
                    }
                } else {
                    Toast.makeText(SubmitQuestionActivity.this, "Dersler yüklenemedi. Kod: " + response.code(), Toast.LENGTH_LONG).show();
                    if (response.code() == 401 || response.code() == 403) {
                        tokenManager.clearToken(); navigateToLogin();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                progressBarSubmitQuestion.setVisibility(View.GONE);
                Toast.makeText(SubmitQuestionActivity.this, "Dersler yüklenirken ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitQuestion() {
        if (spinnerSelectCourseSubmit.getSelectedItemPosition() < 0 || courseList.isEmpty()) {
            Toast.makeText(this, "Lütfen bir ders seçin.", Toast.LENGTH_SHORT).show();
            return;
        }
        int selectedCoursePosition = spinnerSelectCourseSubmit.getSelectedItemPosition();
        // Pozisyonun geçerli olduğundan emin ol
        if (selectedCoursePosition >= courseList.size()) {
            Toast.makeText(this, "Geçersiz ders seçimi.", Toast.LENGTH_SHORT).show();
            return;
        }
        Course selectedCourse = courseList.get(selectedCoursePosition);


        String questionText = editTextQuestionTextSubmit.getText().toString().trim();
        String optA = editTextOptionASubmit.getText().toString().trim();
        String optB = editTextOptionBSubmit.getText().toString().trim();
        String optC = editTextOptionCSubmit.getText().toString().trim();
        String optD = editTextOptionDSubmit.getText().toString().trim();
        String optE = editTextOptionESubmit.getText().toString().trim();
        String correctOpt = spinnerCorrectAnswerSubmit.getSelectedItem().toString();
        boolean isPremium = checkboxIsPremiumOnly.isChecked();

        if (TextUtils.isEmpty(questionText) || TextUtils.isEmpty(optA) || TextUtils.isEmpty(optB) ||
                TextUtils.isEmpty(optC) || TextUtils.isEmpty(optD) || TextUtils.isEmpty(optE) ||
                TextUtils.isEmpty(correctOpt)) {
            Toast.makeText(this, "Lütfen tüm soru alanlarını doldurun.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarSubmitQuestion.setVisibility(View.VISIBLE);
        buttonSubmitQuestionActual.setEnabled(false);

        String authToken = "Bearer " + tokenManager.getToken();

        RequestBody courseIdRB = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(selectedCourse.getId()));
        RequestBody textRB = RequestBody.create(MediaType.parse("text/plain"), questionText);
        RequestBody optARB = RequestBody.create(MediaType.parse("text/plain"), optA);
        RequestBody optBRB = RequestBody.create(MediaType.parse("text/plain"), optB);
        RequestBody optCRB = RequestBody.create(MediaType.parse("text/plain"), optC);
        RequestBody optDRB = RequestBody.create(MediaType.parse("text/plain"), optD);
        RequestBody optERB = RequestBody.create(MediaType.parse("text/plain"), optE);
        RequestBody correctOptRB = RequestBody.create(MediaType.parse("text/plain"), correctOpt);
        RequestBody isPremiumRB = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isPremium));

        MultipartBody.Part photoPart = null;
        if (selectedImageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                String fileName = getFileName(selectedImageUri); // Helper method to get filename
                String mimeType = getMimeType(selectedImageUri); // Helper method to get mimetype

                // InputStream'den byte dizisi oluştur
                byte[] fileBytes = new byte[inputStream.available()];
                inputStream.read(fileBytes);
                inputStream.close();

                RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType != null ? mimeType : "image/*"), fileBytes);
                photoPart = MultipartBody.Part.createFormData("photo", fileName, requestFile);

            } catch (IOException e) {
                Log.e(TAG, "Resim dosyası işlenirken hata: ", e);
                Toast.makeText(this, "Resim yüklenirken bir hata oluştu.", Toast.LENGTH_SHORT).show();
                progressBarSubmitQuestion.setVisibility(View.GONE);
                buttonSubmitQuestionActual.setEnabled(true);
                return;
            }
        }

        Call<ApiResponse> call = apiService.submitNewQuestion(authToken, courseIdRB, textRB,
                optARB, optBRB, optCRB, optDRB, optERB, correctOptRB, isPremiumRB, photoPart);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressBarSubmitQuestion.setVisibility(View.GONE);
                buttonSubmitQuestionActual.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(SubmitQuestionActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    // Formu temizle veya aktiviteyi kapat
                    finish(); // Veya formu sıfırla
                } else {
                    String errorMessage = "Soru gönderilemedi.";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string() + " (Kod: " + response.code() + ")";
                        } catch (Exception e) {Log.e(TAG, "Error body parse failed", e);}
                    }
                    Toast.makeText(SubmitQuestionActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    if (response.code() == 401 || response.code() == 403) {
                        tokenManager.clearToken(); navigateToLogin();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressBarSubmitQuestion.setVisibility(View.GONE);
                buttonSubmitQuestionActual.setEnabled(true);
                Log.e(TAG, "Soru gönderme API çağrısı başarısız", t);
                Toast.makeText(SubmitQuestionActivity.this, "Ağ hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Uri'den dosya adını almak için yardımcı metot
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        // Güvenlik için dosya adını temizleyebilir veya benzersiz bir ad oluşturabilirsiniz
        return "upload_" + System.currentTimeMillis() + "_" + result;
    }

    // Uri'den MIME türünü almak için yardımcı metot
    private String getMimeType(Uri uri) {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }


    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
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