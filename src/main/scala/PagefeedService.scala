package net.gfxmonk.android.pagefeed
 
import _root_.android.app.Service
import _root_.android.content.Context
import _root_.android.content.Intent
import _root_.android.net.Uri
 
class PagefeedService extends Service {
	override def onStartCommand(intent: Intent, flags: Int, id: Int):Int = {
		intent.getAction match {
			case Intent.ACTION_SEND => add(intent.getData)
			case Intent.ACTION_SYNC => sync()
			case _ => invalidIntent()
		}
		return Service.START_NOT_STICKY
	}

	override def onBind(intent: Intent) = {
		// noop
		null
	}

	private def add(url: Uri) = {
	}

	private def sync() = {
	}

	private def invalidIntent() = {
	}

}
