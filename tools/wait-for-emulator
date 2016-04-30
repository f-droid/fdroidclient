#!/bin/bash

sec=0
timeout=360

err() {
	echo "$@"
	exit 1
}

explain() {
	if [[ "$1" =~ "not found" ]]; then
		printf "device not found"
	elif [[ "$1" =~ "offline" ]]; then
		printf "device offline"
	elif [[ "$1" =~ "running" ]]; then
		printf "booting"
	else
		printf "$1"
	fi
}

while true; do
	if [[ $sec -ge $timeout ]]; then
		err "Timeout ($timeout seconds) reached - Failed to start emulator"
	fi
	out=$(adb -e shell getprop init.svc.bootanim 2>&1 | grep -v '^\*')
	if [[ "$out" =~ "command not found" ]]; then
		err "$out"
	fi
	if [[ "$out" =~ "stopped" ]]; then
		break
	fi
	let "r = sec % 5"
	if [[ $r -eq 0 ]]; then
		echo "Waiting for emulator to start: $(explain "$out")"
	fi
	sleep 1
	let "sec++"
done

echo "Emulator is ready"
