package com.ganglike.pintodo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 할 일 목록 저장소. SharedPreferences(JSON)에 저장하고,
 * 같은 프로세스 안에서는 StateFlow로 화면에 변경을 알린다.
 */
object TodoStore {
    private const val PREFS = "todos"
    private const val KEY_LIST = "list"
    private const val KEY_NEXT_ID = "next_id"

    private val state = MutableStateFlow<List<Todo>>(emptyList())
    private var loaded = false

    fun flow(context: Context): StateFlow<List<Todo>> {
        ensureLoaded(context)
        return state
    }

    fun all(context: Context): List<Todo> {
        ensureLoaded(context)
        return state.value
    }

    fun get(context: Context, id: Int): Todo? = all(context).find { it.id == id }

    fun newId(context: Context): Int {
        val p = prefs(context)
        val id = p.getInt(KEY_NEXT_ID, 1)
        p.edit().putInt(KEY_NEXT_ID, id + 1).commit()
        return id
    }

    /** id가 같으면 교체, 없으면 추가 */
    fun upsert(context: Context, todo: Todo) = update(context) { list ->
        if (list.any { it.id == todo.id }) list.map { if (it.id == todo.id) todo else it }
        else list + todo
    }

    fun modify(context: Context, id: Int, change: (Todo) -> Todo) = update(context) { list ->
        list.map { if (it.id == id) change(it) else it }
    }

    fun remove(context: Context, id: Int) = update(context) { list -> list.filterNot { it.id == id } }

    @Synchronized
    fun update(context: Context, change: (List<Todo>) -> List<Todo>) {
        ensureLoaded(context)
        val next = change(state.value)
        save(context, next)
        state.value = next
    }

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        state.value = load(context)
        loaded = true
    }

    private fun load(context: Context): List<Todo> {
        val arr = JSONArray(prefs(context).getString(KEY_LIST, "[]")!!)
        return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }

    private fun save(context: Context, todos: List<Todo>) {
        val arr = JSONArray()
        todos.forEach { arr.put(toJson(it)) }
        prefs(context).edit().putString(KEY_LIST, arr.toString()).commit()
    }

    private fun toJson(t: Todo) = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("memo", t.memo)
        put("pinned", t.pinned)
        put("alertMode", t.alertMode.name)
        putOpt("startAt", t.startAt)
        putOpt("endAt", t.endAt)
        put("repeatDays", JSONArray(t.repeatDays.sorted()))
        put("dailyStart", t.dailyStart)
        putOpt("dailyEnd", t.dailyEnd)
        put("createdAt", t.createdAt)
        putOpt("doneAt", t.doneAt)
        putOpt("snoozeUntil", t.snoozeUntil)
        putOpt("hiddenKey", t.hiddenKey)
        putOpt("alertedKey", t.alertedKey)
    }

    // v1 데이터({id, text})도 읽을 수 있게 모든 필드를 선택적으로 읽음
    private fun fromJson(o: JSONObject): Todo {
        val days = o.optJSONArray("repeatDays")
        return Todo(
            id = o.getInt("id"),
            title = o.optString("title", o.optString("text")),
            memo = o.optString("memo"),
            pinned = o.optBoolean("pinned", true),
            alertMode = runCatching { AlertMode.valueOf(o.getString("alertMode")) }.getOrDefault(AlertMode.SILENT),
            startAt = o.optLongOrNull("startAt"),
            endAt = o.optLongOrNull("endAt"),
            repeatDays = days?.let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() } ?: emptySet(),
            dailyStart = o.optInt("dailyStart", 9 * 60),
            dailyEnd = o.optLongOrNull("dailyEnd")?.toInt(),
            createdAt = o.optLong("createdAt", 0L),
            doneAt = o.optLongOrNull("doneAt"),
            snoozeUntil = o.optLongOrNull("snoozeUntil"),
            hiddenKey = o.optLongOrNull("hiddenKey"),
            // v1 데이터는 이미 알림이 떠 있었으므로 다시 울리지 않게 alertedKey를 맞춰 둠
            alertedKey = if (o.has("alertMode")) o.optLongOrNull("alertedKey") else 0L,
        )
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
