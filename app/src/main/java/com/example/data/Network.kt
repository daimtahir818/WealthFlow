package com.example.data

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class AuthRequest(val email: String, val passwordHash: String)
data class AuthResponse(val token: String, val userEmail: String, val userId: Int)

data class SyncRequest(
    val userId: Int,
    val transactions: List<TransactionPayload>
)

data class TransactionPayload(
    val id: Int,
    val amount: Double,
    val category: String,
    val date: Long,
    val paymentMethod: String,
    val description: String,
    val type: String
)

data class SyncResponse(
    val success: Boolean,
    val syncedIds: List<Int>,
    val message: String
)

interface WealthFlowApiService {
    @POST("auth/signup")
    suspend fun signup(@Body request: AuthRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("ledger/sync")
    suspend fun syncLedger(
        @Header("Authorization") token: String,
        @Body request: SyncRequest
    ): SyncResponse
}

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // 10.0.2.2 points to localhost from Android Emulator
    private const val BASE_URL = "http://10.0.2.2:3000/api/"

    val service: WealthFlowApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WealthFlowApiService::class.java)
    }
}
