package net.gfxmonk.android.pagefeed
 
import _root_.android.content.Context
import scala.collection.mutable.Map
import _root_.android.database.Cursor
import _root_.android.content.ContentValues
import _root_.android.net.Uri


object UrlStore {
	private val Data = Contract.Data
	val TRUE = 1
	val FALSE = 0
	val ATTRIBUTES = Array(Data.ID, Data.URL, Data.ACTIVE, Data.DIRTY, Data.TITLE, Data.BODY, Data.SCROLL)
	def indexOf(attr:String) = {
		val index = UrlStore.ATTRIBUTES.indexOf(attr)
		if(index < 0) { throw new RuntimeException("no such field: " + attr) }
		index
	}
}

class UrlStore (context: Context) {
	import UrlStore._
	import Contract.Data._

	def active() = {
		get(ACTIVE + " = 1")
	}

	def emptyPages() = {
		get(BODY + """ is null or """ + BODY + """ = "" """)
	}

	def db = {
		context.getContentResolver()
	}

	def all() = {
		get(null)
	}

	def add(u:Url):Unit = {
		val values = new ContentValues()
		values.put(URL, u.url)
		values.put(DIRTY, u.dirty)
		values.put(ACTIVE, u.active)
		values.put(TITLE, u.title)
		try {
			val result: Uri = db.insert(u.contentUri, values)
			assert(result != null)
		} catch {
			case _:AssertionError => {
				Util.info("updating instead of adding - should this be possible?")
				db.update(u.contentUri, values, URL + " = ?", List(u.url).toArray)
			}
		}
		Util.info("inserted " + u + " into local DB")
	}

	def hasActive(u:String) = {
		val cursor = db.query(Contract.ContentUri.forPage(u), List(ID).toArray, ACTIVE + " = 1", null, null)
		val result = cursor.getCount() > 0
		cursor.close()
		result
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
		update(item.contentUri, DIRTY -> FALSE)
	}

	def markDeleted(url:String) = {
		Util.info("marking item as deleted (locally):" + url)
		update(Contract.ContentUri.forPage(url), ACTIVE -> FALSE, DIRTY -> TRUE)
	}

	def purge(item:Url) = {
		Util.info("purging item locally: " + item)
		db.delete(item.contentUri, null, null)
	}

	def update(u:Url) {
		// NOTE: only updates what can change (so far).
		update(u.contentUri, TITLE -> u.title, BODY -> u.body)
	}

	private def update(contentUri:Uri, params:Tuple2[String,Any]*) = {
		val values = new ContentValues()
		for ((k,v) <- params) {
			v match {
				case v:String => values.put(k, v)
				case v:Int => values.put(k, v)
				case v => throw new RuntimeException("invalid data type!" + v)
			}
		}
		db.update(contentUri, values, null, null)
	}

	private def get(cond: String) = {
		new UrlSet(db.query(Contract.ContentUri.PAGES, ATTRIBUTES, cond, Array(), Contract.Data.ID))
	}
}

class UrlSet(var cursor:Cursor) {
	import UrlStore._
	import Contract.Data._

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

