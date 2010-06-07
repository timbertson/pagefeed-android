package net.gfxmonk.android.pagefeed

import _root_.android.util.Log

import _root_.org.apache.http.client.HttpClient

case class SyncSummary(var added:Int, var removed:Int, latestDocTime:Long)

class Sync (store: UrlStore, web: HttpClient) {

	val pagefeed = new PagefeedWeb(web)
	def run(sinceTimestamp:Long):SyncSummary = {
		val newTimestamp = processRemoteChanges(sinceTimestamp)
		val summary = new SyncSummary(0, 0, newTimestamp)
		processLocalChanges(summary)
		summary
	}

	private def maxTimestamp(l:List[Long]):Long = {
		var max:Long = 0
		for(i <- l) {
			if (i > max) {
				max = i
			}
		}
		max
	}

	private def processRemoteChanges(sinceTimestamp: Long):Long = {
		val remoteUrls = pagefeed.listDocumentsSince(sinceTimestamp).toList
		Log.d("pagefeed", "urls are: " + remoteUrls.mkString(", "))
		val localUrls = store.active().toList
		val latestTime = maxTimestamp(localUrls.map(_.timestamp))
		for (newRemoteUrl <- (remoteUrls -- localUrls)) {
			Util.info("adding URL (locally): " + newRemoteUrl)
			addItemLocally(newRemoteUrl)
		}
		latestTime
	}

	private def processLocalChanges(summary:SyncSummary) = {
		store.dirty().foreach { item =>
			if(item.active) {
				Util.info("adding URL (remotely): " + item.url)
				addItemRemotely(item)
				summary.added += 1
			} else {
				Util.info("removing URL (remotely): " + item.url)
				removeItemRemotely(item)
				summary.removed += 1
			}
		}
	}

	private def removeItemRemotely(item: Url) = {
		pagefeed.delete(item.url)
		store.purge(item)
	}

	private def addItemRemotely(item: Url) = {
		pagefeed.add(item.url)
		store.markClean(item)
	}

	private def addItemLocally(url: Url) = {
		store.add(url)
	}
}
