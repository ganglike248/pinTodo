package com.pintodo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 백업 파일(JSON): 할 일 전체 + 설정. 서버 없이 사용자가 고른 위치(내 파일, 드라이브 등)에 저장한다.
 * { app: "PinTodo", format: 1, exportedAt, nextId, todos: [...], settings: {...} }
 */
object Backup {
    private const val APP = "PinTodo"
    private const val FORMAT = 1

    class Content(val todos: List<Todo>, val settings: AppSettings?, val nextId: Int, val exportedAt: Long)

    fun export(context: Context): String = JSONObject().apply {
        put("app", APP)
        put("format", FORMAT)
        put("exportedAt", System.currentTimeMillis())
        put("nextId", TodoStore.peekNextId(context))
        put("todos", JSONArray().apply { TodoStore.all(context).forEach { put(TodoStore.toJson(it)) } })
        put("settings", SettingsStore.toJson(SettingsStore.get(context)))
    }.toString(2)

    /** 백업 파일이 아니면 IllegalArgumentException */
    fun read(json: String): Content {
        val o = runCatching { JSONObject(json) }.getOrNull()
        require(o != null && o.optString("app") == APP && o.has("todos")) { "PinTodo 백업 파일이 아니에요" }
        require(o.optInt("format", 1) <= FORMAT) { "더 새 버전의 앱에서 만든 백업이에요. 앱을 업데이트해 주세요" }
        val arr = o.getJSONArray("todos")
        return Content(
            todos = (0 until arr.length()).map { TodoStore.fromJson(arr.getJSONObject(it)) },
            settings = o.optJSONObject("settings")?.let { SettingsStore.fromJson(it) },
            nextId = o.optInt("nextId", 1),
            exportedAt = o.optLong("exportedAt", 0L),
        )
    }

    /** 지금 있는 할 일과 설정을 백업 내용으로 바꿈. 알림·알람은 호출한 쪽에서 Sync.run()으로 맞출 것 */
    fun restore(context: Context, content: Content) {
        TodoStore.replaceAll(context, content.todos, content.nextId)
        content.settings?.let { s -> SettingsStore.update(context) { s } }
    }
}
