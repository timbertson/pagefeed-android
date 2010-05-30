package net.gfxmonk.android.pagefeed

import _root_.android.util.Log

import _root_.org.apache.http.client.HttpClient

case class SyncSummary(added:Int, removed:Int)

class Sync (store: UrlStore, web: HttpClient) {

	val pagefeed = new PagefeedWeb(web)
	def run():SyncSummary = {
		processRemoteChanges()
		processLocalChanges()
	}

	private def processRemoteChanges() = {
		val remoteUrls = pagefeed.list().toList
		Log.d("pagefeed", "urls are: " + remoteUrls.mkString(", "))
		val localUrls = store.active().map(_.url).toList
		for (newRemoteUrl <- (remoteUrls -- localUrls)) {
			Util.info("adding URL (locally): " + newRemoteUrl)
			addItemLocally(newRemoteUrl)
		}
	}

	private def processLocalChanges() = {
		var added = 0
		var removed = 0
		store.dirty().foreach { item =>
			if(item.active) {
				Util.info("adding URL (remotely): " + item.url)
				addItemRemotely(item)
				added += 1
			} else {
				Util.info("removing URL (remotely): " + item.url)
				removeItemRemotely(item)
				removed += 1
			}
		}
		new SyncSummary(added, removed)
	}

	private def removeItemRemotely(item: Url) = {
		pagefeed.delete(item.url)
		store.purge(item)
	}

	private def addItemRemotely(item: Url) = {
		pagefeed.add(item.url)
		store.markClean(item)
	}

	private def addItemLocally(url: String) = {
		store.add(Url.remote(url))
	}
}
