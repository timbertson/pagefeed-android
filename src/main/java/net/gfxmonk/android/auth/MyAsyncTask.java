package net.gfxmonk.android.pagefeed;
import android.os.AsyncTask;

public abstract class MyAsyncTask<InType,ProgressType,OutType> extends AsyncTask<InType,ProgressType,OutType>{
	protected OutType doInBackground(InType ... f) {
		return doInBackground(f[0]);
	}
	abstract protected OutType doInBackground(InType f);

	protected void onPostExecute(OutType ... f) {
		onPostExecute(f[0]);
	}
	abstract protected void onPostExecute(OutType f);
}
