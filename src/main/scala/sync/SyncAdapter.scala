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

import scala.io.Source
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
	var alreadyFailed = false

	implicit def javaList2Seq[T](javaList: java.util.List[T]) : Seq[T] = new BufferWrapper[T]() { def underlying = javaList }

	override def onPerformSync(
		account: Account,
		extras: Bundle,
		authority: String,
		provider: ContentProviderClient,
		result: SyncResult):Unit =
{
		val syncProgress = new SyncProgress(context)
		var success = false
		syncProgress.start()
		var authToken:String = null
		try {
			var client:HttpClient = try {
				authToken = accountManager.blockingGetAuthToken(account, AUTHTOKEN_TYPE, true /* notifyAuthFailure */)
				getAuthenticatedClient(authToken)
			} catch {
				case e:Exception => {
					e.printStackTrace()
					e match {
						case e:ParseException  => result.stats.numParseExceptions += 1
						case e:AuthenticatorException => {
							if (alreadyFailed || authToken == null) {
								result.stats.numAuthExceptions += 1
							} else {
								// retry sync (at most once)
								alreadyFailed = true
								accountManager.invalidateAuthToken(account.`type`, authToken)
								Util.info("invalidated auth token")
								onPerformSync(account, extras, authority, provider, result)
							}
						}
						case e:IOException => result.stats.numIoExceptions += 1
						case _ => throw e
					}
					return
				}
			}

			val urlStore = new UrlStore(context)
			val sync = new Sync(urlStore, client)
			val lastTimestamp = Util.prefLong(context, SyncProgress.PREFERENCE_LAST_DOCTIME, 0)
			try {
				Util.info("sync: got cookie...")
				val sync = new Sync(new UrlStore(context), client)
				val syncResult = sync.run(lastTimestamp)
				result.stats.numInserts = syncResult.added.asInstanceOf[Long]
				result.stats.numDeletes = syncResult.removed.asInstanceOf[Long]
				Util.info("sync result is:" + syncResult)
				if(syncResult.latestDocTime > lastTimestamp) {
					updateTimestamp(context, syncResult.latestDocTime)
				}
				success = true
			} finally {
				urlStore.close()
			}
		} finally {
			syncProgress.finish(success)
		}
	}

	private def updateTimestamp(ctx:Context, newTimestamp:Long) = {
		val editor = ctx.getSharedPreferences(classOf[MainActivity].getName(), Context.MODE_PRIVATE).edit()
		editor.putLong(SyncProgress.PREFERENCE_LAST_DOCTIME, newTimestamp)
		Util.info("saved pref " + SyncProgress.PREFERENCE_LAST_DOCTIME + " = " + newTimestamp)
		editor.commit()
	}

	def getAuthenticatedClient(token:String):HttpClient = {
		val http_client = new DefaultHttpClient()
		http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false)
		
		var http_get = new HttpGet("https://pagefeed.appspot.com/_ah/login?continue=http://localhost/&auth=" + token)
		var response = http_client.execute(http_get)
		val statusCode = response.getStatusLine().getStatusCode()
		if(statusCode != 302) {
			// Response should be a redirect
			var body = "[couldn't fetch body]"
			try {
				val entity = response.getEntity()
				val content = entity.getContent()
				body = Source.fromInputStream(content).mkString
			} catch {
				case e:Exception => {} // oh well
			}
			throw new AuthenticatorException("expecting redirect (302) - got " + statusCode + ". body:\n" +
			body)
		}
		val cookie:Option[Cookie] = http_client.getCookieStore().getCookies().toList.find { _.getName == "ACSID" }
		cookie match {
			case None => throw new AuthenticatorException("no cookie present in redirect respose")
			case Some(cookie: Cookie) => cookie
		}
		http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true)
		return http_client
	}
}
