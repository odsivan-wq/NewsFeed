package com.example.newsfeed.data

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {
    @GET("v2/everything")
    suspend fun everything(
        @Query("q") query: String,
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): NewsResponseDto
}