package com.tg.huli

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class Aria2Json private constructor() {

    companion object {

        const val ADD_URI = "aria2.addUri"

        fun builder(): Aria2Json {
            return Aria2Json()
        }
    }

    private var isDownload = false
    fun setType(isDownload: Boolean) {
        this.isDownload = isDownload
    }

    private var id = ""
    fun setId(id: String) {
        this.id = id
    }

    private var jsonrpc = "2.0"
    fun setJsonRPC(jsonrpc: String) = apply {
        this.jsonrpc = jsonrpc
    }

    private var method = ADD_URI
    fun setMethod(method: String) = apply {
        this.method = method
    }

    private var url = ""
    fun setURL(url: String) = apply {
        this.url = url
    }

    private var token = ""
    fun setToken(token: String) = apply {
        this.token = token
    }

    override fun toString(): String {
        if (id.isEmpty()) {
            id = "aria2.${System.currentTimeMillis()}"
        }
        return "{\"jsonrpc\":\"$jsonrpc\",\"method\":\"$method\",\"id\":\"$id\",\"params\":[\"token:$token\",[\"$url\"],{}]}"
    }

    fun send(url: String) = OkHttpClient()
        .newCall(
            Request.Builder()
                .url(url)
                .post(toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        )
        .execute()
        .body?.string()

}