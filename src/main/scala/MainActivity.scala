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
import _root_.android.widget.SimpleAdapter
import _root_.android.graphics.drawable.shapes.OvalShape
import _root_.android.graphics.drawable.ShapeDrawable
import _root_.android.content.Context
import _root_.android.database.Cursor
import _root_.android.widget.ResourceCursorAdapter
import _root_.android.view.ContextMenu
import _root_.android.view.MenuItem
import _root_.android.text.format.DateUtils
import _root_.android.view.ContextMenu.ContextMenuInfo
import _root_.android.widget.AdapterView.AdapterContextMenuInfo

protected class Selection() {
	var _idx:Option[Int] = None
	def set(idx:Int) =     { _idx = Some(idx) }
	def clear() =          { _idx = None }
	def index:Option[Int] = _idx
}
 
class MainActivity extends ListActivity {
	var urlStore:UrlStore = null
	var cursor:Cursor = null
	var adapter: SimpleCursorAdapter = null
	var broadcastReceiver:CallbackReceiver = null
	var syncDescriptionView:TextView = null
	var activeSelection = new Selection()

	override def onCreate(bundle:Bundle) = {
		super.onCreate(bundle)
		activeSelection.clear()
		val cls = classOf[PagefeedProvider]
		startAccountSelectorIfNecessary()
	}

	override def onStart() = {
		super.onStart()
		urlStore = new UrlStore(this)

		// init views
		setContentView(R.layout.url_list);
		syncDescriptionView = findViewById(R.id.last_sync).asInstanceOf[TextView]

		// setup listeners
		listenForSync()
		registerForContextMenu(getListView())

		// and the main data source
		adapter = initAdapter()
		setListAdapter(adapter)

		activeSelection.index map { idx =>
			val view = findViewById(_root_.android.R.id.list)
			if (view != null) {
				Util.info("selection set to: " + idx)
				val listView = view.asInstanceOf[ListView]
				listView.setSelection(idx)
			}
		}
		activeSelection.clear()
	}

	private def startAccountSelectorIfNecessary() = {
		if (account == null) {
			val intent = new Intent(this, classOf[AccountList])
			startActivity(intent)
		}
	}

	private def account = {
		AccountList.singleEnabledAccount(getApplicationContext())
	}

	private def initAdapter() = {
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
		adapter
	}

	private def updateSyncDescription() = {
		val lastSyncTimeMillis = Util.prefLong(getApplicationContext(), SyncProgress.PREFERENCE_LAST_SYNC, 0)
		var timeDesc = "unknown"
		var desc = "last sync: "
		Util.info("last sync time = " + lastSyncTimeMillis)
		if(lastSyncTimeMillis > 0) {
			timeDesc = DateUtils.formatDateTime(this, lastSyncTimeMillis,
				DateUtils.FORMAT_NO_YEAR |
				DateUtils.FORMAT_SHOW_TIME |
				DateUtils.FORMAT_ABBREV_MONTH)
		}
		desc += timeDesc
		if(account == null || !(AccountList.isAutoSync(account))) {
			desc += " [sync disabled]"
		}
		syncDescriptionView.setText(desc)
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

	override def onResume() = {
		super.onResume()
		updateSyncDescription()
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
				val intent = new Intent()
				intent.setClassName("com.android.providers.subscribedfeeds", "com.android.settings.ManageAccountsSettings")
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				startActivity(intent)
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
		updateSyncDescription()
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
		activeSelection.set(position)
		startActivity(intent)
	}

	override def onStop() = {
		super.onStop()
		stopListeningForSync()
		urlStore.close()
	}
}

