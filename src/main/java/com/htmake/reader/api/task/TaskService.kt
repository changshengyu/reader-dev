package com.htmake.reader.api.task

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class TaskStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELED
}

data class TaskProgress(
    val taskId: String,
    val token: String,
    val type: String,
    val title: String,
    val userNameSpace: String,
    var total: Int = 0,
    var current: Int = 0,
    var status: TaskStatus = TaskStatus.PENDING,
    var message: String = "",
    var error: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    private val subscribers = mutableSetOf<RoutingContext>()

    @Synchronized
    fun subscribe(context: RoutingContext) {
        subscribers.add(context)
        context.response().closeHandler {
            synchronized(this) {
                subscribers.remove(context)
            }
        }
        send(context)
    }

    @Synchronized
    fun update(
        current: Int = this.current,
        total: Int = this.total,
        status: TaskStatus = this.status,
        message: String = this.message,
        error: String = this.error
    ) {
        this.current = current
        this.total = total
        this.status = status
        this.message = message
        this.error = error
        this.updatedAt = System.currentTimeMillis()
        broadcast()
    }

    @Synchronized
    fun broadcast() {
        val iterator = subscribers.iterator()
        while (iterator.hasNext()) {
            val context = iterator.next()
            if (context.response().closed()) {
                iterator.remove()
            } else {
                send(context)
            }
        }
    }

    private fun send(context: RoutingContext) {
        context.response().write("event: progress\n")
        context.response().write("data: ${toJson().encode()}\n\n")
    }

    fun toJson(): JsonObject {
        return JsonObject()
            .put("taskId", taskId)
            .put("type", type)
            .put("title", title)
            .put("total", total)
            .put("current", current)
            .put("status", status.name.lowercase())
            .put("message", message)
            .put("error", error)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)
    }
}

object TaskService {
    private val tasks = ConcurrentHashMap<String, TaskProgress>()

    fun create(type: String, title: String, userNameSpace: String, total: Int = 0): TaskProgress {
        val task = TaskProgress(
            taskId = UUID.randomUUID().toString(),
            token = UUID.randomUUID().toString().replace("-", ""),
            type = type,
            title = title,
            userNameSpace = userNameSpace,
            total = total
        )
        tasks[task.taskId] = task
        cleanup()
        return task
    }

    fun get(taskId: String): TaskProgress? {
        return tasks[taskId]
    }

    private fun cleanup() {
        val expireBefore = System.currentTimeMillis() - 30 * 60 * 1000
        tasks.entries.removeIf { (_, task) ->
            task.updatedAt < expireBefore && task.status != TaskStatus.RUNNING
        }
    }
}
