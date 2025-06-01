package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private int id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("is_admin")
    private boolean isAdmin;

    @SerializedName("email_verified")
    private boolean emailVerified;

    @SerializedName("score")
    private int score;

    @SerializedName("is_premium")
    private boolean isPremium;

    @SerializedName("has_active_premium")
    private boolean hasActivePremium;

    @SerializedName("is_active") // Eklendi
    private boolean isActive;

    @SerializedName("premium_expiration_date")
    private String premiumExpirationDate; // ISO formatında string olarak alalım

    // Getter ve Setter metodları
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return isAdmin; }
    public boolean isEmailVerified() { return emailVerified; }
    public int getScore() { return score; }
    public boolean isPremium() { return isPremium; }
    public boolean hasActivePremium() { return hasActivePremium; }
    public boolean isActive() { return isActive; }
    public String getPremiumExpirationDate() { return premiumExpirationDate; }
}