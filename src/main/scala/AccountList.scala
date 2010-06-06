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
	def singleEnabledAccount(ctx:Context):Account = {
		val accountManager = AccountManager.get(ctx)
		val accounts = accountManager.getAccountsByType(Contract.ACCOUNT_TYPE)
		val syncableAccounts = accounts.filter(isSyncable _)
		if (syncableAccounts.length == 1) syncableAccounts(0) else null
	}

	def isAutoSync(account:Account) = ContentResolver.getSyncAutomatically(account, Contract.AUTHORITY)
	def isSyncable(account:Account) = ContentResolver.getIsSyncable(account, Contract.AUTHORITY) == 1

	def setSync(account:Account, enable:Boolean, ctx:Context) = {
		ContentResolver.setIsSyncable(account, Contract.AUTHORITY, if(enable) 1 else 0)
		Util.toast("sync " + (if(enable) "enabled" else "disabled") + " for " + account.name, ctx)
	}

	def syncNow(account:Account, ctx: Context):Boolean = {
		if(account == null) {
			Util.toast("Error: no appropriate account", ctx)
			false
		} else {
			ContentResolver.requestSync(account, Contract.AUTHORITY, new Bundle())
			true
		}
	}
}

class AccountList extends ListActivity {
	var accounts: Array[Account] = null
	
	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		val accountManager = AccountManager.get(getApplicationContext())
		accounts = accountManager.getAccountsByType(Contract.ACCOUNT_TYPE)
		setContentView(R.layout.account_list);
		if (accounts.length > 0) {
			val names = accounts.map(_.name)
			this.setListAdapter(new ArrayAdapter[String](this, R.layout.account_item, names))
		}
	}

	override def onListItemClick(l: ListView, v: View, position: Int, id: Long) = {
		var account = accounts(position)
		AccountList.setSync(account, true, getApplicationContext())
		accounts.map { a =>
			if(a != account) {
				ContentResolver.setIsSyncable(account, Contract.AUTHORITY, 0)
			}
		}
		finish()
	}
}
