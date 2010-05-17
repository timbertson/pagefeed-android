package net.gfxmonk.android.pagefeed
 
import _root_.android.content.BroadcastReceiver
import _root_.android.content.Context
import _root_.android.content.Intent
import net.gfxmonk.android.pagefeed.PagefeedService
 
class IntentHandler extends BroadcastReceiver {
	override def onReceive(ctx: Context, intent: Intent):Unit = {
		val action = intent.getAction
		// TODO: ignore unexpected actions
		val shareIntent = new Intent(Intent.ACTION_SEND, intent.getData)
		shareIntent.setClass(ctx, classOf[PagefeedService])
		ctx.startService(shareIntent)
		return
	}
}
