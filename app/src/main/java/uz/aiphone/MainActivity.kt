package uz.aiphone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = getSharedPreferences("p", 0)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 80, 40, 40) }
        fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }.also { col.addView(it) }
        val key = EditText(this).apply { hint = "Anthropic API kalit"; setText(p.getString("key", "")); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val cmd = EditText(this).apply { hint = "Buyruq" }
        val log = TextView(this)
        col.addView(key)
        btn("1. Maxsus imkoniyatlarni yoqish") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        btn("2. Batareya cheklovini o'chirish (24 soat)") {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }
        col.addView(cmd)
        btn("Bajarish") {
            p.edit().putString("key", key.text.toString().trim()).apply()
            startForegroundService(Intent(this, AgentService::class.java).putExtra("cmd", cmd.text.toString()))
        }
        btn("To'xtatish") { Agent.running = false }
        col.addView(log)
        setContentView(ScrollView(this).apply { addView(col) })
        Agent.sink = { s -> runOnUiThread { log.append(s + "\n") } }
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        startForegroundService(Intent(this, AgentService::class.java))
    }
}
