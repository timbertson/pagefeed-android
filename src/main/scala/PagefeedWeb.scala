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
import _root_.org.apache.http.client.utils.URLEncodedUtils
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
		try {
			val response = post(BASE + "page/", Map("url" -> url))
			try {
				return parse(response).firstOption
			} catch {
				case e:ParseException => {
					Util.info("after adding item remotely, failed to parse JSON: " + response)
					return None
				}
			}
		} catch {
			case e:HttpException => {
				Util.info("after adding item remotely, got server error: " + e)
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

	def parse(response:String):List[Url] = {
		try {
			val array = new JSONTokener(response).nextValue().asInstanceOf[JSONArray]
			(0 until array.length).map { i =>
				val obj = array.getJSONObject(i)
				val timestamp = obj.getLong("date")
				val url = obj.getString("url")
				val title = obj.getString("title")
				val has_content = obj.getBoolean("has_content")
				Url.remote(url, timestamp, title, has_content)
			}.toList
		} catch {
			case e @ (_:ClassCastException|_:JSONException) => throw new ParseException(response, e)
		}
	}

	def getBody(url:String):String = {
		get(BASE + "page/", Map("url" -> url))
	}

	private def get(url:String):String = {
		Util.info("GETting: " + url)
		val req = new HttpGet(url)
		body(web.execute(req))
	}

	private def get(url:String, params:Map[String,String]):String = {
		get(url + "?" + makeParamsForGet(params))
	}

	private def post(url:String, params:Map[String,String]):String = {
		params.put("json", "true")
		params.put("quiet", "true")
		Util.info("POSTing: " + url + " with params = " + params)
		val post = new HttpPost(url)
		post.setEntity(makeParamsForPost(params))
		body(web.execute(post))
	}

	// honestly... who needs two different param formulations for GET and POST?
	private def makeParamsForPost(params:Map[String,String]) = {
		new UrlEncodedFormEntity(makeParamList(params))
	}

	private def makeParamsForGet(params:Map[String,String]):String = {
		URLEncodedUtils.format(makeParamList(params), "UTF-8")
	}

	private def makeParamList(params:Map[String,String]) = {
		val paramList = new java.util.ArrayList[NameValuePair]()
		params.foreach { case (k,v) => paramList.add(new BasicNameValuePair(k,v)) }
		paramList
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

