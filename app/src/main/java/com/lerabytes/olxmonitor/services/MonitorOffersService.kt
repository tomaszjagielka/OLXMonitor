package com.lerabytes.olxmonitor.services

import android.app.*
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lerabytes.olxmonitor.R
import com.lerabytes.olxmonitor.activities.MainActivity
import com.lerabytes.olxmonitor.adapters.*
import com.lerabytes.olxmonitor.enums.ItemViewTypes
import com.lerabytes.olxmonitor.enums.OfferViewTypes
import com.lerabytes.olxmonitor.fragments.*
import com.lerabytes.olxmonitor.models.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


var isMonitorOffersServiceStarted = false

class MonitorOffersService : Service() {
    private val sTag = "MonitorOffersService"

    private var offersFragmentCallbacks: Callbacks? = null
    private val mBinder: IBinder = LocalBinder()

    override fun onCreate() {
        Log.d("this", "Service created.")
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onStart(intent: Intent?, startid: Int) {
        Log.d(sTag, "Service started by user.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(sTag, "Service started.")

        isMonitorOffersServiceStarted = true
        startMonitorOffersThread()

        return START_STICKY
    }

    override fun onDestroy() {
//        isMonitorOffersServiceStarted = false
        Log.d(sTag, "Service stopped.")
    }

    interface Callbacks {
        fun updateOffers(itemIndex: Int)
    }

    fun registerOffersFragmentCallbacks(fragment: OffersAdapter) {
        offersFragmentCallbacks = fragment
    }

    inner class LocalBinder : Binder() {
        fun getServiceInstance(): MonitorOffersService {
            return this@MonitorOffersService
        }
    }

    private fun createNotification(title: String, message: String = "", notificationId: Int = 0) {
        val channelId = "olxmonitorChannel"
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = channelId
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel)
        }

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
//                .setContentText(message)
        val resultIntent = Intent(this, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent =
            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(resultPendingIntent)
        builder.setAutoCancel(true)
        notificationManager.notify(notificationId, builder.build())
    }

    private fun getForegroundApp(): String {
        val appProcesses =
            (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses
        for (appProcess in appProcesses) {
            if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return appProcess.processName
            }
        }
        return ""
    }

    private fun getOffersToAdd(old: ArrayList<Item>, new: ArrayList<Item>): ArrayList<Item> {
        val offersToAdd = ArrayList<Item>()
        val oldOffersStrings = ArrayList<String>()

        var offerText: OfferText? = null
        var offerImage: OfferImage? = null

        for (offer in old) {
            when (offer.type) {
                OfferViewTypes.OfferText.ordinal ->
                    offerText = offer.`object` as OfferText
                else ->
                    offerImage = offer.`object` as OfferImage
            }

            oldOffersStrings.add(
                when (offer.type) {
                    OfferViewTypes.OfferText.ordinal ->
                        offerText?.offerTitle + offerText?.offerPrice + offerText?.offerLocation + offerText?.offerDate
                    else ->
                        offerImage?.offerTitle + offerImage?.offerPrice + offerImage?.offerLocation + offerImage?.offerDate
                }
            )
        }

        for (offer in new) {
            when (offer.type) {
                OfferViewTypes.OfferText.ordinal ->
                    offerText = offer.`object` as OfferText
                else ->
                    offerImage = offer.`object` as OfferImage
            }

            val newOfferString = when (offer.type) {
                OfferViewTypes.OfferText.ordinal ->
                    offerText?.offerTitle + offerText?.offerPrice + offerText?.offerLocation + offerText?.offerDate
                else ->
                    offerImage?.offerTitle + offerImage?.offerPrice + offerImage?.offerLocation + offerImage?.offerDate
            }

            if (newOfferString !in oldOffersStrings) {
                offersToAdd.add(offer)
            }
        }

        return offersToAdd
    }

    private fun startMonitorOffersThread() {
        val itemsContentBuffer = ArrayList<ArrayList<Item>>()

        thread {
            while (true) {
                itemsContentBuffer.clear()
                val itemsContentTemp = itemsContent

                for (itemContentIndex in itemsContent.indices) {
                    itemsContentBuffer.add(
                        getOffers(
                            getHTML(
                                itemContentIndex,
                                this.getString(R.string.first_url_part)
                            )
                        )
                    )
                }

                for (itemContentBufferIndex in itemsContentBuffer.indices) {
                    if (itemContentBufferIndex > itemsContentTemp.size - 1) break

                    val offersToAdd = getOffersToAdd(
                        itemsContentTemp[itemContentBufferIndex],
                        itemsContentBuffer[itemContentBufferIndex]
                    ).reversed()

                    for (offer in offersToAdd) {
                        itemsContent[itemContentBufferIndex].add(0, offer)
                        offersFragmentCallbacks?.updateOffers(itemContentBufferIndex)
                    }

                    if (offersToAdd.isNotEmpty()) {
                        ItemsFragment().setItemImportant(itemContentBufferIndex)

                        if (getForegroundApp() != "com.lerabytes.olxmonitor:olxmonitor") {
                            val itemTitle: String
                            val itemName: String

                            when (itemsFragment[itemContentBufferIndex].type) {
                                ItemViewTypes.ItemTitle.ordinal -> {
                                    itemTitle =
                                        (itemsFragment[itemContentBufferIndex].`object` as ItemTitle).itemTitle
                                    createNotification(
                                        "Found new offers in $itemTitle!",
                                        "",
                                        itemContentBufferIndex
                                    )
                                }
                                else -> {
                                    itemName =
                                        (itemsFragment[itemContentBufferIndex].`object` as ItemLink).itemName
                                    createNotification(
                                        "Found new offers in $itemName!",
                                        "",
                                        itemContentBufferIndex
                                    )
                                }
                            }
                        }
                    }
                }

                Thread.sleep(5000)
            }
        }
    }
}