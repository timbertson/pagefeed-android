package net.gfxmonk.android.pagefeed

import _root_.android.app.Activity
import _root_.android.content.Intent
import _root_.android.content.ContentValues
import _root_.android.database.Cursor
import _root_.android.view.View
import _root_.android.view.Menu
import _root_.android.view.MenuItem
import _root_.android.net.Uri
import _root_.android.os.Bundle
import _root_.android.util.Log
import _root_.android.view.LayoutInflater
import _root_.android.view.ViewGroup
import _root_.android.webkit.WebView
import _root_.android.widget.TextView
import _root_.android.widget.ViewFlipper
import _root_.android.webkit.WebSettings
import _root_.android.webkit.WebSettings.ZoomDensity
import net.gfxmonk.android.reader.view.ResumePositionWebViewClient

class ViewPost extends Activity {
  val PROJECTION = List(Contract.Data.URL, Contract.Data.BODY, Contract.Data.TITLE, Contract.Data.SCROLL)
	var flipper: ViewFlipper = null
	var webViewClient: ResumePositionWebViewClient = null
	val TAG = "ViewPost"
	var cursor: Cursor = null
	var webView: WebView = null

	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.post_view)
		flipper = findViewById(R.id.post_flip).asInstanceOf[ViewFlipper]
	}

	override def onStart() = {
		super.onStart()
		initCursor()
		try {
			initData()
		} catch {
			case e:RuntimeException => {
				Log.e(TAG, "Error initialising ViewPost data", e)
				Util.toast("Error viewing page.", getApplicationContext())
				backToMain()
			}
		}
	}

	def backToMain() = {
		val intent = new Intent(this, classOf[MainActivity])
		startActivity(intent)
		finish()
	}

	override def onPause() = {
		super.onStop()
		saveScrollPosition()
	}

	override def onCreateOptionsMenu(menu:Menu) = {
		getMenuInflater().inflate(R.menu.url_context_menu, menu)
		getMenuInflater().inflate(R.menu.view_post_menu, menu)
		true
	}

	override def onOptionsItemSelected(item:MenuItem):Boolean = {
		val itemId = item.getItemId()
		if (itemId == R.id.load_images) {
			useNetwork()
			return true
		}
		val handled = new UrlActions(this).handleMenuItem(item, cursor.getString(cursor.getColumnIndexOrThrow(Contract.Data.URL)))
		if (itemId == R.id.delete_item) {
			backToMain()
		}
		handled
	}

	def useNetwork() = {
		webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
	}

	def disableNetworkUse() = {
		webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
	}

	private def saveScrollPosition() = {
		val scrollRatio = webViewClient.getScrollRatio()

		if(scrollRatio != null) {
			var values = new ContentValues()
			Log.d(TAG, "Updating scroll ratio to " + scrollRatio + " for post " + getIntent().getData())
			values.put(Contract.Data.SCROLL, scrollRatio)
			val itemUri = getIntent().getData()
			getContentResolver().update(itemUri, values, null, null)
		}
	}

	private def initCursor() = {
		val uri = getIntent().getData()
		cursor = managedQuery(uri, PROJECTION.toArray, null, null, null)
		cursor.moveToNext()
	}

	private def initData() = {
		var v = LayoutInflater.from(this).inflate(R.layout.post_view_item, null).asInstanceOf[ViewGroup]

		webView = v.findViewById(R.id.post_view_text).asInstanceOf[WebView]

		val title = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Data.TITLE))
		val body = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Data.BODY))

		val html =
		  "<html><head><style type=\"text/css\">body { background-color: #201c19 !important; color: white !important; } a { color: #ddf !important; } h1 h2 h3 { font-size:1em !important; }</style></head><body>" +
			"<h2>" + title + "</h2><br />" + body + "</body></html>"

		val encoding="utf-8"
		webView.loadDataWithBaseURL(null, html, "text/html", encoding, null)

		// add a delegate to resume the scroll position once the WebView renders its content
		val scrollPosition = cursor.getFloat(cursor.getColumnIndexOrThrow(Contract.Data.SCROLL))

		webViewClient = new ResumePositionWebViewClient(scrollPosition, webView, v.findViewById(R.id.post_view_loading), this)
		webView.setWebViewClient(webViewClient)
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY)

		//todo: paramaterise?
		webView.getSettings().setMinimumFontSize(20)
		disableNetworkUse();

		//add view to the flipper.
		flipper.addView(v)
	}
}
