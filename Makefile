arrangement:
	echo -n 'sbt error ~install-emulator' | xsel -i

intent:
	adb shell am start -D -a android.intent.action.SEND -c android.intent.category.DEFAULT -t text/plain -d http://google.com/

emulate:
	sbt error ~install-emulator

.PHONY: arrangement intent
