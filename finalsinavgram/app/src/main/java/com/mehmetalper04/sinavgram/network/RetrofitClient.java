package com.mehmetalper04.sinavgram.network;

import android.content.Context;
import com.mehmetalper04.sinavgram.utils.Constants;
import com.mehmetalper04.sinavgram.utils.TokenManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    // Bu interceptor, her isteğe Authorization header ekler (eğer token varsa)
    // Ancak ApiService interface'inde her metoda @Header("Authorization") eklemek daha açık olabilir.
    // Şimdilik header'ları interface'de tanımladık.
    // Eğer otomatik eklemek isterseniz:
    /*
    private static OkHttpClient getOkHttpClientWithAuth(Context context) {
        TokenManager tokenManager = TokenManager.getInstance(context);
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder();
                        if (tokenManager.hasToken()) {
                            requestBuilder.header("Authorization", "Bearer " + tokenManager.getToken());
                        }
                        requestBuilder.method(original.method(), original.body());
                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    }
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    */

    private static OkHttpClient getOkHttpClientBasic() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Geliştirme için log seviyesi

        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }


    public static synchronized ApiService getApiService(Context context) {
        if (apiService == null) {
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .baseUrl(Constants.API_BASE_URL)
                        .client(getOkHttpClientBasic()) // Otomatik token ekleme için getOkHttpClientWithAuth(context)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}