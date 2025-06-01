package com.mehmetalper04.sinavgram.models;

public class LoginRequest {
    private String identifier; // email veya username olabilir
    private String password;

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }
    // Getter'lar (Retrofit için gerekli olmayabilir ama faydalı)
}