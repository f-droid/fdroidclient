### Upcoming release

* Support for repositories using self-signed HTTPS certificates through
  Trust-on-first-use popup

* Support for TLS Subject-Public-Key-Identifier pinning

### 0.58 (2014-01-11)

* Download icons with a resolution that matches the device's screen density,
  which saves resources on smaller devices and gets rid of unnecessary
  blurriness on larger devices

* Tweaked some layouts, especially the app lists and their compact layout

* App lists now show more useful version information: current version names,
  rather than number of versions available

* Reduce scroll lag in app lists by caching views in a ViewHolder

* Slightly increase performance in repo index XML handling by mapping apps
  with a HashMap, as opposed to doing linear searches

* More app info shown in App Details: The category in which the app was found
  and all the categories the app is in, as well as the Android version
  required to run each one of its versions available

* The preferences screen now uses descriptive summaries, which means that you
  can see what the checkbox preferences actually mean and what the edit and
  list preferences are set at

* Support for dogecoin donation method added (wow)

* Don't keep app icons older than 30 days on disc cache

* Always include incompatible apks in memory to avoid issues with apps
  seemingly not having any apks available

* Fixed a crash when trying to access a non-existing app

* F-Droid registers with Android to receive F-Droid URIs https://\*/fdroid/repo
  and fdroidrepos://

* support including signing key fingerprint in repo URIs

* when adding new repos that include the fingerprint, check to see whether
  that repo exists in F-Droid already, and if the fingerprints match

* Other minor bug fixes

* Lots of translation updates

### 0.55 (2013-11-11)

* Fixed problems with category selection and permission lists on Android 2.X devices.
* Lots of translation updates, including new Norwegian translation.

### 0.54 (2013-11-05)

* New options on the App Details screen to ignore all future updates for that
  particular app, or ignore just the current update.

* Apps with Anti-features are no longer hidden, and the corresponding
  preferences to unhide them are removed. Instead they are clearly marked on the
  App Details screen.

* Apps with incompatible native code architecture requirements are now correctly
  filtered.

* A bug that prevented update notifications from appearing has been fixed.

* Theming support, with Light and Dark themes.

* New launcher and notification icons, and new default/loading app icon. Icons
  are now retrieved dynamically, drastically improving startup time on first
  installation.

* All app donation options have been grouped into a submenu, and Litecoin
  donation support has been added.

* App filter settings now take effect immediately.

* Apk native code ABIs are now shown in expert mode.

* Search uris for market://search and fdroid.search: are  now handled.

* A problem with ActionBar Up navigation on some devices has been fixed.

* Other minor bug fixes, and adjustments to spacings and layouts.

* Lots of translation updates.

### 0.50 (2013-08-20)

* New basic app sharing functionality

* Handle f-droid.org web repo as well as market:// app uris

* Search by just typing on main screen and search results screen

* Flattr and Bitcoin donation methods added

* Noticeable speedups when returning from installs and uninstalls

* Add back to home buttons to the ActionBar

* Don't recommend versions newer than the current or incompatible with the device

* Use standard Android cache locations rather than .fdroid on the SD card

* Fix for crash at boot time where the SD card was slow to initialise

* Lots of bug fixes

* Lots of translation updates
