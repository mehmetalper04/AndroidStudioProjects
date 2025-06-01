package com.mehmetalper04.sinavgram.network;


import com.mehmetalper04.sinavgram.models.*; // Tüm modelleri import et

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // --- Auth Endpoints ---
    @POST("api/register")
    Call<ApiResponse> registerUser(@Body RegisterRequest registerRequest);

    @POST("api/login")
    Call<ApiResponse> loginUser(@Body LoginRequest loginRequest); // ApiResponse User ve Token içerir

    @POST("api/resend-verification-email")
    Call<ApiResponse> resendVerificationEmail(@Body ResendVerificationRequest request);

    @POST("api/resend-verification-email") // Token ile gönderim için (body boş olabilir)
    Call<ApiResponse> resendVerificationEmailWithToken(@Header("Authorization") String authToken);


    // --- Authenticated User Endpoints ---
    @GET("api/courses")
    Call<List<Course>> getCourses(@Header("Authorization") String authToken);

    @GET("api/questions/{course_id}")
    Call<Question> getQuestionForCourse(@Header("Authorization") String authToken, @Path("course_id") int courseId);

    @POST("api/submit-answer")
    Call<AnswerSubmitResponse> submitAnswer(@Header("Authorization") String authToken, @Body AnswerSubmitRequest answerSubmitRequest);

    @POST("api/questions/report")
    Call<ApiResponse> reportQuestion(@Header("Authorization") String authToken, @Body ReportRequest reportRequest);

    @GET("api/user/stats")
    Call<UserStats> getUserStats(@Header("Authorization") String authToken);

    @Multipart // Soru gönderirken fotoğraf olabileceği için Multipart
    @POST("api/questions/submit")
    Call<ApiResponse> submitNewQuestion(
            @Header("Authorization") String authToken,
            @Part("course_id") RequestBody courseId,
            @Part("text") RequestBody text,
            @Part("option_a") RequestBody optionA,
            @Part("option_b") RequestBody optionB,
            @Part("option_c") RequestBody optionC,
            @Part("option_d") RequestBody optionD,
            @Part("option_e") RequestBody optionE,
            @Part("correct_option") RequestBody correctOption,
            @Part("is_premium_only") RequestBody isPremiumOnly, // 'true' veya 'false' string olarak
            @Part MultipartBody.Part photo // İsteğe bağlı fotoğraf
    );

    // Fotoğrafsız soru gönderme (eğer backend sadece form-data kabul ediyorsa bu da Multipart olmalı)
    // Backend'iniz request.form kullandığı için bu da Multipart olmalı.
    // Yukarıdaki submitNewQuestion photo parametresi null gönderilerek kullanılabilir.

    @POST("api/user/subscribe-premium")
    Call<PremiumSubscriptionResponse> subscribePremium(@Header("Authorization") String authToken, @Body PremiumSubscriptionRequest subscriptionRequest);

    // --- Public Endpoints (Token gerektirmeyen) ---
    @GET("api/questions/share/{link_id}") // API yanıtı için
    Call<Question> getSharedQuestion(@Path("link_id") String linkId);

    // Not: /verify-email/<token> rotası genellikle tarayıcıda açılır,
    // mobil uygulama doğrudan bu endpoint'i çağırmaz, ancak kullanıcıyı bilgilendirebilir.
}