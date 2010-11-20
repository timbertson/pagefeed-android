package net.gfxmonk.android.pagefeed

object Url {
	def remote(url:String, timestamp:Long, title:String) = new Url(url, title, false, true, timestamp)
	def local(url:String) = new Url(url, null, true, true, 0)
}

class Url(var url: String, var title:String, var dirty:Boolean, var active:Boolean, var timestamp:Long) {
	var body:String = null
	override def toString = url + " - " + title
	def contentUri = Contract.ContentUri.forPage(url)
	def withBody(body:String) = {
		this.body = body
		this
	}
}
