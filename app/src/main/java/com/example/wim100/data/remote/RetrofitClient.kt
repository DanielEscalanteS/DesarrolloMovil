package com.example.wim100.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val WIM_BASE_URL = "http://10.0.2.2:8080/api/"
    private const val RANDOM_USER_BASE_URL = "https://randomuser.me/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val wimRetrofit = Retrofit.Builder()
        .baseUrl(WIM_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val randomUserRetrofit = Retrofit.Builder()
        .baseUrl(RANDOM_USER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val wimApi: WimApi by lazy {
        wimRetrofit.create(WimApi::class.java)
    }

    val randomUserApi: RandomUserApi by lazy {
        randomUserRetrofit.create(RandomUserApi::class.java)
    }
}
