package net.gfxmonk.android.pagefeed
 
import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.content.Context
import _root_.android.content.Intent
import _root_.android.net.Uri
 
class ShareLink extends Activity {
	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		val intent = getIntent
		val action = intent.getAction
		val url = intent.getExtras.getString(Intent.EXTRA_TEXT);
		Util.info("extra: " + url)
		if (url == null || !url.startsWith("http")) {
			Util.toast("invalid URL", getApplicationContext())
		} else {
			Util.toast("url = " + url.toString, getApplicationContext)
			val shareIntent = new Intent(Intent.ACTION_SEND, Uri.parse(url))
			shareIntent.setClass(this, classOf[PagefeedService])
			startService(shareIntent)
		}
		finish()
	}
}
