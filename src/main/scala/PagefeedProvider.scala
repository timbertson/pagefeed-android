package net.gfxmonk.android.pagefeed

import _root_.android.content.ContentProvider
import _root_.android.net.Uri
import _root_.android.content.ContentValues
import _root_.android.database.Cursor

class PagefeedProvider extends ContentProvider {
	override def onCreate() = {
		true
	}

	override def update(uri: Uri, values: ContentValues, selection: String, selectionArgs: Array[String]):Int = {
		// noop
		1
	}

	override def delete(uri: Uri, selection: String, selectionArgs: Array[String]):Int = {
		// noop
		1
	}

	override def insert(uri: Uri, values: ContentValues):Uri = {
		// noop
		null
	}

	override def getType(uri: Uri):String = {
		"vnd.android.cursor.item/vnd.pagefeed.url"
		/*"vnd.android.cursor.dir/vnd.pagefeed.url"*/
	}

	override def query(uri: Uri, projection: Array[String], selection:String, selectionArgs: Array[String], sortOrder:String):Cursor = {
		// noop
		null
	}

}
