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
import _root_.org.json.JSONTokener
import _root_.org.json.JSONArray

object PagefeedWeb {
	val BASE = "https://pagefeed.appspot.com/"
}

class PagefeedWeb(web: HttpClient) {
	var auth:Any = null
	import PagefeedWeb._

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

	def listDocumentsSince(lastDoctime:Long):List[Url] = {
		var response = get(BASE + "page/list/?since=" + lastDoctime.toString)
		try {
			val array = new JSONTokener(response).nextValue().asInstanceOf[JSONArray]
			(0 until array.length).map { i =>
				val obj = array.getJSONObject(i)
				val timestamp = obj.getLong("date")
				val url = obj.getString("url")
				/*val title = obj.getString("title")*/
				Url.remote(url, timestamp)
			}.toList
		} catch {
			case e:ClassCastException => throw new ParseException(response, e)
		}
	}

	private def get(url:String) = {
		Util.info("GETting: " + url)
		body(web.execute(new HttpGet(url)))
	}

	private def post(url:String, params:Map[String,String]) = {
		params ++ "quiet" -> "true"
		Util.info("POSTing: " + url + " with params = " + params)
		val post = new HttpPost(url)
		post.setEntity(makeParams(params))
		body(web.execute(post))
	}

	private def makeParams(params:Map[String,String]) = {
		val paramList = new java.util.ArrayList[NameValuePair]()
		params.foreach { case (k,v) => paramList.add(new BasicNameValuePair(k,v)) }
		new UrlEncodedFormEntity(paramList)
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

class NotFoundException extends HttpException {}
class ParseException(msg:String, cause:Throwable) extends RuntimeException(msg, cause) {}

