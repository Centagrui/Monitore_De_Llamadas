package com.asf.callmonitoring.data.api

import com.asf.callmonitoring.data.model.Usuario
import com.asf.callmonitoring.data.remote.dto.CallLogDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
interface ApiService {

    @GET("api/usuarios")
    suspend fun getUsuarios(): Response<List<Usuario>>

    @POST("api/llamadas")
    suspend fun postCall(@Body callLog: CallLogDto): Response<Map<String, Any>>
}