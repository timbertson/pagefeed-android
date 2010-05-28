package net.gfxmonk.android.pagefeed.auth

import net.gfxmonk.android.pagefeed._
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import scala.collection.jcl.BufferWrapper
/*import scala.collection.JavaConversions._*/

import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.params.ClientPNames
import org.apache.http.cookie.Cookie
import org.apache.http.impl.client.DefaultHttpClient

import _root_.android.accounts.Account
import _root_.android.accounts.AccountManager
import _root_.android.accounts.AccountManagerCallback
import _root_.android.accounts.AccountManagerFuture
import _root_.android.accounts.AuthenticatorException
import _root_.android.accounts.OperationCanceledException
import _root_.android.app.Activity
import _root_.android.content.Intent
import _root_.android.os.Bundle
import _root_.android.widget.Toast


class AppInfo extends Activity {
	val http_client:DefaultHttpClient = new DefaultHttpClient()

	implicit def javaList2Seq[T](javaList: java.util.List[T]) : Seq[T] = new BufferWrapper[T]() { def underlying = javaList }

	override def onCreate(savedInstanceState:Bundle) = {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.app_info)
	}

	override def onResume() = {
		Util.info("appinfo: resumed")
		super.onResume()
		val intent = getIntent()
		val accountManager = AccountManager.get(getApplicationContext())
		var account:AnyRef = null
		val extras = intent.getExtras()
		if (extras != null) {
			account = extras.get("account")
		}
		account match {
			case null => accountManager.addAccount("com.google", "ah", null, null, this, new GetAuthTokenCallback(), null)
			case account: Account => accountManager.getAuthToken(account, "ah", false, new GetAuthTokenCallback(), null)
		}
	}

	class GetAuthTokenCallback extends AccountManagerCallback[Bundle] {
		def run(result: AccountManagerFuture[Bundle]) = {
			try {
				var bundle = result.getResult()
				var intent = bundle.get(AccountManager.KEY_INTENT).asInstanceOf[Intent]
				if(intent != null) {
					// User input required
					startActivity(intent)
				} else {
					onGetAuthToken(bundle)
				}
			} catch {
				case e: Exception => e.printStackTrace()
			}
			// real cases:
			// OperationCanceledException
			// AuthenticatorException
			// IOException
		}
	}

	protected def onGetAuthToken(bundle: Bundle) = {
		val auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
		new GetCookieTask().execute(auth_token)
	}

	private class GetCookieTask extends MyAsyncTask[String, Void, Boolean] {
		override protected[pagefeed] def doInBackground(token: String):Boolean = {
			try {
				// Don't follow redirects
				http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false)
				
				var http_get = new HttpGet("https://pagefeed.appspot.com/_ah/login?continue=http://localhost/&auth=" + token)
				var response = http_client.execute(http_get)
				if(response.getStatusLine().getStatusCode() != 302) {
					// Response should be a redirect
					return false
				}
				if(http_client.getCookieStore().getCookies().toList.exists { _.getName == "ACSID" }) {
					return true
				}
			} catch {
				case e: Exception => e.printStackTrace()
			/*} catch (ClientProtocolException e) {*/
			/*	// TODO Auto-generated catch block*/
			/*	e.printStackTrace()*/
			/*} catch (IOException e) {*/
			/*	// TODO Auto-generated catch block*/
			/*	e.printStackTrace()*/
			} finally {
				http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true)
			}
			return false
		}
		
		override def onPostExecute(result:Boolean) = {
			new AuthenticatedRequestTask().execute("http://yourapp.appspot.com/admin/")
		}
	}

	private class AuthenticatedRequestTask extends MyAsyncTask[String, Void, HttpResponse] {
		override protected[pagefeed] def doInBackground(url: String):HttpResponse = {
			try {
				var http_get = new HttpGet(url)
				return http_client.execute(http_get)
			} catch {
				case e:Exception => e.printStackTrace()
			}
			// ClientProtocolException
			// IOException
			return null
		}
		
		override def onPostExecute(result: HttpResponse) = {
			try {
				val reader = new BufferedReader(new InputStreamReader(result.getEntity().getContent()))
				val first_line = reader.readLine()
				Toast.makeText(getApplicationContext(), first_line, Toast.LENGTH_LONG).show()
			} catch {
				case e:Exception => e.printStackTrace()
			}
			// IllegalStateException
			// IOException
		}
	}
}
