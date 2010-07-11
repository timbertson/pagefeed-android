package net.gfxmonk.android.pagefeed

import scala.io.Source
import scala.collection.mutable.Map
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
import _root_.org.json.JSONException

object PagefeedWeb {
	var VERSION:String = null
	val BASE = "https://" + (if (VERSION != null) (VERSION + ".latest.") else "") + "pagefeed.appspot.com/"
}

class PagefeedWeb(web: HttpClient) {
	var auth:Any = null
	import PagefeedWeb._

	def add(url:String):Option[Url] = {
		val response = post(BASE + "page/", Map("url" -> url))
		try {
			parse(response).firstOption
		} catch {
			case e:ParseException => {
				Util.info("after adding item remotely, failed to parse JSON: " + response)
				return None
			}
		}
	}

	def delete(url:String) = {
		try {
			post(BASE + "page/del/", Map("url" -> url))
		} catch {
			// ignore 404s, they just mean the item is already deleted
			case _:NotFoundException => {}
		}
	}

	def documents():List[Url] = {
		val response = get(BASE + "page/list/")
		parse(response)
	}

	def parse(body:String):List[Url] = {
		try {
			val array = new JSONTokener(body).nextValue().asInstanceOf[JSONArray]
			(0 until array.length).map { i =>
				val obj = array.getJSONObject(i)
				val timestamp = obj.getLong("date")
				val url = obj.getString("url")
				val title = obj.getString("title")
				Url.remote(url, timestamp, title)
			}.toList
		} catch {
			case e:ClassCastException => throw new ParseException(body, e)
			case e:JSONException => throw new ParseException(body, e)
		}
	}

	private def get(url:String) = {
		Util.info("GETting: " + url)
		body(web.execute(new HttpGet(url)))
	}

	private def post(url:String, params:Map[String,String]):String = {
		params.put("json", "true")
		params.put("quiet", "true")
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

