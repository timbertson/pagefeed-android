package net.gfxmonk.android.pagefeed
import java.util.logging.Logger
import _root_.android.content.Context
import _root_.android.widget.Toast

object Util {
	def info(s:String) = Logger.getLogger("net.gfxmonk.android.pagefeed").info(s)
	def toast(text:String, ctx:Context) = {
		Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
	}
	def prefLong(ctx:Context, key:String, default:Long) = {
		ctx.getSharedPreferences(
			classOf[MainActivity].getName(),
			Context.MODE_PRIVATE
		).getLong(key, default)
	}
}
