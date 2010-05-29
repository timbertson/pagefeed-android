package net.gfxmonk.android.pagefeed

import scala.io.Source
import _root_.org.apache.http.client.methods.HttpGet
import _root_.org.apache.http.client.methods.HttpPost
import _root_.org.apache.http.client.params.ClientPNames
import _root_.org.apache.http.params.BasicHttpParams
import _root_.org.apache.http.cookie.Cookie
import _root_.org.apache.http.client.HttpClient
import _root_.org.apache.http.HttpResponse
import _root_.org.apache.http.HttpException
import _root_.org.apache.http.client.entity.UrlEncodedFormEntity

class PagefeedWeb(web: HttpClient) {
	var auth:Any = null
	val BASE = "http://pagefeed.appspot.com/"

	def add(url:String) = {
		post(BASE + "page/", Map("url" -> url))
	}

	def delete(url:String) = {
		// TODO: ignore 404 errors
		try {
			post(BASE + "page/del/", Map("url" -> url))
		} catch {
			case _:NotFoundException => {}
		}
	}

	def list():List[String] = {
		var response = get(BASE + "page/list/")
		response.lines.map(_.trim).filter(_.length > 0).toList
	}

	private def get(url:String) = {
		Util.info("GETting: " + url)
		body(web.execute(new HttpGet(url)))
	}

	private def post(url:String, params:Map[String,String]) = {
		Util.info("POSTing: " + url + " with params = " + params)
		val httpParams = new BasicHttpParams()
		for ((k,v) <- params) httpParams.setParameter(k, v)
		Util.info("url = " + httpParams.getParameter("url"))
		val post = new HttpPost(url)
		post.setParams(httpParams)
		body(web.execute(post))
	}

	private def body(result: HttpResponse):String = {
		val entity = result.getEntity()
		val content = entity.getContent()
		Util.info("response status = " + result.getStatusLine().getStatusCode())
		Util.info("response body = " + Source.fromInputStream(content).mkString)
		val body = Source.fromInputStream(content).mkString
		result.getStatusLine().getStatusCode() match {
			case 200 => body
			case 404 => throw new NotFoundException()
			case code:Int => throw new HttpException("Response returned code " + code + " -- body content: " + body)
		}
	}
}

class NotFoundException extends HttpException {
}

