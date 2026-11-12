package com.example.newsfeed.data

import com.example.newsfeed.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("X-Api-Key", BuildConfig.NEWS_API_KEY)
                .build()
            chain.proceed(request)
        }
        .build()

    val api: NewsApi = Retrofit.Builder()
        .baseUrl("https://newsapi.org/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NewsApi::class.java)
}