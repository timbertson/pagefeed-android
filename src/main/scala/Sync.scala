package net.gfxmonk.android.pagefeed
import net.gfxmonk.android.pagefeed.UrlStore
import net.gfxmonk.android.pagefeed.Url
import net.gfxmonk.android.pagefeed.PagefeedWeb

class Sync (store: UrlStore) {

	def run() = {
		
	}

	private def processLocalChanges = {
		store.eachDirty { item =>
			if(item.active) {
				addItemRemotely(item)
			} else {
				removeItemRemotely(item)
			}
		}
	}

	private def removeItemRemotely(item: Url) = {
		PagefeedWeb.delete(item.url)
	}

	private def addItemRemotely(item: Url) = {
		PagefeedWeb.add(item.url)
	}
}
