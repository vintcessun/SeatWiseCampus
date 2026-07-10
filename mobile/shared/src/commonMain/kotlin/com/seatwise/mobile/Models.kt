package com.seatwise.mobile

import kotlinx.serialization.Serializable

/** 统一响应包装 { code, message, data } */
@Serializable
data class ApiResult<T>(
    val code: String = "",
    val message: String = "",
    val data: T? = null,
)

@Serializable
data class UserInfo(
    val id: Long = 0,
    val username: String = "",
    val realName: String = "",
    val role: String = "",
    val creditScore: Int = 0,
    val noShowCount: Int = 0,
)

@Serializable
data class LoginData(
    val token: String = "",
    val role: String = "",
    val userInfo: UserInfo = UserInfo(),
)

@Serializable
data class Room(
    val id: Long = 0,
    val name: String = "",
    val buildingId: Long = 0,
    val floorNo: Int = 0,
    val openStart: String = "",
    val openEnd: String = "",
    val status: String = "OPEN",
)

@Serializable
data class BoardSeat(
    val seatId: Long = 0,
    val seatNo: String? = null,
    val rowIndex: Int = 0,
    val colIndex: Int = 0,
    val cellType: String = "SEAT",
    val status: String = "FREE",
)

@Serializable
data class Board(
    val roomName: String = "",
    val cols: Int = 8,
    val seats: List<BoardSeat> = emptyList(),
)

@Serializable
data class Reservation(
    val id: Long = 0,
    val roomName: String = "",
    val seatNo: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "",
)

@Serializable
data class LoginReq(val username: String, val password: String)

@Serializable
data class CreateReq(
    val roomId: Long,
    val seatId: Long,
    val date: String,
    val startTime: String,
    val endTime: String,
)
