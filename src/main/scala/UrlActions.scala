package net.gfxmonk.android.pagefeed

import _root_.android.app.Activity
import _root_.android.view.Menu
import _root_.android.view.MenuItem
import _root_.android.content.Intent
import _root_.android.net.Uri

class UrlActions(activity:Activity) {
	def handleMenuItem(item: MenuItem, url: String) = {
		item.getItemId() match {
			case R.id.delete_item => {
				deleteItem(url)
				true
			}
			case R.id.share_item => {
				shareItem(url)
				true
			}
			case R.id.open_in_browser => {
				openItemInBrowser(url)
				true
			}
			case _ => activity.onContextItemSelected(item)
		}
	}

	def deleteItem(url: String) = {
		new UrlStore(activity).markDeleted(url)
	}

	def openItemInBrowser(url:String) = {
		Util.info("launching URL: " + url)
		val intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
		activity.startActivity(intent)
	}

	def shareItem(url: String) = {
		val intent = new Intent(Intent.ACTION_SEND)
		intent.putExtra(Intent.EXTRA_TEXT, url)
		intent.setType("text/plain")
		activity.startActivity(Intent.createChooser(intent, "Share Link:"))
	}

}
