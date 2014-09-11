### 0.74 (2014-09-11)

* Added "local repo" support to host an F-Droid repo on the device itself, with
  full signed repo support, icons, and optional HTTPS

* Ability to set up such a local repo by choosing from any of the installed
  applications on the device

* Reverted self-signed support since it was broken, only HTTPS certificates
  with proper CA signatures will work for now

* Find local repos on the same network using Bonjour/mDNS

* Support for F-Droid repos on Tor Hidden Services (.onion addresses)

* HTTP Proxy support in Preferences

* Directly send installed apps to other devices via Bluetooth and Android Beam
  (NFC+Bluetooth), also compatible with Samsung/HTC S-Beam

* Initial support for root and system installers, allowing the client to
  install apks directly on its own

* Switch to Appcompat from the Support library

* Fix some crashes

* Translation updates

### 0.66 (2014-05-01)

* Fix crash on startup for devices with more than 500 installed apps

* Send apps to other devices directly from the App Details screen via NFC or Bluetooth

* Improved performance for devices with many installed apps

* Improve ellipsizing and spacing in the app lists

* Start translating the category lists

* Keep track of installed apps internally, rather than asking Android each time

* Security fixes and updates for adding of repos

* Fix bug introduced in 0.63 which made F-Droid always omit density-specific
  icons, making icons blurry on high-res devices

* Fix some other crashes

* Translation updates

### 0.63 (2014-04-07)

* Support for Network Service Discovery of local F-Droid repos on Android 4.1+
  from the repository management screen

* Always remember the selected category in the list of apps

* Send F-Droid via Bluetooth to any device that supports receiving APKs via
  Bluetooth (stock Android blocks APKs, most ROMs allow them)

* NFC support: beam repo configs from the repo detail view (Android 4.0+),
  beam the F-Droid.apk from F-Droid's main screen (Android 4.1+)

* Support for repositories using self-signed HTTPS certificates through
  a Trust-on-first-use popup

* Support for TLS Subject-Public-Key-Identifier pinning

* Add native Right-to-Left support on devices running 4.2 and later

* Filter app compatibility by maxSdkVersion too

* Major internal changes to enable F-Droid to handle repos with thousands
  of apps without slowing down too much. These internal changes will also make
  new features easier to implement.

* Various fixes to layout issues introduced in 0.58

* Translation updates

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
