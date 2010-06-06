package net.gfxmonk.android.pagefeed

import _root_.android.content.Intent
import _root_.android.app.PendingIntent
import _root_.android.content.BroadcastReceiver
import _root_.android.content.Context
import _root_.android.app.NotificationManager
import _root_.android.app.Notification

object SyncProgress {
	val SYNC_IN_PROGRESS = 1
	val PREFERENCE_LAST_SYNC = "lastSyncDate"
}

class SyncProgress(ctx:Context) {
	import SyncProgress._
	def start() = {
		val note = new Notification(R.drawable.notification, ctx.getString(R.string.sync_running), System.currentTimeMillis())
		note.flags |= Notification.FLAG_ONGOING_EVENT
		val intent = new Intent(ctx, classOf[MainActivity])
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		val pendingIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
		note.setLatestEventInfo(ctx, ctx.getString(R.string.app_name), ctx.getString(R.string.sync_running), pendingIntent);
		notificationService.notify(SYNC_IN_PROGRESS, note)
	}

	def finish(success:Boolean) = {
		Util.info("sync finished with success = " + success)
		ctx.sendBroadcast(new Intent(Contract.ACTION_SYNC_COMPLETE))
		notificationService.cancel(SYNC_IN_PROGRESS)
		if(success) {
			val editor = ctx.getSharedPreferences(classOf[MainActivity].getName(), Context.MODE_PRIVATE).edit()
			editor.putLong(PREFERENCE_LAST_SYNC, System.currentTimeMillis())
			Util.info("saved pref " + PREFERENCE_LAST_SYNC + " = " + System.currentTimeMillis())
			editor.commit()
		}
	}

	private def notificationService = {
		ctx.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
	}
}

class CallbackReceiver(callback:()=>Unit) extends BroadcastReceiver {
	override def onReceive(ctx:Context, intent:Intent):Unit = {
		callback()
	}
}

