package com.example.final_quizapp;

import android.widget.Button;

public class Question {
    private String imageUrl;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String option5;
    private String correctAnswer;

    public Question(String imageUrl, String option1, String option2, String option3, String option4, String option5, String correctAnswer) {
        this.imageUrl = imageUrl;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.option5 = option5;
        this.correctAnswer = correctAnswer;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getOption1() {
        return option1;
    }

    public String getOption2() {
        return option2;
    }

    public String getOption3() {
        return option3;
    }

    public String getOption4() {
        return option4;
    }

    public String getOption5() {
        return option5;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }


}