package net.gfxmonk.android.pagefeed
import _root_.android.text.TextUtils
import _root_.android.database.Cursor

object Url {
	def remote(url:String, timestamp:Long, title:String, has_content:Boolean) = new Url(url, title, has_content, false, true, timestamp)
	def local(url:String) = new Url(url, null, false, true, true, 0)

	def fromCursorRow(cursor:Cursor, row:Int) = {
		cursor.moveToPosition(row)
		fromCursor(cursor)
	}

	def fromCursor(cursor:Cursor) = {
		val url = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Data.URL))
		val title = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Data.TITLE))
		val dirty = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Data.DIRTY)) > 0
		val active = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Data.ACTIVE)) > 0
		val body = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Data.BODY))
		val has_content = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Data.HAS_CONTENT)) > 0
		val timestamp = 0
		new Url(url, title, has_content, dirty, active, timestamp).withBody(body)
	}
}

class Url(var url: String, var title:String, var has_content:Boolean, var dirty:Boolean, var active:Boolean, var timestamp:Long) {
	var body:String = null
	override def toString = url + " - " + title
	def contentUri = Contract.ContentUri.forPage(url)
	def withBody(body:String) = {
		this.body = body
		this
	}
	def hasBody = { !TextUtils.isEmpty(body) }
}
