package com.example.final_quizapp;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiInterface {

    @GET("get_random_question.php")
    Call<Question> getRandomQuestion(@Query("userId") String userId);

    @POST("submit_answer.php")
    Call<Void> submitAnswer(@Body HashMap<String, String> answerData);

    @POST("add_question.php")
    Call<Void> addQuestion(@Body HashMap<String, String> questionData);
}
