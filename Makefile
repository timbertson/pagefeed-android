arrangement:
	echo -n 'sbt error ~install-emulator' | xsel -i

intent:
	adb shell am start -D -a android.intent.action.SEND -c android.intent.category.DEFAULT -t text/plain -d http://google.com/ -n net.gfxmonk.android.pagefeed/.ShareLink

emulate:
	sbt ~reinstall-emulator

log:
	adb logcat \*:E pagefeed:\*

log_more:
	abd logcat \*:I pagefeed:\*

size=128
icon:
	convert ~/Desktop/pagefee-android.png -resize $(size)x -background transparent -gravity center -extent $(size)x$(size)+0+0 +repage ./src/main/res/drawable/app_icon.png

.PHONY: arrangement intent icon log log_more
