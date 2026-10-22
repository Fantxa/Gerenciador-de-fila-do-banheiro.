package com.example.restroomqueue

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class OverdueWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong("id", 0)
        val since = inputData.getLong("since", 0)
        val s = AppDb.get(applicationContext).dao().student(id) ?: return Result.success()
        if (s.outSince != since) return Result.success() // already came back
        val pt = Settings.lang(applicationContext) == Lang.PT
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("overdue", if (pt) "Atrasos" else "Overdue", NotificationManager.IMPORTANCE_HIGH))
        nm.notify(
            id.toInt(),
            NotificationCompat.Builder(applicationContext, "overdue")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(if (pt) "Fora há mais de 10 minutos" else "Out for 10+ minutes")
                .setContentText(if (pt) "${s.name} ainda não voltou" else "${s.name} hasn't come back yet")
                .setAutoCancel(true).build()
        )
        return Result.success()
    }
}
