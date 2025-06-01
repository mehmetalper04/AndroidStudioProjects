package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;

public class AnswerSubmitResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("is_correct")
    private Boolean isCorrect; // Null olabilir (boş bırakıldığında)

    @SerializedName("correct_option")
    private String correctOption;

    @SerializedName("new_score")
    private int newScore;

    @SerializedName("show_ad_trigger")
    private boolean showAdTrigger;

    // Getter metodları
    public String getMessage() { return message; }
    public Boolean getCorrect() { return isCorrect; }
    public String getCorrectOption() { return correctOption; }
    public int getNewScore() { return newScore; }
    public boolean isShowAdTrigger() { return showAdTrigger; }
}