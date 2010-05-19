package net.gfxmonk.android.pagefeed
 
import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.content.Context
import _root_.android.content.Intent
import net.gfxmonk.android.pagefeed.PagefeedService
 
class ShareLink extends Activity {
	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		val intent = getIntent
		val action = intent.getAction
		// TODO: ignore unexpected actions
		val shareIntent = new Intent(Intent.ACTION_SEND, intent.getData)
		shareIntent.setClass(this, classOf[PagefeedService])
		startService(shareIntent)
		finish()
	}
}
