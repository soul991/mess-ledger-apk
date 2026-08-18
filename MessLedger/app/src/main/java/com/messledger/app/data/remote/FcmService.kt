package com.messledger.app.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.messledger.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var firestore: FirebaseFirestore

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = auth.currentUser?.uid ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("fcmTokens")
                    .document(token)
                    .set(mapOf("token" to token, "updatedAt" to System.currentTimeMillis()))
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val title = message.notification?.title ?: message.data["title"] ?: "Mess Ledger"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mess Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "mess_ledger_channel"
    }
}
