package lol.arian.notifmirror

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ApiSender {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val leftoverPlaceholder = Regex("\\{\\{\\w+\\}\\}")

    fun send(context: Context, from: String, body: String, whenMs: Long) {
        sendNotification(context, mapOf("title" to from, "text" to body, "when" to whenMs))
    }

    /**
     * Native fallback used when the Flutter engine is not attached.
     * Renders the same template variables as the Dart side (app, subText, largeIcon, id, ...),
     * so payloads look identical no matter which path sent them.
     */
    fun sendNotification(context: Context, fields: Map<String, Any?>) {
        val prefs = context.getSharedPreferences("msg_mirror", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "") ?: ""
        val template = prefs.getString("payload_template", "") ?: ""
        val title = fields["title"]?.toString() ?: ""
        val text = fields["text"]?.toString()?.trim() ?: ""
        val body = if (text.isNotEmpty()) text else title
        if (endpoint.isEmpty() || body.isEmpty()) {
            try { LogStore.append(context, "ApiSender skip: endpoint/body empty") } catch (_: Exception) {}
            return
        }
        val whenMs = (fields["when"] as? Number)?.toLong() ?: 0L
        val dateStr = dateFormat.format(Date(if (whenMs > 0) whenMs else System.currentTimeMillis()))
        val values = LinkedHashMap<String, String>()
        for ((k, v) in fields) values[k] = v?.toString() ?: ""
        values["body"] = body
        values["from"] = title
        values["date"] = dateStr
        values["app"] = fields["app"]?.toString()?.takeIf { it.isNotEmpty() } ?: "notification"
        values["type"] = "notification"
        values["reception"] = prefs.getString("reception", "") ?: ""
        val payload = renderPayload(template, values)
        post(context, endpoint, payload, title, body.length)
    }

    /** Fills {{key}} placeholders with JSON-escaped values; unknown placeholders become "". */
    internal fun renderPayload(template: String, values: Map<String, String>): JSONObject {
        val fallback = JSONObject()
            .put("message_body", values["body"] ?: "")
            .put("message_from", values["from"] ?: "")
            .put("message_date", values["date"] ?: "")
            .put("type", values["type"] ?: "notification")
        if (template.isBlank()) return fallback
        return try {
            var rendered = template
            for ((k, v) in values) rendered = rendered.replace("{{$k}}", escape(v))
            rendered = leftoverPlaceholder.replace(rendered, "")
            JSONObject(rendered)
        } catch (e: Exception) {
            fallback
        }
    }

    private fun post(context: Context, endpoint: String, payload: JSONObject, from: String, bodyLen: Int) {

        Thread {
            try {
                LogStore.append(context, "ApiSender POST → $endpoint from='$from' len=$bodyLen")
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val code = conn.responseCode
                val resp = try {
                    BufferedReader(conn.inputStream.reader()).use { br ->
                        val sb = StringBuilder()
                        var line: String?
                        while (true) {
                            line = br.readLine(); if (line == null) break; sb.append(line)
                        }
                        sb.toString()
                    }
                } catch (_: Exception) { "" }
                LogStore.append(context, "ApiSender ← status=$code len=${resp.length}")
                conn.disconnect()
            } catch (e: Exception) {
                try { LogStore.append(context, "ApiSender error: ${e.message}") } catch (_: Exception) {}
            }
        }.start()
    }

    /** JSON string escaping without the surrounding quotes (templates already quote "{{x}}"). */
    internal fun escape(v: String): String {
        val quoted = JSONObject.quote(v)
        return quoted.substring(1, quoted.length - 1)
    }
}


