package com.tg.huli

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tg.huli.Utils.Companion.fetchJarPath
import lib.github1552980358.ktExtension.jvm.keyword.tryCatch
import lib.github1552980358.ktExtension.jvm.keyword.tryOnly
import java.io.File
import java.net.URL
import kotlin.text.StringBuilder

const val FILE_CONFIG = "server.config"
const val FILE_TOKEN = "token"
const val FILE_MASTER = "master"
const val FILE_TARGET_DIR = "target_dir"
const val FILE_ARIA_TOKEN = "aria_token"
const val FILE_ARIA_URL = "aria_url"

const val FILE_CHAT_ID = "chat_id"

const val COMMAND_TELEGRAPH = "/telegraph"
const val COMMAND_ARIA = "/aria"
const val COMMAND_ARIA_STAT = "stat"
const val COMMAND_ARIA_DOWNLOAD = "dl"

val jarPath = fetchJarPath()

var token = ""
var historyChatId = ""
var masterId = -1
var targetDir = ""

var ariaToken = ""
var ariaURL = ""

fun main(args: Array<String>) {

    println("Jar location: $jarPath")

    if (args.size == 1 && args[0] == "init") {
        initialize()
        return
    }

    if (!getDataLoaded(args)) {
        println("Unknown args/files not found")
        return
    }

    println("=================================")
    println(" Token: $token")
    println(" HistoryChatId: $historyChatId")
    println(" MasterID: $masterId")
    println(" Target Directory: $targetDir")
    println(" Jar Directory: $jarPath")
    println("=================================")
    println()

    val url = "https://api.telegram.org/bot$token/"

    var msg = ""

    var chatId = ""
    var userId = 0

    var updates: JsonObject

    val chatIdFile = File(FILE_CHAT_ID).apply {
        if (!exists()) {
            createNewFile()
        }
    }
    var text = ""
    var list: ArrayList<String>
    val stringBuilder = StringBuilder()

    while (true) {
        tryOnly { Thread.sleep(1000) }

        if (!tryCatch { tryCatch { msg = getUpdates(url, historyChatId) } } || msg.isEmpty()) {
            continue
        }

        println()
        println("Update received")

        updates = try {
            JsonParser.parseString(msg).asJsonObject
        } catch (e: Exception) {
            continue
        }
        if (!try {
                updates.get("ok").asBoolean
            } catch (e: Exception) {
                continue
            }
        ) {
            continue
        }
        val results = try {
            updates.get("result").asJsonArray
        } catch (e: Exception) {
            continue
        }
        if (results.size() == 0) {
            continue
        }

        println("Read updates...")

        // read message
        for (jsonElement in results) {
            var jsonMessage = jsonElement.asJsonObject

            if (!tryCatch { chatId = jsonMessage.get("update_id").asString } || chatId.isEmpty() || chatId == historyChatId) {
                println("Updates skipped")
                continue
            }

            println("ChatId: $chatId")

            // update chat id
            historyChatId = chatId

            // Can't get message object
            if (!tryCatch { jsonMessage = jsonMessage.get("message").asJsonObject }) {
                continue
            }

            if (!tryCatch { userId = jsonMessage.get("from").asJsonObject.get("id").asInt }) {
                println("Unknown UserId/UserId fetch failed")
                continue
            }

            if (userId != masterId) {
                sendMsg(url, userId, "You are not my master, get the fuck off!")
                println("Unknown user update")
                continue
            }

            if (!tryCatch { text = jsonMessage.get("text").asString } || text.isEmpty()) {
                println("Text fetch failed")
                continue
            }

            // Save message
            if (!text.startsWith('/')) {
                println("Non-command message -> $text")
                continue
            }

            println("Command message -> $text")

            list = decodeCommand(text)

            when (list.first()) {
                COMMAND_TELEGRAPH -> {
                    sendMsg(url, userId, "Start downloading...")
                    sendMsg(
                        url,
                        userId,
                        if (downloadGraph(list[2], stringBuilder, list[1] == "1")) "$stringBuilder: Success"
                        else "$stringBuilder: Failed"
                    )
                }
                COMMAND_ARIA -> {
                    when (list[1]) {
                        COMMAND_ARIA_STAT -> {

                        }
                        COMMAND_ARIA_DOWNLOAD -> {
                            sendMsg(url, userId, if (ariaDownload(list[2], stringBuilder)) "Aria2 added: $stringBuilder" else "Aria2 add failed")
                        }
                        else -> sendMsg(url, userId, "Unknown command")
                    }
                }
                else -> sendMsg(url, userId, "Unknown command")
            }

        }

        chatIdFile.writeText(historyChatId)

    }

}

fun initialize() {
    println("Initialize start")

    File(jarPath, FILE_CONFIG).apply {
        if (exists()) {
            delete()
        }
        createNewFile()
        print("Paste token: ")
        appendText("$FILE_TOKEN=" + System.`in`.bufferedReader().readLine() + '\n')
        print("Input master id: ")
        appendText("$FILE_MASTER=" +System.`in`.bufferedReader().readLine() + '\n')
        print("Input target directory: ")
        appendText("$FILE_TARGET_DIR=" +System.`in`.bufferedReader().readLine() + '\n')
    }

    println("  Initialize done  ")
    println("===================")
    println()
}

fun getUpdates(url: String, lastChatId: String): String {
    var msg: String
    URL(
        url + "getUpdates?" + if (lastChatId.isNotEmpty()) {
            "offset=$lastChatId&limit=10"
        } else "limit=10"
    ).openStream().bufferedReader().use { bufferedReader ->
        msg = bufferedReader.readText()
    }
    return msg
}

fun getDataLoaded(args: Array<String>): Boolean {
    return if (args.isEmpty() || (args.size != 4 && args.size != 3)) loadDataFromFile()
    else loadDataFromArgs(args)
}

fun loadDataFromFile(): Boolean {
    File(jarPath, FILE_CHAT_ID).apply {
        if (exists()) {
            historyChatId = readText()
        }
    }

    File(jarPath, FILE_CONFIG).readLines().apply {
        for (line in this) {
            when (line.substring(0, line.indexOf('='))) {
                FILE_TOKEN -> token = line.substring(line.indexOf('=') + 1)
                FILE_TARGET_DIR -> targetDir = line.substring(line.indexOf('=') + 1)
                FILE_MASTER -> masterId = line.substring(line.indexOf('=') + 1).toInt()
                FILE_ARIA_TOKEN -> ariaToken = line.substring(line.indexOf('=') + 1)
                FILE_ARIA_URL -> ariaURL = line.substring(line.indexOf('=') + 1)
                else -> continue
            }
        }
    }

    if (targetDir.isNotEmpty()) {
        if (!targetDir.endsWith('/') && !targetDir.endsWith('\\')) {
            targetDir += File.separator
        }
    }

    if (token.isEmpty() || masterId == -1) {
        return false
    }

    return true
}

fun loadDataFromArgs(args: Array<String>): Boolean {
    if (args.size == 4) {
        token = args[0]
        historyChatId = args[1]
        masterId = args[2].toInt()
        targetDir = args[3]
    } else {
        token = args[0]
        masterId = args[1].toInt()
        targetDir = args[2]
    }
    return true
}

fun sendMsg(url: String, userId: Int, text: String) {
    println("SendMessage -> $userId: $text")
    URL(url + "sendMessage?chat_id=$userId&text=$text").openStream().use { tryOnly { Thread.sleep(1000) } }
}

fun getExtension(url: String): String {
    return url.substring(url.lastIndexOf('.'))
}

fun getTitle(index: Int, url: String): String {
    return when {
        index > 99 -> index.toString()
        index > 9 -> "0$index"
        else -> "00$index"
    } + getExtension(url)
}

fun downloadGraph(url: String, stringBuilder: StringBuilder, hasCover: Boolean): Boolean {
    var respond = ""
    if (!tryCatch {
            URL(url).openStream().bufferedReader().use { bufferedReader -> respond = bufferedReader.readText() }
        } || respond.isEmpty()) {
        return false
    }
    respond = respond.substring(respond.indexOf("<meta property=\"og:title\" content=\""))

    val dir = File(
        targetDir + respond.substring(35, respond.indexOf("\">"))
    ).apply {
        mkdir()
        println("Target dir created -> $path")
    }

    stringBuilder.clear()
    stringBuilder.append(dir.name)

    var i = 0
    var urlPic: String

    println("Start Download...")
    if (!tryCatch {
            while (respond.indexOf("<img src=\"") != -1) {
                respond = respond.substring(respond.indexOf("<img src=\"") + 10)
                urlPic = "https://telegra.ph${respond.substring(0, respond.indexOf("\">"))}"
                respond = respond.substring(respond.indexOf("\">"))
                if (i == 0 && hasCover) {
                    i++
                    continue
                }
                URL(urlPic).openStream().use { inputStream ->
                    File(dir, getTitle(i, urlPic)).apply {
                        println("${urlPic} -> $path")
                        writeBytes(inputStream.readBytes())
                    }
                }
                i++
            }
        }) {
        dir.delete()
        return false
    }

    return true
}

fun ariaDownload(url: String, stringBuilder: StringBuilder) = tryCatch {
    stringBuilder.clear()
    stringBuilder.append(
        Aria2Json.builder()
            .setURL(url)
            .setToken(ariaToken)
            .send(ariaURL)
    )
}

fun decodeCommand(string: String) = arrayListOf<String>().apply {

    val stringBuilder = StringBuilder()

    for ((i, char) in string.withIndex()) {
        if (char == ' ') {
            if (stringBuilder.toString().isNotEmpty()) {
                add(stringBuilder.toString())
                if (this.size == 2) {
                    add(string.substring(i + 1))
                    return@apply
                }
            }
            stringBuilder.clear()
            continue
        }
        stringBuilder.append(char)
    }

}