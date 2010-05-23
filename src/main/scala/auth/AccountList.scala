package net.gfxmonk.android.pagefeed.auth

import _root_.android.accounts.Account
import _root_.android.accounts.AccountManager
import _root_.android.app.ListActivity
import _root_.android.content.Intent
import _root_.android.os.Bundle
import _root_.android.view.View
import _root_.android.widget.ArrayAdapter
import _root_.android.widget.ListView

class AccountList extends ListActivity {
	
	/** Called when the activity is first created. */
	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		val accountManager = AccountManager.get(getApplicationContext())
		val accounts = accountManager.getAccountsByType("com.google")
		Util.info("got accounts: " + accounts.length)
		if (accounts.length == 0) {
			// start the activity anyways - it'll add an account if none is present
			var intent = new Intent(this, classOf[AppInfo])
			startActivity(intent)
		} else {
			this.setListAdapter(new ArrayAdapter[Account](this, R.layout.list_item, accounts))
		}
	}

	override def onListItemClick(l: ListView, v: View, position: Int, id: Long) = {
		var account = getListView().getItemAtPosition(position).asInstanceOf[Account]
		var intent = new Intent(this, classOf[AppInfo])
		intent.putExtra("account", account)
		startActivity(intent)
	}
}
