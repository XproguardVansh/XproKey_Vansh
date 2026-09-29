package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.common.MessageResponseDto
import com.xprokeey2.data.remote.dto.vault.CreateCategoryRequestDto
import com.xprokeey2.data.remote.dto.vault.CreateCategoryResponseDto
import com.xprokeey2.data.remote.dto.vault.CustomCategoriesDto
import com.xprokeey2.data.remote.dto.vault.VaultCategoriesDto
import com.xprokeey2.data.remote.dto.vault.VaultItemListDto
import com.xprokeey2.data.remote.dto.vault.VaultItemRequestDto
import com.xprokeey2.data.remote.dto.vault.VaultItemResponseDto
import com.xprokeey2.data.remote.dto.vault.VaultItemUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface VaultApi {

    @GET("vault/getallvault")
    suspend fun getItems(@Header("Authorization") authorization: String): VaultItemListDto

    @GET("vault/getvaultbyid/{id}")
    suspend fun getItem(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
    ): VaultItemResponseDto

    @POST("vault/createvault")
    suspend fun createItem(
        @Header("Authorization") authorization: String,
        @Body body: VaultItemRequestDto,
    ): VaultItemResponseDto

    @PATCH("vault/updatevault/{id}")
    suspend fun updateItem(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body body: VaultItemUpdateRequestDto,
    ): MessageResponseDto

    @DELETE("vault/deletevault/{id}")
    suspend fun deleteItem(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
    ): MessageResponseDto

    @GET("vault/categories")
    suspend fun getCategories(@Header("Authorization") authorization: String): VaultCategoriesDto

    @GET("vault/categories/custom")
    suspend fun getCustomCategories(@Header("Authorization") authorization: String): CustomCategoriesDto

    @POST("vault/categories")
    suspend fun createCategory(
        @Header("Authorization") authorization: String,
        @Body body: CreateCategoryRequestDto,
    ): CreateCategoryResponseDto
}
