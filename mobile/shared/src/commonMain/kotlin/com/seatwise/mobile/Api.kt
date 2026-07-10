package com.seatwise.mobile

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** 平台默认后端地址（Android 模拟器用 10.0.2.2 映射宿主；iOS 模拟器用 localhost） */
expect val platformBaseUrl: String

/**
 * SeatWise 后端 REST 客户端（学生端子集）。token 通过 satoken 头传递。
 */
class SeatWiseApi(var baseUrl: String = platformBaseUrl) {

    var token: String? = null

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private fun url(path: String) = "$baseUrl/api$path"

    suspend fun login(username: String, password: String): LoginData {
        val res: ApiResult<LoginData> = client.post(url("/auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(LoginReq(username, password))
        }.body()
        if (res.code != "0" || res.data == null) throw ApiException(res.message.ifBlank { "登录失败" })
        token = res.data.token
        return res.data
    }

    suspend fun rooms(campusId: Long = 1): List<Room> {
        val res: ApiResult<List<Room>> = client.get(url("/study-rooms")) {
            auth(); parameter("campusId", campusId)
        }.body()
        return res.data ?: emptyList()
    }

    suspend fun board(roomId: Long, date: String, start: String, end: String): Board {
        val res: ApiResult<Board> = client.get(url("/study-rooms/$roomId/board")) {
            auth()
            parameter("date", date); parameter("start", start); parameter("end", end)
        }.body()
        return res.data ?: Board()
    }

    suspend fun createReservation(roomId: Long, seatId: Long, date: String, start: String, end: String): String {
        val res: ApiResult<Reservation> = client.post(url("/reservations")) {
            auth(); contentType(ContentType.Application.Json)
            setBody(CreateReq(roomId, seatId, date, start, end))
        }.body()
        if (res.code != "0") throw ApiException(res.message.ifBlank { "预约失败" })
        return "预约成功"
    }

    suspend fun myReservations(): List<Reservation> {
        val res: ApiResult<List<Reservation>> = client.get(url("/reservations/me")) { auth() }.body()
        return res.data ?: emptyList()
    }

    suspend fun cancel(id: Long) {
        val res: ApiResult<Reservation> = client.post(url("/reservations/$id/cancel")) { auth() }.body()
        if (res.code != "0") throw ApiException(res.message.ifBlank { "取消失败" })
    }

    private fun io.ktor.client.request.HttpRequestBuilder.auth() {
        token?.let { header("satoken", it) }
    }
}

class ApiException(message: String) : Exception(message)
