package net.gfxmonk.android.pagefeed
 
import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.content.Context
import _root_.android.content.Intent
import _root_.android.net.Uri
import _root_.android.content.ComponentName
import _root_.android.content.ServiceConnection
import _root_.android.os.IBinder
 
class ShareLink extends Activity {
	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		val intent = getIntent
		val action = intent.getAction
		val url = intent.getExtras.getString(Intent.EXTRA_TEXT);
		Util.info("url = " + url)
		if (url == null || !url.startsWith("http")) {
			Util.toast("invalid URL", getApplicationContext())
		} else {
			val shareIntent = new Intent()
			shareIntent.setClass(this, classOf[PagefeedService])
			val connection:SimpleServiceBinder = new SimpleServiceBinder ( (service, conn) =>
				{
					if(service has url) {
						service.remove(url)
					} else {
						service.add(url)
					}
					this.unbindService(conn)
					this.finish()
				}
			)
			bindService(shareIntent, connection, Context.BIND_AUTO_CREATE)
		}
	}
}

class SimpleServiceBinder(proc:((PagefeedService,ServiceConnection)=>Unit)) extends ServiceConnection {
	override def onServiceConnected(name:ComponentName, service:IBinder) = {
		proc(service.asInstanceOf[ServiceBinder[PagefeedService]].service, this)
	}
	override def onServiceDisconnected(name:ComponentName) = {}
}
