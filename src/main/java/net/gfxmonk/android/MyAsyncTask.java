package net.gfxmonk.android;
import android.os.AsyncTask;

public abstract class MyAsyncTask<T1,T2,T3> extends AsyncTask<T1,T2,T2>{
	protected T3 doInBackground(T1 ... f) {
		return doInBackground(f[0]);
	}
	abstract protected T3 doInBackground(T1 f);

	protected void onPostExecute(T3 ... f) {
		return onPostExecute(f[0]);
	}
	abstract protected void onPostExecute(T3 f);
}
