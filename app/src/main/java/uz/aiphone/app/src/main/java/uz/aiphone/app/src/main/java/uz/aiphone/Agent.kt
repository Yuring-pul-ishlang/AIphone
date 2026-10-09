package uz.aiphone

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Agent {
    @Volatile var running = false
    @Volatile var sink: ((String) -> Unit)? = null
    private const val SYS = "You operate an Android phone for its owner, only to fulfil the owner's command. " +
        "Each turn you get the goal, steps done, and the current screen as lines 'id|class|text|desc|click|edit'. " +
        "Reply with ONE JSON object only: " +
        "{\"action\":\"open_app\",\"app\":\"label\"} | {\"action\":\"url\",\"url\":\"tel:/sms:/geo:/https://...\"} | " +
        "{\"action\":\"click\",\"id\":N} | {\"action\":\"type\",\"id\":N,\"text\":\"...\"} | " +
        "{\"action\":\"scroll\",\"dir\":\"down|up\"} | {\"action\":\"back\"} | {\"action\":\"home\"} | " +
        "{\"action\":\"done\",\"msg\":\"short result in the owner's language\"}. " +
        "Do nothing beyond the command. Never enter passwords, payment data or confirm money transfers; use done and ask the owner instead."

    fun run(ctx: Context, goal: String, log: (String) -> Unit) {
        val key = ctx.getSharedPreferences("p", 0).getString("key", "") ?: ""
        if (key.isBlank()) { log("API kalit kiritilmagan"); return }
        val svc = PhoneService.inst
        if (svc == null) { log("Maxsus imkoniyatlar yoqilmagan"); return }
        running = true
        val hist = StringBuilder()
        try {
            for (step in 1..25) {
                if (!running) { log("To'xtatildi"); break }
                val a = ask(key, "Goal: $goal\nSteps done:\n$hist\nScreen:\n${svc.snapshot()}")
                log("$step: $a")
                if (a.optString("action") == "done") { log(a.optString("msg", "Tayyor")); break }
                hist.append(step).append(". ").append(a).append('\n')
                svc.perform(ctx, a)
                Thread.sleep(1200)
            }
        } catch (e: Exception) { log("Xato: ${e.message}") } finally { running = false }
    }

    private fun ask(key: String, user: String): JSONObject {
        val body = JSONObject().put("model", "claude-sonnet-5-5").put("max_tokens", 300).put("system", SYS)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", user)))
        val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("x-api-key", key)
        c.setRequestProperty("anthropic-version", "2023-06-01")
        c.setRequestProperty("content-type", "application/json")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val txt = (if (c.responseCode < 400) c.inputStream else c.errorStream).bufferedReader().readText()
        if (c.responseCode >= 400) throw Exception(txt.take(200))
        val t = JSONObject(txt).getJSONArray("content").getJSONObject(0).getString("text")
        return JSONObject(t.substring(t.indexOf('{'), t.lastIndexOf('}') + 1))
    }
}
