package net.gfxmonk.android.pagefeed

class TaskAsync[InType, ProgressType, OutType](f:(InType)=>OutType) extends MyAsyncTask[InType, ProgressType, OutType]{
	var postExecute: (OutType) => Unit = null
	override protected[pagefeed] def doInBackground(inVal: InType):OutType = {
		return f(inVal)
	}

	override protected[pagefeed] def onPostExecute(result:OutType):Unit = {
		if (postExecute != null) {
			postExecute(result)
		}
		null
	}

	def andThen(andThenFunc:(OutType) => Unit) = {
		postExecute = andThenFunc
		this
	}
}

