package net.gfxmonk.android.pagefeed
 
import _root_.android.app.ListActivity
import _root_.android.content.Intent
import _root_.android.net.Uri
import _root_.android.os.Bundle
import _root_.android.view.View
import _root_.android.widget.TextView
import _root_.android.widget.ListView
import _root_.android.widget.SimpleCursorAdapter
import _root_.android.graphics.drawable.shapes.OvalShape
import _root_.android.graphics.drawable.ShapeDrawable
import _root_.android.content.Context
import _root_.android.database.Cursor
import _root_.android.widget.ResourceCursorAdapter
 
class MainActivity extends ListActivity {
	var urlStore:UrlStore = null
	var cursor:Cursor = null

	override def onStart() = {
		super.onStart()
		if(! AccountList.hasEnabledAccount(getApplicationContext())) {
			val intent = new Intent()
			intent.setClass(this, classOf[AccountList])
			startActivity(intent)
		}

		urlStore = new UrlStore(this)
		var cls = classOf[PagefeedProvider] // ack! stop proguard from stripping this class!

		setContentView(R.layout.url_list);
		cursor = urlStore.active().cursor
		val adapter = new SimpleCursorAdapter(
			this,
			R.layout.url_item,
			cursor,
			Array(UrlStore.URL, UrlStore.DIRTY),
			Array(R.id.url, R.id.sync_state)
		)
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			var dirtyIndex = UrlStore.indexOf(UrlStore.DIRTY)
			override def setViewValue(view: View, cursor: Cursor, columnIndex: Int):Boolean = {
				if(columnIndex == dirtyIndex) {
					val dirtyIndicator = view.asInstanceOf[View]
					val dirty = cursor.getInt(dirtyIndex) > 0
					val bg = if (dirty) R.drawable.ring else R.drawable.circle
					dirtyIndicator.setBackgroundResource(bg)
					true
				} else {
					false
				}
			}
		})

		setListAdapter(adapter);
	}

	override def onListItemClick(list: ListView, view: View, pos: Int, id: Long) = {
		cursor.moveToPosition(pos)
		Util.info("launching URL: " + cursor.getString(UrlStore.indexOf(UrlStore.URL)))
		var url = cursor.getString(UrlStore.indexOf(UrlStore.URL))
		val intent = new Intent(Intent.ACTION_VIEW)
		intent.setData(Uri.parse(url))
		startActivity(intent)
	}

	override def onStop() = {
		super.onStop()
		urlStore.close()
	}

	def isSyncEnabled() = {
		// TODO: something like this, and let the user know if it's not enabled
		// also, get the last sync state perhaps?
		/*val am = AccountManager.get(this)*/
		/*val accounts = am.getAccountsByType("com.google")*/
		/*ContentResolver.setIsSyncable(accounts[0], ContactsContract.AUTHORITY, 1)*/
		/*ContentResolver.setSyncAutomatically(accounts[0], ContactsContract.AUTHORITY, true)*/
	}

}
