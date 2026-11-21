package lol.arian.notifmirror

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ApiSenderTest {
    private val template = """
        {
          "notification_id": "{{id}}",
          "message_body": "{{body}}",
          "message_from": "{{from}}",
          "message_app": "{{app}}",
          "extra_fields": { "subText": "{{subText}}", "largeIcon": "{{largeIcon}}" }
        }
    """.trimIndent()

    @Test
    fun doesNotCrashWithEmptyEndpoint() {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication() as Context
        ApiSender.send(ctx, from = "t", body = "b", whenMs = 0L)
        assert(true)
    }

    @Test
    fun rendersAllFieldsWithJsonEscaping() {
        val json = ApiSender.renderPayload(template, mapOf(
            "id" to "0|jp.naver.line.android|1|null|10001|1700000000000",
            "body" to "he said \"hi\"\nnext line \\ end",
            "from" to "Rick",
            "app" to "jp.naver.line.android",
            "subText" to "布-實戰群",
            "largeIcon" to "iVBORw0KGgo=",
        ))
        assertEquals("0|jp.naver.line.android|1|null|10001|1700000000000", json.getString("notification_id"))
        assertEquals("he said \"hi\"\nnext line \\ end", json.getString("message_body"))
        assertEquals("jp.naver.line.android", json.getString("message_app"))
        val extra = json.getJSONObject("extra_fields")
        assertEquals("布-實戰群", extra.getString("subText"))
        assertEquals("iVBORw0KGgo=", extra.getString("largeIcon"))
    }

    @Test
    fun unknownPlaceholdersBecomeEmpty() {
        val json = ApiSender.renderPayload(template, mapOf("body" to "b", "from" to "f", "app" to "x"))
        assertEquals("", json.getString("notification_id"))
        assertEquals("", json.getJSONObject("extra_fields").getString("subText"))
    }

    @Test
    fun brokenTemplateFallsBackToMinimalPayload() {
        val json = ApiSender.renderPayload("{ not json {{body}}", mapOf("body" to "b", "from" to "f", "date" to "d"))
        assertEquals("b", json.getString("message_body"))
        assertEquals("f", json.getString("message_from"))
    }
}
