package com.asf.callmonitoring.data.remote

import com.asf.callmonitoring.data.remote.dto.LlamadaRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LlamadasApiService {

    @POST("api/llamadas")
    suspend fun registrarLlamada(
        @Body request: LlamadaRequestDto
    ): Response<Unit>
}