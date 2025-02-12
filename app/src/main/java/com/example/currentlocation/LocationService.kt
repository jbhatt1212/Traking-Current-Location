package com.example.currentlocation

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService:Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var myLocationClient : MyLocationClient
    override fun onBind(p0: Intent?): IBinder? {
         return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        myLocationClient = MyDefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
        Log.e("LocationService", "Service Created")
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location",
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }
    @SuppressLint("ServiceCast")
    private fun start() {
        Log.e("LocationService", "Service started")
        try {
            val notification = NotificationCompat.Builder(this, "location")
                .setContentTitle("Tracking location")
                .setContentText("Location: null")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setOngoing(true)

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Ensure location client is initialized
            if (!::myLocationClient.isInitialized) {
                Log.e("LocationService", "myLocationClient is not initialized!")
                stopSelf() // Stop the service to prevent further crashes
                return
            }

            myLocationClient.getLocationUpdates(10L)
                .catch { e ->
                    Log.e("LocationService", "Error receiving location updates", e)
                }
                .onEach {
                    val lat = it.latitude.toString()
                    val long = it.longitude.toString()
                    Log.d("LocationService", "Location Updated: ($lat, $long)")

                    val updateNotification = notification.setContentText("Location: ($lat, $long)")
                    notificationManager.notify(1, updateNotification.build())
                }
                .launchIn(serviceScope)

            startForeground(1, notification.build())

        } catch (e: Exception) {
            Log.e("LocationService", "Error starting service", e)
        }
    }
    private fun stop(){
        Log.e("LocationService", "Service stoped")
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object{
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}