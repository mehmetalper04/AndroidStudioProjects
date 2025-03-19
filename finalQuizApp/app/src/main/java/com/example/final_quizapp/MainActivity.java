package com.example.final_quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123; // Request code for Google Sign-In
    private static final String TAG = "MainActivity";

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private Button googleSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Web client ID from google-services.json
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check if the user is already signed in
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToNextActivity();
        }

        // Set up the Google Sign-In Button
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                        .getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign-in failed", e);
                Toast.makeText(this, "Google Sign-In failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
        Button quest = findViewById(R.id.addquestion);
        quest.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddQuestionActivity.class);
            startActivity(intent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Sign in with Google: success");
                        navigateToNextActivity();
                    } else {
                        Log.w(TAG, "Sign in with Google: failure", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(MainActivity.this, QuizActivity.class);
        startActivity(intent);
        finish(); // Close MainActivity
    }



}