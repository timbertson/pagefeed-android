package net.gfxmonk.android.pagefeed

import net.gfxmonk.android.pagefeed._
import _root_.android.accounts.Account
import _root_.android.accounts.AccountManager
import _root_.android.app.ListActivity
import _root_.android.content.Intent
import _root_.android.os.Bundle
import _root_.android.view.View
import _root_.android.widget.ArrayAdapter
import _root_.android.widget.ListView
import _root_.android.content.ContentResolver
import _root_.android.content.Context

object AccountList {
	def hasEnabledAccount(ctx:Context):Boolean = {
		val accountManager = AccountManager.get(ctx)
		val accounts = accountManager.getAccountsByType(Contract.ACCOUNT_TYPE)
		accounts.find(ContentResolver.getIsSyncable(_, Contract.AUTHORITY) > 0) != None
	}
}

class AccountList extends ListActivity {
	var accounts: Array[Account] = null
	
	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		val accountManager = AccountManager.get(getApplicationContext())
		accounts = accountManager.getAccountsByType(Contract.ACCOUNT_TYPE)
		Util.info("got accounts: " + accounts.length)
		if (accounts.length == 0) {
			// start the activity anyways - it'll add an account if none is present
			/*var intent = new Intent(this, classOf[AppInfo])*/
			/*startActivity(intent)*/
		} else {
			val names = accounts.map(_.name)
			this.setListAdapter(new ArrayAdapter[String](this, R.layout.account_list, names))
		}
	}

	override def onListItemClick(l: ListView, v: View, position: Int, id: Long) = {
		var account = accounts(position)
		ContentResolver.setIsSyncable(account, Contract.AUTHORITY, 1)
	}
}
