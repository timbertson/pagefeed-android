package net.gfxmonk.android.pagefeed
 
import _root_.android.content.Context
import scala.io.Source
import scala.collection.mutable.Map
import java.io.FileNotFoundException
import java.util.logging.Logger
import _root_.android.database.Cursor
import _root_.android.database.sqlite.SQLiteDatabase
import _root_.android.database.sqlite.SQLiteDatabase.CursorFactory
import _root_.android.database.sqlite.SQLiteOpenHelper
import _root_.android.content.ContentValues
import _root_.android.net.Uri


object UrlStore {
	val name = "pagefeed"
	val version = 1
	val table_name = "url"
	val URL = "url"
	val DIRTY = "dirty"
	val ACTIVE = "active"
	val ID = "_id"
	val ATTRIBUTES = Array(UrlStore.ID, UrlStore.URL, UrlStore.ACTIVE, UrlStore.DIRTY)
}

class UrlStore (context: Context) extends
	SQLiteOpenHelper(context, UrlStore.name, null, UrlStore.version) {

	type Callback = (Url)=>Unit
	private def db = getWritableDatabase()
	val cls = UrlStore
	val log = Logger.getAnonymousLogger()

	def active() = {
		get(UrlStore.ACTIVE + " = 1")
	}

	def add(u:String) = {
		val values = new ContentValues()
		values.put(cls.URL, u)
		values.put(cls.DIRTY, true)
		values.put(cls.ACTIVE, true)
		db.insert(cls.table_name, null, values)
	}

	def add(u:Uri):Unit = add(u.toString)

	def eachDirty(block: Callback) = {
		iterateOverCursor(get(UrlStore.DIRTY + " = 1"), block)
	}

	private def get(cond: String) = {
		db.query(cls.table_name, UrlStore.ATTRIBUTES, cond, Array(), null, null, null)
	}

	private def iterateOverCursor(c:Cursor, block:Callback) = {
		val map = Map[String,Int]()
		val indexes = UrlStore.ATTRIBUTES.map(attr => map.put(attr, c.getColumnIndexOrThrow(attr)))
		while(! c.isAfterLast) {
			var url = c.getString(map.get(UrlStore.URL).get)
			var dirty = c.getInt(map.get(UrlStore.DIRTY).get) == 1
			var active = c.getInt(map.get(UrlStore.ACTIVE).get) == 1
			block(new Url(url, dirty, active))
		}
	}

	// --- db bookkeeping

	override def onCreate(db:SQLiteDatabase) = {
		log.info("creating table!")
		db.execSQL("create table url (" +
			"_id integer primary key, " +
			"url text not null, " +
			"dirty boolean, " +
			"active boolean default 1" +
		");")
	}

	override def onUpgrade(db:SQLiteDatabase, old_version:Int, new_version:Int) = {
		// noop (yet)
	}


}
