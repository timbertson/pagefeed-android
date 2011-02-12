package net.gfxmonk.android.pagefeed

import _root_.android.app.Activity
import _root_.android.content.Intent
import _root_.android.content.ContentValues
import _root_.android.database.Cursor
import _root_.android.view.View
import _root_.android.view.Menu
import _root_.android.view.MenuItem
import _root_.android.net.Uri
import _root_.android.os.Bundle
import _root_.android.util.Log
import _root_.android.view.LayoutInflater
import _root_.android.view.ViewGroup
import _root_.android.webkit.WebView
import _root_.android.widget.TextView
import _root_.android.widget.ViewFlipper
import _root_.android.webkit.WebSettings
import _root_.android.webkit.WebSettings.ZoomDensity
import _root_.android.view.animation.AlphaAnimation
import _root_.android.view.animation.Animation
import _root_.android.view.animation.Animation.AnimationListener
import _root_.android.widget.SeekBar
import _root_.android.widget.Button
import _root_.android.content.SharedPreferences.Editor
import net.gfxmonk.android.reader.view.ResumePositionWebViewClient

class ViewPost extends Activity {
  val PROJECTION = List(Contract.Data.URL, Contract.Data.BODY, Contract.Data.TITLE, Contract.Data.SCROLL)
	var flipper: ViewFlipper = null
	var webViewClient: ResumePositionWebViewClient = null
	val TAG = "ViewPost"
	var cursor: Cursor = null
	var webView: WebView = null

	val maxBrightness:Int = 255 // R.integer.max_brightness
	val brightnessRange:Int = 155 // R.integer.brightness_range
	val minBrightness:Int = (maxBrightness - brightnessRange)

	override def onCreate(savedInstanceState: Bundle) = {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.post_view)
		flipper = findViewById(R.id.post_flip).asInstanceOf[ViewFlipper]
	}

	override def onStart() = {
		super.onStart()
		initCursor()
		try {
			initData()
		} catch {
			case e:RuntimeException => {
				Log.e(TAG, "Error initialising ViewPost data", e)
				Util.toast("Error viewing page.", getApplicationContext())
				backToMain()
			}
		}
	}

	def backToMain() = {
		val intent = new Intent(this, classOf[MainActivity])
		startActivity(intent)
		finish()
	}

	override def onPause() = {
		saveScrollPosition()
		hideBrightnessSlider()
		super.onStop()
	}

	override def onCreateOptionsMenu(menu:Menu) = {
		getMenuInflater().inflate(R.menu.view_post_menu, menu)
		getMenuInflater().inflate(R.menu.url_context_menu, menu)
		true
	}

	def showBrightnessSlider() = {
		animateSlider(true)
	}

	def hideBrightnessSlider():Unit = {
		if(editingBrightness) {
			preferenceEditor.map(_.commit)
			preferenceEditor = None
			animateSlider(false)
		}
	}

	var editingBrightness = false
	private def animateSlider(show:Boolean) = {
		editingBrightness = show
		val view = findViewById(R.id.brightness_slider_container)
		val fade = if (show) new AlphaAnimation(0.0f, 1.0f) else new AlphaAnimation(1.0f, 0.0f)
		fade.setDuration(100)
		if (!show) {
			fade.setAnimationListener(
				new AnimationListener() {
					override def onAnimationEnd(a:Animation) = {
						view.setVisibility(View.GONE)
					}
					override def onAnimationStart(a:Animation) = {}
					override def onAnimationRepeat(a:Animation) = {}
				}
			)
		} else {
			view.setVisibility(View.VISIBLE)
			val slider = view.findViewById(R.id.brightness_slider).asInstanceOf[SeekBar]
			slider.setProgress(alphaSliderValue)
			slider.setOnSeekBarChangeListener(
				new SeekBar.OnSeekBarChangeListener() {
					def onProgressChanged(seekbar:SeekBar, progress:Int, fromUser:Boolean) = {
						val newAlpha = (progress.floatValue + minBrightness) / maxBrightness.floatValue
						setViewOpacity(newAlpha)
					}
					def onStartTrackingTouch(seekbar:SeekBar) = {}
					def onStopTrackingTouch(seekbar:SeekBar) = {}
				}
			)
			val self = this
			view.findViewById(R.id.hide_brightness_slider).asInstanceOf[Button].setOnClickListener(
				new View.OnClickListener() {
						def onClick(v:View) = self.hideBrightnessSlider()
				}
			)
		}
		view.startAnimation(fade)
	}


	override def onOptionsItemSelected(item:MenuItem):Boolean = {
		val itemId = item.getItemId()
		//TODO: load_images doesn't work!
		/*if (itemId == R.id.load_images) {*/
		/*	useNetwork()*/
		/*	return true*/
		/*}*/
		if (itemId == R.id.change_brightness) {
			showBrightnessSlider()
			return true
		}
		val handled = new UrlActions(this).handleMenuItem(item, cursor.getString(cursor.getColumnIndexOrThrow(Contract.Data.URL)))
		if (itemId == R.id.delete_item) {
			backToMain()
		}
		handled
	}

	def useNetwork() = {
		webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
	}

	def disableNetworkUse() = {
		webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
	}

	private def saveScrollPosition() = {
		val scrollRatio = webViewClient.getScrollRatio()

		if(scrollRatio != null) {
			var values = new ContentValues()
			Log.d(TAG, "Updating scroll ratio to " + scrollRatio + " for post " + getIntent().getData())
			values.put(Contract.Data.SCROLL, scrollRatio)
			val itemUri = getIntent().getData()
			getContentResolver().update(itemUri, values, null, null)
		}
	}

	private def initCursor() = {
		val uri = getIntent().getData()
		cursor = managedQuery(uri, PROJECTION.toArray, null, null, null)
		cursor.moveToNext()
	}

	private def alpha:Float = {
		Preferences.get(this, Preferences.TEXT_BRIGHTNESS)
	}

	private def alphaSliderValue = {
		((Preferences.get(this, Preferences.TEXT_BRIGHTNESS) * maxBrightness) - minBrightness).asInstanceOf[Int]
	}

	var preferenceEditor:Option[Editor] = None
	private def setViewOpacity(newAlpha:Float) = {
		val alphaChange = new AlphaAnimation(alpha, newAlpha)
		alphaChange.setFillAfter(true)
		webView.startAnimation(alphaChange)
		if (newAlpha != alpha) {
			preferenceEditor = Some(preferenceEditor.getOrElse(Preferences(this).edit()))
			preferenceEditor.map(_.putFloat(Preferences.TEXT_BRIGHTNESS.key, newAlpha))
		}
	}

	private def initData() = {
		var v = LayoutInflater.from(this).inflate(R.layout.post_view_item, null).asInstanceOf[ViewGroup]

		webView = v.findViewById(R.id.post_view_text).asInstanceOf[WebView]
		setViewOpacity(alpha)

		val title = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Data.TITLE))
		val body = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Data.BODY))

		//TODO: set font-color dynamically
		val html =
		  "<html><head><style type=\"text/css\">body { background-color: #201c19 !important; color: #FFF !important; } a { color: #ddf !important; } h1 h2 h3 { font-size:1em !important; }</style></head><body>" +
			"<h2>" + title + "</h2><br />" + body + "</body></html>"

		val encoding="utf-8"
		webView.loadDataWithBaseURL(null, html, "text/html", encoding, null)

		// add a delegate to resume the scroll position once the WebView renders its content
		val scrollPosition = cursor.getFloat(cursor.getColumnIndexOrThrow(Contract.Data.SCROLL))

		webViewClient = new ResumePositionWebViewClient(scrollPosition, webView, v.findViewById(R.id.post_view_loading), this)
		webView.setWebViewClient(webViewClient)
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY)

		//todo: paramaterise?
		webView.getSettings().setMinimumFontSize(20)
		disableNetworkUse();

		//add view to the flipper.
		flipper.addView(v)
	}
}
