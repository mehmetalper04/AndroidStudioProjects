package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;

public class AnswerSubmitRequest {
    @SerializedName("question_id")
    private int questionId;

    @SerializedName("selected_option")
    private String selectedOption; // "A", "B", "C", "D", "E", veya "F" (boş için)

    public AnswerSubmitRequest(int questionId, String selectedOption) {
        this.questionId = questionId;
        this.selectedOption = selectedOption;
    }
}