    package com.namgyun.tamakitchen.network;
    
    import com.namgyun.tamakitchen.BuildConfig;
    import com.google.gson.Gson;
    import com.google.gson.GsonBuilder;
    
    import java.util.concurrent.TimeUnit;
    
    import okhttp3.Interceptor;
    import okhttp3.OkHttpClient;
    import okhttp3.Request;
    import okhttp3.logging.HttpLoggingInterceptor;
    import retrofit2.Retrofit;
    import retrofit2.converter.gson.GsonConverterFactory;
    
    public class FridgeApi {
    
        /*
         * 🔥 이제 IP 하드코딩 안 함
         * build.gradle.kts의 BASE_URL 사용
         */
        public static final String BASE_URL = BuildConfig.BASE_URL;
    
        private static Retrofit retrofit = null;
    
        public static Retrofit getClient() {
    
            if (retrofit == null) {
    
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
    
                Interceptor headerInterceptor = chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json; charset=utf-8")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                };
    
                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(headerInterceptor)
                        .addInterceptor(logging)
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build();
    
                Gson gson = new GsonBuilder()
                        .setLenient()
                        .create();
    
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build();
            }
    
            return retrofit;
        }
    }