package net.gfxmonk.android.pagefeed

object Url {
	def remote(url:String) = new Url(url, false, true)
	def local(url:String) = new Url(url, true, true)
}

class Url(var url: String, var dirty:Boolean, var active:Boolean) {
	override def toString = url
}
