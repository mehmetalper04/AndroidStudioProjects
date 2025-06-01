package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;

public class ReportRequest {
    @SerializedName("question_id")
    private int questionId;

    @SerializedName("reason")
    private String reason;

    public ReportRequest(int questionId, String reason) {
        this.questionId = questionId;
        this.reason = reason;
    }
}
