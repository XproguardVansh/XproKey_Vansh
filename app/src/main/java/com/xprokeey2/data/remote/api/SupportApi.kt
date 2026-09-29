package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.support.CreateTicketRequestDto
import com.xprokeey2.data.remote.dto.support.CreateTicketResponseDto
import com.xprokeey2.data.remote.dto.support.SupportTicketListDto
import com.xprokeey2.data.remote.dto.support.SupportTicketResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SupportApi {

    @POST("support/tickets")
    suspend fun createTicket(
        @Header("Authorization") authorization: String,
        @Body body: CreateTicketRequestDto,
    ): CreateTicketResponseDto

    @GET("support/my-tickets")
    suspend fun getMyTickets(@Header("Authorization") authorization: String): SupportTicketListDto

    @GET("support/tickets/{id}")
    suspend fun getTicket(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
    ): SupportTicketResponseDto
}
