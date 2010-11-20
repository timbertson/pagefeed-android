package net.gfxmonk.android.pagefeed
import _root_.android.net.Uri

object Contract {
	val AUTHORITY = "net.gfxmonk.android.pagefeed"
	val ACCOUNT_TYPE = "com.google"
	val ACTION_SYNC_COMPLETE = "net.gfxmonk.android.pagefeed.broadcast.SYNC_COMPLETE"

	object Data {
		val URL = "url"
		val DIRTY = "dirty"
		val ACTIVE = "active"
		val TITLE= "title"
		val BODY = "body"
		val SCROLL = "scroll"
		val ID = "_id"
	}

	object ContentUri {
		private val BASE = Uri.parse("content://" + AUTHORITY + "/")
		val PAGES = BASE.buildUpon().appendPath("pages").build()

		private val PAGE = "page"
		def forPage(pageUrl: String) = {
			BASE.buildUpon().appendPath(PAGE).appendPath(pageUrl).build()
		}
	}
}
