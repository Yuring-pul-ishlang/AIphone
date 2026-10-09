package uz.aiphone

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class AgentService : Service() {
    override fun onCreate() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("a", "AI Telefon", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "a").setContentTitle("AI Telefon ishlayapti")
            .setSmallIcon(android.R.drawable.ic_dialog_info).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(1, n)
    }
    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val cmd = i?.getStringExtra("cmd")?.trim()
        if (!cmd.isNullOrEmpty() && !Agent.running) Thread { Agent.run(this, cmd) { Agent.sink?.invoke(it) } }.start()
        return START_STICKY
    }
    override fun onBind(i: Intent?): IBinder? = null
}
