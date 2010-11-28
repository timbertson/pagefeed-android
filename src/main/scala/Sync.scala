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

	def downloadPageContents() = {
		for (page <- store.emptyPages()) {
			populateBody(page)
		}
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
		val remoteUrlObjects:List[Url] = pagefeed.documents().toList
		val remoteUrls = remoteUrlObjects.map(_.url)
		Log.d("pagefeed", "remote urls are: " + remoteUrlObjects.mkString(", "))
		val localUrlObjects = store.all().toList
		Log.d("pagefeed", "local urls are: " + localUrlObjects.mkString(", "))
		val localUrlMap = Map[String,Url]((localUrlObjects.map(u => u.url -> u)):_*)

		for (remoteUrl <- remoteUrlObjects) {
			localUrlMap.get(remoteUrl.url) match {
				case None => {
					addItemLocally(remoteUrl)
					summary.added += 1
				}
				case Some(localUrl:Url) => {
					if(localUrl.active) {
						updateItem(localUrl, remoteUrl)
					}
				}
			}

			if(remoteUrl.timestamp > summary.latestDocTime) {
				summary.latestDocTime = remoteUrl.timestamp
			}
		}

		for(localUrl <- localUrlObjects if localUrl.active && (!localUrl.dirty)) {
			if (!remoteUrls.contains(localUrl.url)) {
				removeItemLocally(localUrl)
			}
		}
	}

	private def processLocalChanges(summary:SyncSummary) = {
		store.dirty().foreach { item =>
			if(item.active) {
				addItemRemotely(item)
				summary.added += 1
			} else {
				removeItemRemotely(item)
				summary.removed += 1
			}
		}
	}

	private def populateBody(item: Url) = {
		Util.info("populating body for url: " + item)
		val body = pagefeed.getBody(item.url)
		updateBody(item, body)
	}

	private def updateBody(item:Url, content:String) = {
		item.body = content
		store.update(item)
	}

	private def removeItemRemotely(item: Url) = {
		Util.info("removing URL (remotely): " + item.url)
		pagefeed.delete(item.url)
		store.purge(item)
	}

	private def addItemRemotely(item: Url) = {
		Util.info("adding URL (remotely): " + item.url)
		val remoteUrl:Option[Url] = pagefeed.add(item.url)
		remoteUrl.map(updateItem(item, _))
		store.markClean(item)
	}

	private def addItemLocally(url: Url) = {
		Util.info("adding URL (locally): " + url)
		store.add(url)
	}

	private def removeItemLocally(url: Url) = {
		Util.info("removing URL (locally): " + url)
		store.purge(url)
	}

	private def updateItem(local: Url, remote: Url):Unit = {
		if(local.title == remote.title) {
			return
		}
		Util.info("title update: '"+local.title+"' is now '"+remote.title+"'")
		local.title = remote.title
		store.update(local)
	}
}
