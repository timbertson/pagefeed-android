package net.gfxmonk.android.pagefeed
 
import _root_.android.content.Context
import scala.io.Source
import java.io.FileNotFoundException
import java.util.logging.Logger
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
}

class UrlStore (context: Context) extends
	SQLiteOpenHelper(context, UrlStore.name, null, UrlStore.version)
{

	private def db = getWritableDatabase()
	val cls = UrlStore
	val log = Logger.getAnonymousLogger()

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

	def active() = {
		db.query(cls.table_name, Array(UrlStore.ID, UrlStore.URL, UrlStore.DIRTY), "active = 1", Array(), null, null, null)
	}

	def add(u:String) = {
		val values = new ContentValues()
		values.put(cls.URL, u)
		values.put(cls.DIRTY, true)
		values.put(cls.ACTIVE, true)
		db.insert(cls.table_name, null, values)
	}

	def add(u:Uri):Unit = add(u.toString)

}
