package com.mehmetalper04.sinavgram.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class Question {
    @SerializedName("id")
    private int id;

    @SerializedName("text")
    private String text;

    @SerializedName("options")
    private Map<String, String> options; // {"A": "Cevap A", "B": "Cevap B", ...}

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("link_id")
    private String linkId;

    @SerializedName("course")
    private String courseName;

    @SerializedName("course_id")
    private int courseId;

    @SerializedName("is_premium_only")
    private boolean isPremiumOnly;

    // Getter ve Setter metodları
    public int getId() { return id; }
    public String getText() { return text; }
    public Map<String, String> getOptions() { return options; }
    public String getImageUrl() { return imageUrl; }
    public String getLinkId() { return linkId; }
    public String getCourseName() { return courseName; }
    public int getCourseId() { return courseId; }
    public boolean isPremiumOnly() { return isPremiumOnly; }
}
