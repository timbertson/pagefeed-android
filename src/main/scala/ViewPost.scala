package net.gfxmonk.android.pagefeed

import _root_.android.app.Activity
import _root_.android.content.ContentValues
import _root_.android.database.Cursor
import _root_.android.net.Uri
import _root_.android.os.Bundle
import _root_.android.util.Log
import _root_.android.view.LayoutInflater
import _root_.android.view.ViewGroup
import _root_.android.webkit.WebView
import _root_.android.widget.TextView
import _root_.android.widget.ViewFlipper
import net.gfxmonk.android.reader.view.ResumePositionWebViewClient

class ViewPost extends Activity {
  val PROJECTION = List(Contract.Data.URL, Contract.Data.BODY)
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
		initData()
	}

	override def onStop() = {
		super.onStop()
		saveScrollPosition()
	}

	private def saveScrollPosition() = {
		val scrollRatio = webViewClient.getScrollRatio()

		if(scrollRatio != null) {
			var values = new ContentValues()
			Log.d(TAG, "Updating scroll ratio to " + scrollRatio + " for post " + getIntent().getData())
			values.put(Contract.Data.SCROLL, scrollRatio)
			getContentResolver().update(getIntent().getData(), values, null, null)
		}
	}

	private def initCursor()
	{
		val uri = getIntent().getData()
		Util.info("ViewPost querying for URI " + uri)
		cursor = managedQuery(uri, PROJECTION.toArray, null, null, null)
		Util.info("cursor returned " + cursor.getCount() + " rows");
		cursor.moveToNext()
	}

	private def initData() {
		var v = LayoutInflater.from(this).inflate(R.layout.post_view_item, null).asInstanceOf[ViewGroup]

		var postTitle = v.findViewById(R.id.post_view_title).asInstanceOf[TextView]
		webView = v.findViewById(R.id.post_view_text).asInstanceOf[WebView]

		val title = cursor.getString(cursor.getColumnIndex(Contract.Data.TITLE))
		postTitle.setText(title)

		val html =
		  "<html><head><style type=\"text/css\">body { background-color: #201c19; color: white; } a { color: #ddf; }</style></head><body>" +
		  getBody() +
		  "</body></html>"

		webView.loadData(html, "text/html", "utf-8")

		// add a delegate to resume the scroll position once the WebView renders its content
		val scrollPosition = cursor.getFloat(cursor.getColumnIndex(Contract.Data.SCROLL))

		webViewClient = new ResumePositionWebViewClient(scrollPosition, webView, v.findViewById(R.id.post_view_loading))
		webView.setWebViewClient(webViewClient)

		//add view to the flipper.
		flipper.addView(v)
	}

	private def getBody() {
		cursor.getString(cursor.getColumnIndex(Contract.Data.BODY))
	}

}
