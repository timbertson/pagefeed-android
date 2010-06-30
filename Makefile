arrangement:
	echo -n 'sbt error ~install-emulator' | xsel -i

intent:
	adb shell am start -D -a android.intent.action.SEND -c android.intent.category.DEFAULT -t text/plain -d http://google.com/ -n net.gfxmonk.android.pagefeed/.ShareLink

emulate:
	sbt warn ~reinstall-emulator

install:
	sbt warn ~reinstall-device

log:
	adb logcat \*:E pagefeed:\*

log_more:
	adb logcat \*:I pagefeed:\*

db:
	adb shell 'sqlite3 data/data/net.gfxmonk.android.pagefeed/databases/pagefeed ".dump url"'

drop_db:
	adb shell rm /data/data/net.gfxmonk.android.pagefeed/databases/pagefeed

release:
	sbt sign-release
	find target -name '*.apk' -mtime 0


size=128
notification_size=48
notification_inner_size=40
icon:
	[ ! -f /tmp/pagefeed.png ] || \
		convert /tmp/pagefeed.png \
			-resize $(size)x \
			-background transparent \
			-gravity center \
			-extent $(size)x$(size)+0+0 +repage \
			./src/main/res/drawable/app_icon.png
	[ ! -f /tmp/pagefeed-notification.png ] || \
		convert /tmp/pagefeed-notification.png -colorspace Gray \
			-resize x$(notification_inner_size) \
			-background transparent \
			-gravity center \
			-extent $(notification_size)x$(notification_size)+0+0 +repage \
			./src/main/res/drawable/notification.png

.PHONY: arrangement intent icon log log_more
