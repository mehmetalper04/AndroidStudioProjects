package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;

public class PremiumSubscriptionRequest {
    @SerializedName("plan_type")
    private String planType; // "monthly" veya "yearly"

    public PremiumSubscriptionRequest(String planType) {
        this.planType = planType;
    }
}