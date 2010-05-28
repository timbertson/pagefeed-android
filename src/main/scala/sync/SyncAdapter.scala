package net.gfxmonk.android.pagefeed.sync

import _root_.android.accounts.Account
import _root_.android.accounts.AccountManager
import _root_.android.accounts.AuthenticatorException
import _root_.android.accounts.OperationCanceledException
import _root_.android.content.AbstractThreadedSyncAdapter
import _root_.android.content.ContentProviderClient
import _root_.android.content.Context
import _root_.android.content.SyncResult
import _root_.android.os.Bundle
import _root_.android.util.Log

import scala.collection.jcl.BufferWrapper
/*import scala.collection.JavaConversions._*/

import _root_.org.apache.http.ParseException
import _root_.org.apache.http.auth.AuthenticationException
import _root_.org.json.JSONException
import _root_.org.apache.http.client.methods.HttpGet
import _root_.org.apache.http.client.params.ClientPNames
import _root_.org.apache.http.cookie.Cookie
import _root_.org.apache.http.impl.client.DefaultHttpClient

import _root_.java.io.IOException
import _root_.java.util.Date
import _root_.java.util.List

class SyncAdapter(context: Context, autoInitialize: Boolean)
	extends AbstractThreadedSyncAdapter(context, autoInitialize)
{
	val TAG = "SyncAdapter"
	var lastUpdated: Date = null
	val accountManager = AccountManager.get(context)
	var AUTHTOKEN_TYPE = "ah"

	implicit def javaList2Seq[T](javaList: java.util.List[T]) : Seq[T] = new BufferWrapper[T]() { def underlying = javaList }

	override def onPerformSync(
		account: Account,
		extras: Bundle,
		authority: String,
		provider: ContentProviderClient,
		result: SyncResult) =
{
		def log(s:Throwable) = Log.w(TAG, s)
		try {
			// use the account manager to request the credentials
			val authToken = accountManager.blockingGetAuthToken(account, AUTHTOKEN_TYPE, true /* notifyAuthFailure */)
			Log.d(TAG, "sync woo!")
			Log.d(TAG, "got token: " + authToken)
			val cookie = getCookie(authToken)
			Log.d(TAG, "got cookie: " + cookie)
			lastUpdated = new Date()
		} catch {
			case e:ParseException  => {
				log(e)
				result.stats.numParseExceptions += 1
			}
			case e:AuthenticatorException => {
				log(e)
				result.stats.numAuthExceptions += 1
			}
			case e: IOException => {
				log(e)
				result.stats.numIoExceptions += 1
			}
			case e => {
				log(e)
			}
		}
	}

	def getCookie(token:String):Cookie = {
		val http_client = new DefaultHttpClient()
		http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false)
		
		var http_get = new HttpGet("https://pagefeed.appspot.com/_ah/login?continue=http://localhost/&auth=" + token)
		var response = http_client.execute(http_get)
		val statusCode = response.getStatusLine().getStatusCode()
		if(statusCode != 302) {
			// Response should be a redirect
			throw new AuthenticatorException("expecting redirect (302) - got " + statusCode)
		}
		val cookie:Option[Cookie] = http_client.getCookieStore().getCookies().toList.find { _.getName == "ACSID" }
		cookie match {
			case None => throw new AuthenticatorException("no cookie present in redirect respose")
			case Some(cookie: Cookie) => cookie
		}
		/*} catch (ClientProtocolException e) {*/
		/*	// TODO Auto-generated catch block*/
		/*	e.printStackTrace()*/
		/*} catch (IOException e) {*/
		/*	// TODO Auto-generated catch block*/
		/*	e.printStackTrace()*/
	}
}
