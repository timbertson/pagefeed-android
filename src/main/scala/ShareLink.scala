package net.gfxmonk.android.pagefeed
 
import _root_.android.app.Activity
import _root_.android.app.ListActivity
import _root_.android.widget.ArrayAdapter
import _root_.android.os.Bundle
import _root_.android.content.Context
import _root_.android.content.Intent
import _root_.android.net.Uri
import _root_.android.view.View
import _root_.android.widget.ListView
import _root_.android.content.ComponentName
import _root_.android.content.ServiceConnection
import _root_.android.os.IBinder
import _root_.android.app.AlertDialog
import _root_.android.content.DialogInterface
import _root_.android.content.DialogInterface.OnClickListener
import _root_.android.content.DialogInterface.OnCancelListener
 
class ShareLink extends ListActivity {
	var url:String = null

	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		val intent = getIntent
		val action = intent.getAction
		url = intent.getExtras.getString(Intent.EXTRA_TEXT);
		Util.info("url = " + url)
		if (url == null || !url.startsWith("http")) {
			Util.toast("invalid URL", getApplicationContext())
		} else {
			val self = this
			withService { service =>
				if(service has url) {
					self.populateDialog()
				} else {
					service.add(url)
					self.finish()
				}
			}
		}
	}

	def populateDialog() {
		val listItems = getResources().getStringArray(R.array.url_actions)
		setListAdapter(new ArrayAdapter(this, R.layout.list_item, listItems))
		setContentView(R.layout.list_view)
		val activity = this;
	}

	override def onListItemClick(list: ListView, view: View, position: Int, id: Long) = {
		val doRemove = position == 0
		Util.info("on save dialog, clicked item " + position + ". " + (if(doRemove) "removing" else "keeping") + ".")
		if (doRemove) {
			withService { service =>
				service.remove(url)
			}
		}
		this.finish()
	}

	private def withService(proc:(PagefeedService)=>Unit) {
		val shareIntent = new Intent()
		shareIntent.setClass(this, classOf[PagefeedService])
		val connection:SimpleServiceBinder = new SimpleServiceBinder ( (service, conn) =>
			try {
				proc(service)
			} finally {
				this.unbindService(conn)
			}
		)
		bindService(shareIntent, connection, Context.BIND_AUTO_CREATE)
	}

}

class SimpleServiceBinder(proc:((PagefeedService,ServiceConnection)=>Unit)) extends ServiceConnection {
	override def onServiceConnected(name:ComponentName, service:IBinder) = {
		proc(service.asInstanceOf[ServiceBinder[PagefeedService]].service, this)
	}
	override def onServiceDisconnected(name:ComponentName) = {}
}
