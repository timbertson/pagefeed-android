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
import _root_.org.apache.http.client.HttpClient
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
		result: SyncResult):Unit =
{
		var client:HttpClient = try {
			val authToken = accountManager.blockingGetAuthToken(account, AUTHTOKEN_TYPE, true /* notifyAuthFailure */)
			getAuthenticatedClient(authToken)
		} catch {
			case e:ParseException  => {
				result.stats.numParseExceptions += 1
				throw e
			}
			case e:AuthenticatorException => {
				result.stats.numAuthExceptions += 1
				throw e
			}
			case e: IOException => {
				result.stats.numIoExceptions += 1
				throw e
			}
		}

		val urlStore = new UrlStore(context)
		val sync = new Sync(urlStore, client)
		try {
			Util.info("sync: got cookie...")
			val sync = new Sync(new UrlStore(context), client)
			val syncResult = sync.run()
			result.stats.numInserts = syncResult.added.asInstanceOf[Long]
			result.stats.numDeletes = syncResult.removed.asInstanceOf[Long]
		} finally {
			urlStore.close()
		}
	}

	def getAuthenticatedClient(token:String):HttpClient = {
		val http_client = new DefaultHttpClient()
		http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false)
		
		try {
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
			return http_client
		} finally {
			// reset the HANDLE_REDIRECTS setting
			http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true)
		}
	}
}
