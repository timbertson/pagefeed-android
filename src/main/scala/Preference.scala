package net.gfxmonk.android.pagefeed

import _root_.android.preference.PreferenceActivity
import _root_.android.preference.PreferenceScreen
import _root_.android.os.Bundle
import _root_.android.content.Intent

object Preferences {
	object ALWAYS_DOWNLOAD_CONTENT {
		val key = "PREF_ALWAYS_DOWNLOAD_CONTENT"
		val default = false
	}
}

class Preferences extends PreferenceActivity {
	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		addPreferencesFromResource(R.xml.userpreferences)
		findPreference("ACCOUNT_SETTINGS").setIntent(accountSettingsIntent)
	}

	private def accountSettingsIntent = {
		val intent = new Intent("android.settings.SYNC_SETTINGS")
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		intent
	}
}

