package net.gfxmonk.android.pagefeed
 
import _root_.android.app.Service
import _root_.android.content.Context
import _root_.android.content.Intent
import _root_.android.net.Uri
import _root_.android.os.Binder
 
class PagefeedService extends Service {
	var _store:Option[UrlStore] = None

	override def onStartCommand(intent: Intent, flags: Int, id: Int):Int = {
		intent.getAction match {
			case Intent.ACTION_SEND => {
				try {
					add(intent.getData.toString)
				} finally {
					cleanup()
				}
			}
			case _ => invalidIntent()
		}
		return Service.START_NOT_STICKY
	}

	override def onBind(intent: Intent) = {
		var self = this
		new ServiceBinder[PagefeedService] {
			override def service = self
		}
	}

	def has(url:String) = store hasActive url

	def add(uri: String) = {
		attempt(store.add(uri), "Page added!")
	}

	def remove(url: String) = {
		attempt(store.markDeleted(url), "Page REMOVED!")
	}

	private def attempt(proc: =>Unit, desc:String) {
		try {
			proc
			Util.toast(desc, getApplicationContext)
		} catch {
			case e => Util.toast("Error: " + e, getApplicationContext)
		}
	}

	private def invalidIntent() = {
		Util.toast("invalid intent", getApplicationContext())
	}

	override def onDestroy() = cleanup()

	private def store:UrlStore = {
		if(_store.isEmpty) {
			_store = Some(new UrlStore(this))
		}
		_store.get
	}

	private def cleanup() = {
		Util.info("pagefeedservice: cleanup()")
		_store.map(_.close)
		_store = None
	}

}

abstract class ServiceBinder[T] extends Binder {
	def service:T
}

