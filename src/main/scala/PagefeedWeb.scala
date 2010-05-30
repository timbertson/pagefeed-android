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
import _root_.org.apache.http.message.BasicNameValuePair
import _root_.org.apache.http.NameValuePair

class PagefeedWeb(web: HttpClient) {
	var auth:Any = null
	val BASE = "http://pagefeed.appspot.com/"

	def add(url:String) = {
		post(BASE + "page/", Map("url" -> url))
	}

	def delete(url:String) = {
		try {
			post(BASE + "page/del/", Map("url" -> url))
		} catch {
			// ignore 404s, they just mean the item is already deleted
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
		params ++ "quiet" -> "true"
		Util.info("POSTing: " + url + " with params = " + params)
		val paramList = new java.util.ArrayList[NameValuePair]()
		params.foreach { case (k,v) => paramList.add(new BasicNameValuePair(k,v)) }
		val post = new HttpPost(url)
		post.setEntity(new UrlEncodedFormEntity(paramList))
		body(web.execute(post))
	}

	private def body(result: HttpResponse):String = {
		val entity = result.getEntity()
		val content = entity.getContent()
		Util.info("response status = " + result.getStatusLine().getStatusCode())
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

