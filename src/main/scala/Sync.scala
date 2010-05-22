package net.gfxmonk.android.pagefeed

class Sync (store: UrlStore) {

	val pagefeed = new PagefeedWeb()
	def run() = {
		processRemoteChanges
		processLocalChanges
	}

	private def processRemoteChanges = {
		val remoteUrls = pagefeed.list().map( u => Url.remote(u) )
		val localUrls = store.active().toList
		for (newRemoteUrl <- (remoteUrls -- localUrls)) {
			store.add(newRemoteUrl)
		}
	}

	private def processLocalChanges = {
		store.dirty().foreach { item =>
			if(item.active) {
				addItemRemotely(item)
			} else {
				removeItemRemotely(item)
			}
		}
	}

	private def removeItemRemotely(item: Url) = {
		pagefeed.delete(item.url)
	}

	private def addItemRemotely(item: Url) = {
		pagefeed.add(item.url)
	}
}
