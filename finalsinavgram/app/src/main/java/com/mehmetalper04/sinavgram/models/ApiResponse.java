package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("token") // Login için
    private String token;

    @SerializedName("user") // Login için
    private User user;

    @SerializedName("email_not_verified") // Login için
    private Boolean emailNotVerified;

    @SerializedName("email") // Login'de email_not_verified ise
    private String emailForVerification;


    // Getter metodları
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public User getUser() { return user; }
    public Boolean isEmailNotVerified() { return emailNotVerified; }
    public String getEmailForVerification() { return emailForVerification; }
}