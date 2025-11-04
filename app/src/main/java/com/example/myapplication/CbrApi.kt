package com.example.myapplication

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface CbrApi {
    @GET
    fun getGoldRate(@Url url: String): Call<GoldRateResponse>
}