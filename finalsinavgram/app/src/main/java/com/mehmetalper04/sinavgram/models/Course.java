package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;

public class Course {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    // Getter ve Setter metodları
    public int getId() { return id; }
    public String getName() { return name; }
}