package net.gfxmonk.android.pagefeed

import _root_.android.preference.PreferenceActivity
import _root_.android.preference.PreferenceScreen
import _root_.android.os.Bundle
import _root_.android.content.Intent
import _root_.android.content.Context

class Preference[T](val key:String, val default:T) {
}

object Preferences {
	val ALWAYS_DOWNLOAD_CONTENT = new Preference("PREF_ALWAYS_DOWNLOAD_CONTENT", false)
	val TEXT_BRIGHTNESS = new Preference("TEXT_BRIGHTNESS", 1.0)
	lazy val sharedPreferencesKey = classOf[MainActivity].getName()

	def apply(ctx:Context) = {
		ctx.getSharedPreferences(sharedPreferencesKey, Context.MODE_PRIVATE)
	}

	def get(ctx:Context, pref:Preference[Double]) = {
		apply(ctx).getFloat(pref.key, pref.default.asInstanceOf[Float])
	}

	def get(ctx:Context, pref:Preference[Boolean]) = {
		apply(ctx).getBoolean(pref.key, pref.default)
	}
}

class Preferences extends PreferenceActivity {
	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		getPreferenceManager().setSharedPreferencesName(Preferences.sharedPreferencesKey)
		addPreferencesFromResource(R.xml.userpreferences)
		findPreference("ACCOUNT_SETTINGS").setIntent(accountSettingsIntent)
	}

	private def accountSettingsIntent = {
		val intent = new Intent("android.settings.SYNC_SETTINGS")
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		intent
	}
}

