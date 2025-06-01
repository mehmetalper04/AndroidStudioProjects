package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;

public class PremiumSubscriptionResponse { // ApiResponse ile birleştirilebilir.
    @SerializedName("message")
    private String message;

    @SerializedName("is_premium")
    private boolean isPremium;

    @SerializedName("has_active_premium")
    private boolean hasActivePremium;

    @SerializedName("premium_expiration_date")
    private String premiumExpirationDate;

    @SerializedName("simulated_transaction_id")
    private String simulatedTransactionId;

    // Getter metodları
    public String getMessage() { return message; }
    public boolean isPremium() { return isPremium; }
    public boolean hasActivePremium() { return hasActivePremium; }
    public String getPremiumExpirationDate() { return premiumExpirationDate; }
    public String getSimulatedTransactionId() { return simulatedTransactionId; }
}