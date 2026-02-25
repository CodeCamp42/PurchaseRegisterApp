package com.example.purchaseregister.api

import com.example.purchaseregister.api.request.*
import com.example.purchaseregister.api.responses.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.ResponseBody

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

    @POST("api/auth/sign-out")
    suspend fun signOut(
        @Header("Authorization") authorization: String?
    ): Response<Unit>

    @GET("api/invoices")
    suspend fun getInvoices(
        @Query("startDate") periodStart: String,
        @Query("endDate") periodEnd: String,
        @Query("ruc") ruc: String,
        @Query("usuario") solUsername: String,
        @Query("claveSol") solPassword: String,
        @Query("clientId") clientId: String,
        @Query("clientSecret") clientSecret: String
    ): List<SunatResponse>

    @PUT("factura/scraping-completado/{numeroComprobante}")
    @Headers("Content-Type: application/json")
    suspend fun markScrapingCompleted(
        @Path("numeroComprobante") documentNumber: String,
        @Body request: ScrapingCompletedRequest? = null
    ): ScrapingCompletedResponse

    @POST("factura/guardar-productos/{numeroComprobante}")
    @Headers("Content-Type: application/json")
    suspend fun saveInvoiceProducts(
        @Path("numeroComprobante") documentNumber: String,
        @Body request: SaveProductsRequest
    ): SaveProductsResponse

    @POST("factura/procesarFactura")
    @Headers("Content-Type: application/json")
    suspend fun registerInvoicesInDB(
        @Body request: RegisterInvoicesRequest
    ): RegisterInvoicesResponse

    @GET("factura/{numeroComprobante}")
    suspend fun checkRegisteredInvoice(
        @Path("numeroComprobante") documentNumber: String
    ): RegisteredInvoiceResponse

    @POST("sunat/descargar-xml")
    suspend fun downloadXmlWithQueue(
        @Body request: InvoiceDetailRequest
    ): QueuedResponse

    @GET("sunat/job/{jobId}")
    suspend fun getJobStatus(
        @Path("jobId") jobId: String
    ): JobStatusResponse

    @GET("factura/descargar/{numeroComprobante}/{tipo}")
    @Headers("Content-Type: application/octet-stream")
    suspend fun downloadFile(
        @Path("numeroComprobante") documentNumber: String,
        @Path("tipo") type: String
    ): ResponseBody

    @GET("factura/ui/usuario/{usuarioId}/completo")
    suspend fun getCompleteUserInvoices(
        @Path("usuarioId") userId: String
    ): InvoicesUIResponse
}