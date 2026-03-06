package com.example.purchaseregister.api

import com.example.purchaseregister.api.request.*
import com.example.purchaseregister.api.responses.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SunatApiService {

    @POST("api/auth/sign-in/email")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/users/me/fcm-token")
    suspend fun sendFcmToken(
        @Header("Authorization") authorization: String?,
        @Body request: FcmTokenRequest
    ): Response<FcmTokenResponse>

    @POST("api/auth/sign-up/email")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("api/auth/forget-password")
    suspend fun requestPasswordReset(
        @Body request: ForgotPasswordRequest
    ): Response<Unit>

    @POST("api/users/me/sunat-credentials")
    suspend fun saveSunatCredentials(
        @Body request: SaveSunatCredentialsRequest
    ): Response<SaveSunatCredentialsResponse>

    @PATCH("api/users/me/sunat-credentials")
    suspend fun updateSunatCredentials(
        @Body request: UpdateSunatCredentialsRequest
    ): Response<Unit>

    @POST("api/auth/sign-out")
    suspend fun signOut(
        @Header("Authorization") authorization: String?
    ): Response<Unit>

    @GET("api/invoices")
    suspend fun getInvoices(
        @Query("startDate") periodStart: String,
        @Query("endDate") periodEnd: String,
    ): List<SunatResponse>

    @GET("api/invoices/{id}")
    suspend fun getInvoiceDetails(
        @Path("id") invoiceId: Int
    ): Response<InvoiceDetailsResponse>

    @GET("api/export-invoices/{invoiceId}/{fileId}")
    suspend fun downloadSunatDocument(
        @Path("invoiceId") invoiceId: Int,
        @Path("fileId") fileId: Int
    ): Response<ResponseBody>

    @GET("api/export-invoices")
    suspend fun downloadInvoicesCsv(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("format") format: String = "json"
    ): Response<ResponseBody>

    @POST("factura/procesarFactura")
    @Headers("Content-Type: application/json")
    suspend fun registerInvoicesInDB(
        @Body request: RegisterInvoicesRequest
    ): RegisterInvoicesResponse
}