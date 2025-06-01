package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class UserStats {
    @SerializedName("username")
    private String username;

    @SerializedName("score")
    private int score;

    @SerializedName("total_answered_questions")
    private int totalAnsweredQuestions;

    @SerializedName("correctly_answered_questions")
    private int correctlyAnsweredQuestions;

    @SerializedName("incorrectly_answered_questions")
    private int incorrectlyAnsweredQuestions;

    @SerializedName("blank_answered_questions")
    private int blankAnsweredQuestions;

    @SerializedName("accuracy_rate_excluding_blanks")
    private double accuracyRateExcludingBlanks;

    @SerializedName("rates")
    private Map<String, Double> rates; // {"true": 66.7, "false": 20.0, ...}

    @SerializedName("email_verified")
    private boolean emailVerified;

    @SerializedName("is_premium")
    private boolean isPremium;

    @SerializedName("has_active_premium")
    private boolean hasActivePremium;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("premium_expiration_date")
    private String premiumExpirationDate;

    // Getter metodları
    public String getUsername() { return username; }
    public int getScore() { return score; }
    public int getTotalAnsweredQuestions() { return totalAnsweredQuestions; }
    public int getCorrectlyAnsweredQuestions() { return correctlyAnsweredQuestions; }
    public int getIncorrectlyAnsweredQuestions() { return incorrectlyAnsweredQuestions; }
    public int getBlankAnsweredQuestions() { return blankAnsweredQuestions; }
    public double getAccuracyRateExcludingBlanks() { return accuracyRateExcludingBlanks; }
    public Map<String, Double> getRates() { return rates; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isPremium() { return isPremium; }
    public boolean hasActivePremium() { return hasActivePremium; }
    public boolean isActive() { return isActive; }
    public String getPremiumExpirationDate() { return premiumExpirationDate; }
}