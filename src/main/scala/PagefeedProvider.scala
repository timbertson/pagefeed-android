package net.gfxmonk.android.pagefeed

import _root_.android.content.Context
import _root_.android.database.Cursor
import _root_.android.database.SQLException
import _root_.android.database.sqlite.SQLiteDatabase
import _root_.android.database.sqlite.SQLiteDatabase.CursorFactory
import _root_.android.database.sqlite.SQLiteOpenHelper
import _root_.android.content.ContentValues
import _root_.android.content.ContentProvider
import _root_.android.net.Uri


class PagefeedProvider extends ContentProvider {

	private var context:Context = null
	var db:UrlDb = null

	override def onCreate() = {
		context = getContext()
		db = new UrlDb(context)
		Util.info("Created content provider!")
		true
	}

	override def getType(uri: Uri):String = {
		uri match {
			case Contract.ContentUri.PAGES => "vnd.android.cursor.dir/vnd.pagefeed.url"
			case _ => "vnd.android.cursor.item/vnd.pagefeed.url"
		}
	}

	override def query(uri:Uri, projection: Array[String], selection: String, selectionArgs: Array[String], sortOrder: String) = {
		if(uri == Contract.ContentUri.PAGES) {
			db.query(projection, selection, selectionArgs, sortOrder)
		}
		else {
			db.queryForUri(uri, projection, selection, selectionArgs, sortOrder)
		}
	}

	override def insert(uri:Uri, values:ContentValues):Uri = {
		val inserted = db.insert(uri, values)
		notifyChange(uri)
		return inserted
	}

	override def delete(uri:Uri, selection: String, selectionArgs: Array[String]): Int = {
		assert(selection == null && selectionArgs == null)
		val deleted = db.delete(uri)
		notifyChange(uri)
		return deleted
	}

	override def update(uri:Uri, values:ContentValues, selection: String, selectionArgs: Array[String]): Int = {
		assert(selection == null && selectionArgs == null)
		val updated = db.update(uri, values)
		notifyChange(uri)
		return updated
	}

	private def notifyChange(uri:Uri) = {
		getContext().getContentResolver().notifyChange(uri, null)
	}
}





object UrlDb {
	val name = "pagefeed"
	val version = 5
	val tableName = "url"
}

class UrlDb (context: Context) extends SQLiteOpenHelper(context, UrlDb.name, null, UrlDb.version) {
	import UrlDb._
	import Contract.Data._
	var _db:Option[SQLiteDatabase] = None

	private def db[Result](func: (SQLiteDatabase)=>Result):Result = {
		_db = Some(getWritableDatabase())
		val db = _db.get
		db.synchronized {
			val r: Result = func(db)
			return r
		}
	}

	def query(projection: Array[String], selection: String, selectionArgs: Array[String], sortOrder: String) = {
		db(_.query(tableName, projection, selection, selectionArgs, null, null, sortOrder))
	}

	def queryForUri(uri:Uri, projection: Array[String], selection: String, selectionArgs: Array[String], sortOrder:String) = {
		val scopedSelection = (if(selection == null) "" else (selection + " and ")) + URL + " = ?"
		var args = selectionArgs match {
			case null => List[String]()
			case _ => selectionArgs.toList.asInstanceOf[List[String]]
		}
		args = args  ++ List(uri.getLastPathSegment())
		query(projection, scopedSelection, args.toArray, sortOrder)
	}

	def delete(uri: Uri) = {
		Util.info("deleting url " + uri.getLastPathSegment())
		db(_.delete(tableName, URL + " = ?", List(uri.getLastPathSegment()).toArray))
	}

	def insert(uri: Uri, values:ContentValues): Uri = {
		try {
			val insertedId = db(_.insertOrThrow(tableName, null, values))
			if(insertedId < 0) {
				return null
			}
			return uri
		} catch {
			case _:SQLException => return null
		}
	}

	def update(uri:Uri, values: ContentValues) = {
		db(_.update(tableName, values, URL + " = ?", List(uri.getLastPathSegment()).toArray))
	}

	// --- db bookkeeping

	override def onCreate(db:SQLiteDatabase) = {
		Util.info("creating table!")
		db.execSQL("create table url (" +
			"_id integer primary key" +
			", url text unique not null" +
			", dirty boolean" +
			", active boolean default 1" +
			", date integer default 0" +
			""", title text default "" """ +
			""", body text default null """ +
			""", scroll number default 0 """ +
			""", has_content boolean default 0 """ +
		");")
	}

	override def onUpgrade(db:SQLiteDatabase, old_version:Int, new_version:Int) = {
		val transitions = Map(
			  2 -> """alter table url add column title text default "";"""
			, 3 -> """alter table url add column body text default null;"""
			, 4 -> """alter table url add column scroll number default 0;"""
			, 5 -> """alter table url add column has_content boolean default 0;"""
		)
		for (i <- (old_version until new_version).map(_+1)) {
			Util.info("DB::Migrate[" + old_version + "->" + i + "] " + transitions(i))
			db.execSQL(transitions(i))
		}
	}
}

