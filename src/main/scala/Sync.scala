package net.gfxmonk.android.pagefeed

import _root_.android.util.Log

import _root_.org.apache.http.client.HttpClient

class Sync (store: UrlStore, web: HttpClient) {

	val pagefeed = new PagefeedWeb(web)
	def run() = {
		processRemoteChanges
		processLocalChanges
	}

	private def processRemoteChanges = {
		val remoteUrls = pagefeed.list().toList
		Log.d("pagefeed", "urls are: " + remoteUrls.mkString(", "))
		val localUrls = store.active().map(_.url).toList
		for (newRemoteUrl <- (remoteUrls -- localUrls)) {
			Util.info("adding URL (locally): " + newRemoteUrl)
			addItemLocally(newRemoteUrl)
		}
	}

	private def processLocalChanges = {
		store.dirty().foreach { item =>
			if(item.active) {
				Util.info("adding URL (remotely): " + item.url)
				addItemRemotely(item)
			} else {
				Util.info("removing URL (remotely): " + item.url)
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

	private def addItemLocally(url: String) = {
		store.add(Url.remote(url))
	}
}
