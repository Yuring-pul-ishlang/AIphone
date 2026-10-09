package uz.aiphone

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject

class PhoneService : AccessibilityService() {
    companion object { @Volatile var inst: PhoneService? = null }
    private var nodes = listOf<AccessibilityNodeInfo>()

    override fun onServiceConnected() { inst = this }
    override fun onUnbind(i: Intent?): Boolean { inst = null; return super.onUnbind(i) }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun snapshot(): String {
        val root = rootInActiveWindow ?: return "(bo'sh ekran)"
        val list = mutableListOf<AccessibilityNodeInfo>()
        val sb = StringBuilder("app=${root.packageName}\n")
        fun walk(n: AccessibilityNodeInfo) {
            val t = n.text?.toString() ?: ""
            val d = n.contentDescription?.toString() ?: ""
            if (n.isClickable || n.isEditable || n.isScrollable || t.isNotBlank() || d.isNotBlank()) {
                sb.append(list.size).append('|').append(n.className?.toString()?.substringAfterLast('.'))
                    .append('|').append(t.take(60)).append('|').append(d.take(60))
                    .append(if (n.isClickable) "|click" else "").append(if (n.isEditable) "|edit" else "").append('\n')
                list.add(n)
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let { walk(it) }
        }
        walk(root); nodes = list
        return sb.toString().take(6000)
    }

    fun perform(ctx: Context, a: JSONObject) {
        val n = nodes.getOrNull(a.optInt("id", -1))
        when (a.optString("action")) {
            "open_app" -> {
                val pm = packageManager
                val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val app = pm.queryIntentActivities(q, 0).firstOrNull {
                    it.loadLabel(pm).toString().contains(a.optString("app"), true)
                }
                app?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(::startActivity) }
            }
            "url" -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.optString("url"))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "click" -> { var x = n; while (x != null && !x.isClickable) x = x.parent; x?.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
            "type" -> n?.let {
                val b = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, a.optString("text")) }
                it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
            }
            "scroll" -> nodes.firstOrNull { it.isScrollable }?.performAction(
                if (a.optString("dir") == "up") AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }
}
