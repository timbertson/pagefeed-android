package net.gfxmonk.android.pagefeed
import net.gfxmonk.android.pagefeed.Url

class PagefeedWeb {
	var auth:Any = null
	val BASE = "http://pagefeed.appspot.com"

	private def ensureAuth() = {
		// TODO
	}

	def add(url:String) = {
		ensureAuth()
		post(BASE + "page/", Map("url" -> url))
	}

	def delete(url:String) = {
		ensureAuth()
		// TODO: ignore 404 errors
		post(BASE + "page/del/", Map("url" -> url))
	}

	def list():List[String] = {
		var response = get(BASE + "page/list/")
		response.body.lines.map(_.trim).filter(_.length > 0)
	}

	private def get(url:String) = {
		// TODO
		new Response(200, "OK")
	}

	private def post(url:String, params:Map) = {
		// TODO
		new Response(200, "OK")
	}
}

class Response(val code:Int, val body:String) {
	
}
