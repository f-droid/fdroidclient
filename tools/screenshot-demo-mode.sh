#!/bin/sh
CMD=$1

if [[ $ADB == "" ]]; then
  ADB=adb
fi

if [[ $CMD != "on" && $CMD != "off" ]]; then
  echo "Usage: $0 [on|off]" >&2
  exit
fi

$ADB root || exit
$ADB wait-for-devices
$ADB shell settings put system time_12_24 24
$ADB shell settings put global sysui_demo_allowed 1

if [ $CMD == "on" ]; then
  $ADB shell settings put system screen_off_timeout 2147483647
  $ADB shell am broadcast -a com.android.systemui.demo -e command enter || exit
  $ADB shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1337
  $ADB shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false
  $ADB shell am broadcast -a com.android.systemui.demo -e command battery -e level 100
  $ADB shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e fully true -e level 4
  $ADB shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e fully true -e datatype none -e level 4
  $ADB shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
elif [ $CMD == "off" ]; then
  $ADB shell am broadcast -a com.android.systemui.demo -e command exit
  $ADB shell settings put system screen_off_timeout 30000
  $ADB shell settings put global sysui_demo_allowed 0
fi
