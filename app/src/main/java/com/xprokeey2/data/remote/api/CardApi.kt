package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.card.CardDto
import com.xprokeey2.data.remote.dto.card.CardListDto
import com.xprokeey2.data.remote.dto.card.CardRequestDto
import com.xprokeey2.data.remote.dto.card.CardUpdateRequestDto
import com.xprokeey2.data.remote.dto.common.DataResponseDto
import com.xprokeey2.data.remote.dto.common.MessageResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CardApi {

    @GET("cards/getallcard")
    suspend fun getCards(@Header("Authorization") authorization: String): DataResponseDto<CardListDto>

    @GET("cards/getcardbyid/{id}")
    suspend fun getCard(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
    ): DataResponseDto<CardDto>

    @POST("cards/createcard")
    suspend fun createCard(
        @Header("Authorization") authorization: String,
        @Body body: CardRequestDto,
    ): DataResponseDto<CardDto>

    @PUT("cards/updatecard/{id}")
    suspend fun updateCard(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body body: CardUpdateRequestDto,
    ): DataResponseDto<CardDto>

    @DELETE("cards/deletecard/{id}")
    suspend fun deleteCard(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
    ): MessageResponseDto
}
