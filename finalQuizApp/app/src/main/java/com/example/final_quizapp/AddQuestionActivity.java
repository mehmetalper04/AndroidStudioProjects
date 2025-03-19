package com.example.final_quizapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddQuestionActivity extends AppCompatActivity {

    private static final String TAG = "AddQuestionActivity";
    private Uri selectedImageUri = null;

    private ImageView previewImage;
    private EditText option1Field, option2Field, option3Field, option4Field, option5Field;
    private Spinner correctAnswerSpinner;
    private Button uploadImageButton, submitQuestionButton;

    private StorageReference firebaseStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_question);

        // Initialize UI components
        previewImage = findViewById(R.id.preview_image);
        option1Field = findViewById(R.id.option_1);
        option2Field = findViewById(R.id.option_2);
        option3Field = findViewById(R.id.option_3);
        option4Field = findViewById(R.id.option_4);
        option5Field = findViewById(R.id.option_5);
        correctAnswerSpinner = findViewById(R.id.correct_answer_spinner);
        uploadImageButton = findViewById(R.id.upload_image_button);
        submitQuestionButton = findViewById(R.id.submit_question_button);

        // Initialize Firebase Storage
        firebaseStorage = FirebaseStorage.getInstance().getReference();

        // Set up image picker
        ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        previewImage.setImageURI(selectedImageUri);
                        previewImage.setVisibility(ImageView.VISIBLE);
                    }
                });

        uploadImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePicker.launch(intent);
        });

        // Handle form submission
        submitQuestionButton.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Please upload an image for the question.", Toast.LENGTH_SHORT).show();
                return;
            }

            String option1 = option1Field.getText().toString().trim();
            String option2 = option2Field.getText().toString().trim();
            String option3 = option3Field.getText().toString().trim();
            String option4 = option4Field.getText().toString().trim();
            String option5 = option5Field.getText().toString().trim();
            String correctAnswer = correctAnswerSpinner.getSelectedItem().toString();

            if (option1.isEmpty() || option2.isEmpty() || option3.isEmpty() || option4.isEmpty() || option5.isEmpty()) {
                Toast.makeText(this, "Please fill in all answer options.", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadImageToFirebase(option1, option2, option3, option4, option5, correctAnswer);
        });
    }

    private void uploadImageToFirebase(String option1, String option2, String option3, String option4, String option5, String correctAnswer) {
        String fileName = "questions/" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = firebaseStorage.child(fileName);

        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    submitQuestionToServer(imageUrl, option1, option2, option3, option4, option5, correctAnswer);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed: " + e.getMessage());
                    Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show();
                });
    }

    private void submitQuestionToServer(String imageUrl, String option1, String option2, String option3, String option4, String option5, String correctAnswer) {
        ApiInterface apiInterface = RetrofitClient.getClient().create(ApiInterface.class);

        HashMap<String, String> questionData = new HashMap<>();
        questionData.put("image_url", imageUrl);
        questionData.put("option_1", option1);
        questionData.put("option_2", option2);
        questionData.put("option_3", option3);
        questionData.put("option_4", option4);
        questionData.put("option_5", option5);
        questionData.put("correct_answer", correctAnswer);

        apiInterface.addQuestion(questionData).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddQuestionActivity.this, "Question added successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                } else {
                    Toast.makeText(AddQuestionActivity.this, "Failed to add question.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to submit question: " + t.getMessage());
                Toast.makeText(AddQuestionActivity.this, "Error occurred while adding question.", Toast.LENGTH_SHORT).show();
            }
        });
        // AddQuestionScreen.java
        Button exit = findViewById(R.id.exit_button);

        exit.setOnClickListener(v -> {
            finish();
        });
    }
}
