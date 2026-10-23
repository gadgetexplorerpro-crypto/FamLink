package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (
      intent?.action == Intent.ACTION_BOOT_COMPLETED ||
      intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
    ) {
      Log.d("BootReceiver", "Device rebooted or package updated. Checking location sharing preference...")
      val wasEnabled = FamLinkLocationService.isEnabled(context)
      if (wasEnabled) {
        Log.i("BootReceiver", "Restarting FamLink location foreground service...")
        FamLinkLocationService.start(context)
      }
    }
  }
}
