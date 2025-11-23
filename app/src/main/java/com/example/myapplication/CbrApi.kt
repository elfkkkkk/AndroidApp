package com.example.myapplication

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CbrApi {
    @GET("scripts/xml_metall.asp")
    fun getGoldRate(
        @Query("date_req1") dateFrom: String,
        @Query("date_req2") dateTo: String
    ): Call<MetallData>
}