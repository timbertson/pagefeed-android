package net.gfxmonk.android.pagefeed
 
import _root_.android.app.ListActivity
import _root_.android.content.Intent
import _root_.android.content.IntentFilter
import _root_.android.net.Uri
import _root_.android.os.Bundle
import _root_.android.view.View
import _root_.android.view.Menu
import _root_.android.widget.TextView
import _root_.android.widget.ListView
import _root_.android.widget.SimpleCursorAdapter
import _root_.android.graphics.drawable.shapes.OvalShape
import _root_.android.graphics.drawable.ShapeDrawable
import _root_.android.content.Context
import _root_.android.database.Cursor
import _root_.android.widget.ResourceCursorAdapter
import _root_.android.view.ContextMenu
import _root_.android.view.MenuItem
import _root_.android.view.ContextMenu.ContextMenuInfo
import _root_.android.widget.AdapterView.AdapterContextMenuInfo
 
class MainActivity extends ListActivity {
	var urlStore:UrlStore = null
	var cursor:Cursor = null
	var adapter: SimpleCursorAdapter = null
	var broadcastReceiver:CallbackReceiver = null

	override def onStart() = {
		super.onStart()
		if (account == null) {
			val intent = new Intent(this, classOf[AccountList])
			startActivity(intent)
		} else {
			// add "not auto sync" alert
		}

		urlStore = new UrlStore(this)
		var cls = classOf[PagefeedProvider] // ack! stop proguard from stripping this class!

		listenForSync()

		setContentView(R.layout.url_list);
		cursor = urlStore.active().cursor
		adapter = new SimpleCursorAdapter(
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


		registerForContextMenu(getListView())
		setListAdapter(adapter)
	}

	private def account = {
		AccountList.singleEnabledAccount(getApplicationContext())
	}

	override def onCreateContextMenu(menu: ContextMenu, v:View, info:ContextMenuInfo) = {
		super.onCreateContextMenu(menu, v, info);
		getMenuInflater().inflate(R.menu.url_context_menu, menu)
		true
	}

	override def onContextItemSelected(item: MenuItem):Boolean = {
		val info = item.getMenuInfo().asInstanceOf[AdapterContextMenuInfo]
		item.getItemId() match {
			case R.id.delete_item => {
				deleteItemAt(info.position)
				true
			}
			case _ =>
				super.onContextItemSelected(item)
		}
	}

	override def onCreateOptionsMenu(menu:Menu) = {
		getMenuInflater().inflate(R.menu.main_menu, menu)
		true
	}

	override def onOptionsItemSelected(item:MenuItem):Boolean = {
		val info = item.getMenuInfo().asInstanceOf[AdapterContextMenuInfo]
		item.getItemId() match {
			case R.id.sync_now => {
				AccountList.syncNow(account, getApplicationContext())
				true
			}
			case R.id.sync_settings => {
				startActivity(new Intent().setClassName("com.android.providers.subscribedfeeds", "com.android.settings.ManageAccountsSettings"))
				true
			}
			case _ =>
				super.onContextItemSelected(item)
		}
	}

	private def itemAt(position: Int):String = {
		cursor.moveToPosition(position)
		cursor.getString(UrlStore.indexOf(UrlStore.URL))
	}

	private def deleteItemAt(position: Int) = {
		Util.info("deleting URL: " + cursor.getString(UrlStore.indexOf(UrlStore.URL)))
		urlStore.markDeleted(itemAt(position))
		refresh()
	}

	private def refresh() = {
		Util.info("refreshing list view...")
		cursor.requery()
		adapter.notifyDataSetChanged()
	}

	private def refreshAll() = {
		refresh()
		updateSyncInfo()
	}

	private def updateSyncInfo() = {
		//TODO
	}

	private def listenForSync() = {
		broadcastReceiver = new CallbackReceiver(this.refreshAll _)
		registerReceiver(broadcastReceiver, new IntentFilter(Contract.ACTION_SYNC_COMPLETE))
	}
	private def stopListeningForSync() = {
		unregisterReceiver(broadcastReceiver)
		broadcastReceiver = null
	}

	override def onListItemClick(list: ListView, view: View, position: Int, id: Long) = {
		var url = itemAt(position)
		Util.info("launching URL: " + cursor.getString(UrlStore.indexOf(UrlStore.URL)))
		val intent = new Intent(Intent.ACTION_VIEW)
		intent.setData(Uri.parse(url))
		startActivity(intent)
	}

	override def onStop() = {
		super.onStop()
		stopListeningForSync()
		urlStore.close()
	}
}

