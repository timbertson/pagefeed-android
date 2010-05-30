package net.gfxmonk.android.pagefeed
 
import _root_.android.app.ListActivity
import _root_.android.os.Bundle
import _root_.android.widget.TextView
import _root_.android.widget.ListView
import _root_.android.widget.SimpleCursorAdapter
 
class MainActivity extends ListActivity {
	var urlStore:UrlStore = null

	override def onStart() = {
		super.onStart()
		urlStore = new UrlStore(this)

		setContentView(R.layout.url_list);
		setListAdapter(new SimpleCursorAdapter(
			this,
			R.layout.url_item,
			urlStore.active().cursor,
			Array(UrlStore.URL, UrlStore.DIRTY),
			Array(R.id.url, R.id.sync_state)
		));
	}

	override def onStop() = {
		super.onStop()
		urlStore.close()
	}

	def isSyncEnabled() = {
		// something like this:
		/*val am = AccountManager.get(this)*/
		/*val accounts = am.getAccountsByType("com.google")*/
		/*ContentResolver.setIsSyncable(accounts[0], ContactsContract.AUTHORITY, 1)*/
		/*ContentResolver.setSyncAutomatically(accounts[0], ContactsContract.AUTHORITY, true)*/
	}

}
