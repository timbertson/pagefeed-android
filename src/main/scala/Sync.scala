package net.gfxmonk.android.pagefeed

import _root_.android.util.Log

import _root_.org.apache.http.client.HttpClient

case class SyncSummary(var added:Int, var removed:Int, var latestDocTime:Long)

class Sync (store: UrlStore, web: HttpClient) {

	val pagefeed = new PagefeedWeb(web)
	def run(sinceTimestamp:Long):SyncSummary = {
		val summary = new SyncSummary(0, 0, sinceTimestamp)
		processRemoteChanges(summary)
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

	private def processRemoteChanges(summary:SyncSummary):Unit = {
		def getUrl(u:Url) = u.url
		val remoteUrls = pagefeed.documents().toList
		Log.d("pagefeed", "urls are: " + remoteUrls.mkString(", "))
		val localUrlObjects = store.active().toList
		val localUrls = localUrlObjects.map(_.url)
		for (remoteUrl <- remoteUrls) {
			if(!localUrls.contains(remoteUrl.url)) {
				Util.info("adding URL (locally): " + remoteUrl)
				addItemLocally(remoteUrl)
				summary.added += 1
			}
			if(remoteUrl.timestamp > summary.latestDocTime) {
				summary.latestDocTime = remoteUrl.timestamp
			}
		}
		for(localUrl <- localUrlObjects) {
			if ((!localUrl.dirty) && (!remoteUrls.contains(localUrl.url))) {
				Util.info("removing URL (locally): " + localUrl)
				removeItemLocally(localUrl)
			}
		}
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
	private def removeItemLocally(url: Url) = {
		store.purge(url)
	}
}
