package net.gfxmonk.android.pagefeed
 
import _root_.android.app.ListActivity
import _root_.android.os.Bundle
import _root_.android.widget.TextView
import _root_.android.widget.ListView
import _root_.android.widget.SimpleCursorAdapter
import net.gfxmonk.android.pagefeed.UrlStore
 
class MainActivity extends ListActivity {

	override def onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)

		var urlStore = new UrlStore(this)

		setContentView(R.layout.url_list);
		setListAdapter(new SimpleCursorAdapter(
			this,
			R.layout.url_item,
			urlStore.active(),
			Array(UrlStore.URL, UrlStore.DIRTY),
			Array(R.id.url, R.id.sync_state)
		));
	}
}
