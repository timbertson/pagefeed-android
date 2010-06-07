package net.gfxmonk.android.pagefeed

object Url {
	def remote(url:String, timestamp:Long) = new Url(url, false, true, timestamp)
	def local(url:String) = new Url(url, true, true, 0)
}

class Url(var url: String, var dirty:Boolean, var active:Boolean, var timestamp:Long) {
	override def toString = url
}
