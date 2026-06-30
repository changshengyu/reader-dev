package com.htmake.reader.api.controller

import com.htmake.reader.api.ReturnData
import com.htmake.reader.api.task.TaskProgress
import com.htmake.reader.api.task.TaskService
import com.htmake.reader.api.task.TaskStatus
import com.htmake.reader.utils.asJsonArray
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssSource
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

class TaskController(coroutineContext: CoroutineContext) : BaseController(coroutineContext) {

    fun taskEvents(context: RoutingContext) {
        val taskId = context.pathParam("taskId") ?: ""
        val token = context.queryParam("token").firstOrNull() ?: ""
        val task = TaskService.get(taskId)
        if (task == null || task.token != token) {
            context.response().setStatusCode(404).end()
            return
        }
        context.response()
            .putHeader("Content-Type", "text/event-stream")
            .putHeader("Cache-Control", "no-cache")
            .putHeader("Connection", "keep-alive")
            .putHeader("X-Accel-Buffering", "no")
            .setChunked(true)
        task.subscribe(context)
    }

    suspend fun createImportSourcesTask(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val payload = context.bodyAsJson
        val sourceType = payload.getString("sourceType", "book")
        val sourceList = payload.getJsonArray("sourceList")
            ?: return returnData.setErrorMsg("参数错误")
        val userNameSpace = getUserNameSpace(context)
        val title = if (sourceType == "rss") "导入RSS源" else "导入书源"
        val task = TaskService.create(
            type = if (sourceType == "rss") "IMPORT_RSS_SOURCE" else "IMPORT_BOOK_SOURCE",
            title = title,
            userNameSpace = userNameSpace,
            total = sourceList.size()
        )

        launch(Dispatchers.IO) {
            if (sourceType == "rss") {
                runImportRssSourcesTask(task, sourceList)
            } else {
                runImportBookSourcesTask(task, sourceList)
            }
        }

        return returnData.setData(
            mapOf(
                "taskId" to task.taskId,
                "token" to task.token,
                "type" to task.type,
                "title" to task.title
            )
        )
    }

    private suspend fun runImportBookSourcesTask(task: TaskProgress, sourceJsonArray: JsonArray) {
        try {
            task.update(status = TaskStatus.RUNNING, message = "开始导入书源")
            var bookSourceList = asJsonArray(getUserStorage(task.userNameSpace, "bookSource")) ?: JsonArray()
            for (k in 0 until sourceJsonArray.size()) {
                val bookSource = BookSource.fromJson(sourceJsonArray.getJsonObject(k).toString()).getOrNull()
                if (bookSource != null) {
                    var existIndex = -1
                    for (i in 0 until bookSourceList.size()) {
                        val oldBookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
                        if (oldBookSource.bookSourceUrl == bookSource.bookSourceUrl) {
                            existIndex = i
                            break
                        }
                    }
                    if (existIndex >= 0) {
                        val sourceList = bookSourceList.getList()
                        sourceList[existIndex] = JsonObject.mapFrom(bookSource)
                        bookSourceList = JsonArray(sourceList)
                    } else {
                        bookSourceList.add(JsonObject.mapFrom(bookSource))
                    }
                }
                task.update(
                    current = k + 1,
                    message = "正在导入 ${k + 1}/${sourceJsonArray.size()}"
                )
            }
            saveUserStorage(task.userNameSpace, "bookSource", bookSourceList)
            task.update(
                current = sourceJsonArray.size(),
                status = TaskStatus.SUCCESS,
                message = "导入书源完成"
            )
        } catch (error: CancellationException) {
            task.update(status = TaskStatus.CANCELED, message = "任务已取消")
        } catch (error: Exception) {
            task.update(
                status = TaskStatus.FAILED,
                message = "导入书源失败",
                error = error.message ?: error.toString()
            )
        }
    }

    private suspend fun runImportRssSourcesTask(task: TaskProgress, sourceJsonArray: JsonArray) {
        try {
            task.update(status = TaskStatus.RUNNING, message = "开始导入RSS源")
            var rssSourceList = asJsonArray(getUserStorage(task.userNameSpace, "rssSources")) ?: JsonArray()
            for (k in 0 until sourceJsonArray.size()) {
                val rssSource = sourceJsonArray.getJsonObject(k).mapTo(RssSource::class.java)
                if (rssSource.sourceUrl.isNotEmpty() && rssSource.sourceName.isNotEmpty()) {
                    var existIndex = -1
                    for (i in 0 until rssSourceList.size()) {
                        val oldRssSource = rssSourceList.getJsonObject(i).mapTo(RssSource::class.java)
                        if (oldRssSource.sourceUrl == rssSource.sourceUrl) {
                            existIndex = i
                            break
                        }
                    }
                    if (existIndex >= 0) {
                        val sourceList = rssSourceList.getList()
                        sourceList[existIndex] = JsonObject.mapFrom(rssSource)
                        rssSourceList = JsonArray(sourceList)
                    } else {
                        rssSourceList.add(JsonObject.mapFrom(rssSource))
                    }
                }
                task.update(
                    current = k + 1,
                    message = "正在导入 ${k + 1}/${sourceJsonArray.size()}"
                )
            }
            saveUserStorage(task.userNameSpace, "rssSources", rssSourceList)
            task.update(
                current = sourceJsonArray.size(),
                status = TaskStatus.SUCCESS,
                message = "导入RSS源完成"
            )
        } catch (error: CancellationException) {
            task.update(status = TaskStatus.CANCELED, message = "任务已取消")
        } catch (error: Exception) {
            task.update(
                status = TaskStatus.FAILED,
                message = "导入RSS源失败",
                error = error.message ?: error.toString()
            )
        }
    }
}
