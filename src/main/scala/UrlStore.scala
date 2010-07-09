package net.gfxmonk.android.pagefeed
 
import _root_.android.content.Context
import scala.collection.mutable.Map
import scala.collection.mutable.Queue
import java.util.logging.Logger
import _root_.android.database.Cursor
import _root_.android.database.sqlite.SQLiteDatabase
import _root_.android.database.sqlite.SQLiteDatabase.CursorFactory
import _root_.android.database.sqlite.SQLiteOpenHelper
import _root_.android.content.ContentValues
import _root_.android.net.Uri
import _root_.android.database.sqlite.SQLiteConstraintException


object UrlStore {
	val name = "pagefeed"
	val version = 2
	val tableName = "url"
	val URL = "url"
	val DIRTY = "dirty"
	val ACTIVE = "active"
	val TITLE= "title"
	val ID = "_id"
	val TRUE = 1
	val FALSE = 0
	val ATTRIBUTES = Array(UrlStore.ID, UrlStore.URL, UrlStore.ACTIVE, UrlStore.DIRTY, UrlStore.TITLE)
	def indexOf(attr:String) = {
		val index = UrlStore.ATTRIBUTES.indexOf(attr)
		if(index < 0) { throw new RuntimeException("no such field: " + attr) }
		index
	}
}

class UrlStore (context: Context) extends
	SQLiteOpenHelper(context, UrlStore.name, null, UrlStore.version) {
	import UrlStore._

	Util.info("UrlStore: created")
	var openCursors = new Queue[Cursor]()
	var _db:SQLiteDatabase = null

	private def db = {
		_db = getWritableDatabase()
		_db
	}

	def active() = {
		get(ACTIVE + " = 1")
	}

	def add(u:Url):Unit = {
		val values = new ContentValues()
		values.put(URL, u.url)
		values.put(DIRTY, u.dirty)
		values.put(ACTIVE, u.active)
		try {
			db.insert(tableName, null, values)
		} catch {
			case _:SQLiteConstraintException => {} // URL uniqueness - doesn't matter
		}
		Util.info("inserted " + u + " into local DB")
	}

	def add(u:String):Unit = {
		add(Url.local(u))
	}

	def add(u:Uri):Unit = {
		add(u.toString)
	}

	def dirty() = {
		get(DIRTY + " = 1")
	}

	def markClean(item:Url) = {
		assert(item.active, "a clean deleted item should be purged!")
		Util.info("marking item as clean:" + item)
		update(item.url, DIRTY -> FALSE)
	}

	def markDeleted(url:String) = {
		Util.info("marking item as deleted (locally):" + url)
		update(url, ACTIVE -> FALSE, DIRTY -> TRUE)
	}

	def purge(item:Url) = {
		Util.info("purging item locally: " + item)
		db.delete(tableName, URL + " = ?", List(item.url).toArray)
	}

	def update(u:Url) {
		// NOTE: only updates what can change (so far).
		update(u.url, TITLE -> u.title)
	}

	private def update(url:String, params:Tuple2[String,Any]*) = {
		val values = new ContentValues()
		for ((k,v) <- params) {
			v match {
				case v:String => values.put(k, v)
				case v:Int => values.put(k, v)
				case v => throw new RuntimeException("invalid data type!" + v)
			}
		}
		db.update(tableName, values, URL + " = ?", List(url).toArray)
	}

	override def close() = {
		super.close()
		for (cursor <- openCursors ) {
			if (!cursor.isClosed()) {
				Util.info("cursor::close()")
				cursor.close()
			}
		}
		openCursors = new Queue[Cursor]()
		if(_db != null) {
			_db.close()
		}
		Util.info("UrlStore::close()")
	}

	private def get(cond: String) = {
		val urlSet = new UrlSet(db.query(tableName, ATTRIBUTES, cond, Array(), null, null, null))
		openCursors.enqueue(urlSet.cursor)
		urlSet
	}

	// --- db bookkeeping

	override def onCreate(db:SQLiteDatabase) = {
		Util.info("creating table!")
		db.execSQL("create table url (" +
			"_id integer primary key, " +
			"url text unique not null, " +
			"dirty boolean, " +
			"active boolean default 1" +
			"date integer default 0" +
			"title text default 0" +
		");")
	}

	override def onUpgrade(db:SQLiteDatabase, old_version:Int, new_version:Int) = {
		val transitions = Map(
			2 -> "alter table url add column title text default '';"
		)
		for (i <- (old_version until new_version).map(_+1)) {
			Util.info("DB::Migrate[" + old_version + "->" + i + "] " + transitions(i))
			db.execSQL(transitions(i))
		}
	}

}

class UrlSet(var cursor:Cursor) {
	import UrlStore._

	private def elements = {
		val columnMap = Map[String,Int]()
		val indexes = ATTRIBUTES.map(attr => columnMap.put(attr, cursor.getColumnIndexOrThrow(attr)))
		cursor.moveToFirst()
		new Iterator[Url] {
			def hasNext = ! cursor.isAfterLast()
			def next = {
				var url = cursor.getString(columnMap.get(URL).get)
				var dirty = cursor.getInt(columnMap.get(DIRTY).get) == 1
				var active = cursor.getInt(columnMap.get(ACTIVE).get) == 1
				var title = cursor.getString(columnMap.get(TITLE).get)
				cursor.moveToNext()
				new Url(url, title, dirty, active, 0)
			}
		}
	}

	def map[T](mapper:(Url => T)) = toList.map(mapper)
	def foreach(mapper: (Url => Unit)) = toList.foreach(mapper)

	def toList = {
		val list = elements.toList
		cursor.close()
		cursor = null
		list
	}
}
