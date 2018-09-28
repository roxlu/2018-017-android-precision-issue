#!/bin/sh
export ANDROID_SERIAL=13878580469799
adb shell screencap -p /sdcard/screencap.png
adb pull /sdcard/screencap.png
#mv ./screencap.png ~/tmp/
