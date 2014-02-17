These issues are a must-fix before the next stable release:

* Move Ignore settings into separate table to not overwrite them upon repo
  update

* Right after updating a repo, "Recently Updated" shows the apps correctly but
  the new apks don't show up on App Details until the whole app is restarted
  (or until the repos are wiped and re-downloaded)

Other minor issues:

* Make the bluetooth option prettier. Options:
	- Move it into submenu (like "Share F-Droid" -> "Bluetooth/Mail/NFC/...")
	- Remove ellipsis from menu option string
