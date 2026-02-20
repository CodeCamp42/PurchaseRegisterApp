package com.example.purchaseregister.api

import com.example.purchaseregister.api.request.*
import com.example.purchaseregister.api.responses.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.ResponseBody

interface SunatApiService {
    @GET("sunat/facturas")
    suspend fun getInvoices(
        @Query("periodoInicio") periodStart: String,
        @Query("periodoFin") periodEnd: String,
        @Query("ruc") ruc: String,
        @Query("usuario") user: String,
        @Query("claveSol") solPassword: String,
        @Query("clientId") clientId: String,
        @Query("clientSecret") clientSecret: String
    ): SunatResponse

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

    @POST("factura/registrar-desde-sunat")
    @Headers("Content-Type: application/json")
    suspend fun registerInvoiceFromSunat(
        @Body request: RegisterInvoiceFromSunatRequest
    ): RegisterInvoiceFromSunatResponse

    @GET("factura/ui/{numeroComprobante}")
    suspend fun getInvoiceForUI(
        @Path("numeroComprobante") documentNumber: String
    ): InvoiceUIResponse

    @GET("factura/ui/usuario/{usuarioId}")
    suspend fun getUserInvoicesForUI(
        @Path("usuarioId") userId: String
    ): InvoicesUIResponse

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

    @POST("sunat/validar-credenciales")
    suspend fun validateCredentials(
        @Body request: ValidateCredentialsRequest
    ): ValidateCredentialsResponse

    @GET("factura/ui/usuario/{usuarioId}/completo")
    suspend fun getCompleteUserInvoices(
        @Path("usuarioId") userId: String
    ): InvoicesUIResponse

    @POST("api/auth/sign-in/email")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/auth/sign-up/email")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("api/auth/forget-password")
    suspend fun requestPasswordReset(
        @Body request: ForgotPasswordRequest
    ): Response<Unit>

    @GET("api/auth/session")
    suspend fun getSession(
    ): Response<SessionResponse>
}