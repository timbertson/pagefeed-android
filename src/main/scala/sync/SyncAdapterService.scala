package net.gfxmonk.android.pagefeed.sync

import _root_.android.app.Service
import _root_.android.content.Intent
import _root_.android.os.IBinder

/**
 * Service to handle Account sync. This is invoked with an intent with action
 * ACTION_AUTHENTICATOR_INTENT. It instantiates the syncadapter and returns its
 * IBinder.
 */
object SyncAdapterService {
	/*val lock = new Object()*/
	var adapter:SyncAdapter = null
}

class SyncAdapterService extends Service {
	override def onCreate() = SyncAdapterService synchronized {
		if (SyncAdapterService.adapter == null) {
			SyncAdapterService.adapter = new SyncAdapter(getApplicationContext(), true)
		}
	}

	override def onBind(intent: Intent) = SyncAdapterService.adapter.getSyncAdapterBinder()
}
