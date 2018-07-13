### 1.3-alpha4 (2018-07-13)

* fix Data/WiFi preferences to properly schedule Updats

* fix Install/Uninstall events for clearer feedback

* track pending installs properly, stop fake repeating updates

* add support for Repo Push Requests when using Index V1

* support NoSourceSince anti-feature

* share menu item for repos

* fix a few crasher bugs

### 1.3-alpha3 (2018-06-27)

* fix bug that disabled Privileged Extension

* prevent crash loop after rapid install/uninstall cycling

* add expert option to send debug version/UUID on each HTTP download

* allow user to disable ACRA entirely with a preference

* basic Install History viewer, available only when logging is enabled

### 1.3-alpha2 (2018-06-25)

* Settings improvements

* new Expert Setting for disabling all notifications

* huge improvements for custom "whitelabel" F-Droid versions

### 1.3-alpha1 (2018-06-15)

* improved Settings for controlling data usage

* support push install/uninstall requests in index-v1

### 1.3-alpha0 (2018-04-25)

* more battery conscious background operation on Android 5.0 and newer

* make Anti-Features list in App Details clickable

* new Settings for controlling data usage

* switch Settings to Material style

* bumped minimum supported version to Android 4.0 (14)

### 1.2.2 (2018-04-23)

* fix crasher bug on devices running on Android 4.2 or older #1424

### 1.2.1 (2018-04-18)

* improved automatic mirror selection

* more swap/nearby bug fixes and improvements

### 1.2 (2018-04-13)

* lots of swap/nearby bug fixes and improvements

* fix one cause of reoccuring update notifications (#1271)

* make F-Droid recognize fdroid nightly URLs from GitLab

### 1.2-alpha1 (2018-04-06)

* fix Privileged Extension install with apps with uses-permision-sdk-23

* automatically trim or delete cache when storage space is low

* improved performance on low memory devices

* make all downloads respect "Only on Wi-Fi" preference

### 1.2-alpha0 (2018-03-30)

* add custom mirrors to any repo by clicking links, scanning QR codes, etc.

* reduce memory usage when device is running low

### 1.1 (2018-03-21)

* fix some problems with items Updates reappearing

* fix failback install method when permissions aren't in sync #1310

### 1.1-alpha4 (2018-03-09)

* fix the most popular ACRA crash reports

* UI layout improvements

* warn users when scanning QR with camera without autofocus

### 1.1-alpha3 (2018-02-13)

* add sort button to Search view: alpha or most recent

* fix bugs: #1305 #1306 #1325

* add more detail to ACRA crash reports

### 1.1-alpha2 (2018-02-06)

* reload index after system locale change or OS upgrade

* add "panic responder" support

### 1.1-alpha1 (2018-01-26)

* provision new repos via a provisioning file

* "Android App Links" handling aka "Digital Asset Links"

* new privacy prefs: disable screenshots; exit on panic

### 1.1-alpha0 (2017-11-09)

* automatically choose between official repo mirrors

* fullscreen, swipeable app screenshot navigation

* new preference to prevent screenshots/recents

* fix crasher bug #1203

### 1.0.1 (2017-10-23)

* fixed index update failure on Android 5.0 (#1014)

### 1.0 (2017-10-10)

* Completely overhauled workflow for updating apps

* Fully translatable app summaries and descriptions

* "What's New" section to show changes in current release

* Screenshots and feature graphics

* Support installing media, OTA, ZIP, etc. files

* Improved protection against tracking (HTTP ETag, TLS, etc.)

* Fully background updates with Privileged Extension

* Highlight donations to app developers

* Much faster index updates

### 1.0-alpha5 (2017-10-04)

* Fix bug that prevented translations from showing up on Android >= 7.0 (#987)

* Fix DB upgrade crash from 1.0-alpha3 --> 1.0-alpha4 (#1181)

### 1.0-alpha4 (2017-09-27)

* Added swipe gestures to the Updates tab

* Display warnings with actions in Updates tab for KnownVulns

* Translation updates

* Dark UI fixes

### 1.0-alpha3 (2017-09-12)

* Big UI performance improvements, especially with archive enabled

* Fixed crasher bugs

### 1.0-alpha2 (2017-09-04)

* Prevent HTTP ETag from being used as a tracking cookie

* Improved screenshots layout

* Properly clean up temp and cached files

* Dark mode fixes

### 1.0-alpha1 (2017-07-18)

* Fix bug removing apps from repos (#568)

* Much faster index updates

### 1.0-alpha0 (2017-07-08)

* Support installing media, OTA, ZIP, etc. files

* Fully support APKs signed by multiple signing keys

* Tibetan translation

* Remove related apps and categories after disabling repo

### 0.104 (2017-06-16)

* Support apps with APKs signed by more than one key

* Fix F-Droid update notifications that never go away

### 0.103.2 (2017-05-31)

* Fix problematic updates and notifications (#1013)

* Language and stability updates

### 0.103.1 (2017-05-12)

* Various stability fixes

* Bits of text no longer randomly switch to English

* Fix send F-Droid via Bluetooth on Android 7.x

### 0.103 (2017-05-02)

* Complete overhaul of the user experience

* Complete support for localization, including app descriptions

* Support for screenshots, graphics, and "What's New" texts

* Stable support for F-Droid Privileged Extension

### 0.102.3 (2017-04-01)

* Fix issue with installing from the wrong repo (#909)

* Allow F-Droid to update Privileged Extension (#911)

* Ignore errors that are likely due to filesystem corruption (#855)

* Improve installs/uninstalls with Privileged Extension on 7.x

### 0.102.2 (2017-03-14)

* Fix installing with Privileged Extension on 7.x

* Detect app updates via sytem OTA updates (#819)

### 0.102.1 (2017-02-24)

* Detect installed/uninstalled state more reliably (#854)

* Ensure dark theme gets applied everywhere (#750)

### 0.102 (2016-11-28)

* Optionally keep install history

* Optionally let repositories request installs and uninstalls of apps

* Support for APK extension files (OBB)

* Enable TLS v1.2 for HTTPS on all devices that support it (again)

* Better support for multiple repositories providing the same app

### 0.101 (2016-09-28)

* Support for Android 2.2 is dropped, 2.3.3 or later is now required

* Fixed APK Cache bugs, requiring the cache time be reset to one day

* Use Privileged Extension by default if installed

* Optionally grey out apps that require Anti-Features

* Translation updates

### 0.100.1 (2016-06-21)

* Fix background crash after installing or updating apps

* Fix crash if an app has a short description

* Fix background crash in the Wi-Fi state change swap service

* Fix crash if there is a problem listing the cached files to delete

### 0.100 (2016-06-07)

* Ability to download apps in the background

* Significant performance improvements when updating repositories

* Add setting to enable automatic downloading of updates

* Apks can now be kept on disk for various amounts of time

* Show what repository each apk comes from

* Better support for Android 6.0

* Translation updates

### 0.99.2 (2016-04-01)

* Stability fixes for app swap

### 0.99.1 (2016-03-22)

* Re-enable SNI support on Guardian Project repo

### 0.99 (2016-02-29)

* Add simple "Use Tor" setting

* Enable TLS v1.2 for HTTPS on all devices that support it

* Fix a series of Swap crashes, including a background crash

* Fix most Android lint errors and some warnings

* Translation updates

### 0.98.1 (2016-02-14)

* Fix crash when entering only a space into the search dialog

* Fix crash when entering slashes into the search dialog

* Fix potential fragment crash when installing/removing a package

* Fix crash when adding malformed URIs as repos

* Fix Android.mk build when the output dir. is a relative path

### 0.98 (2016-02-01)

* Add opt-in crash reporting via ACRA

* Add support for HTTP basic authentication of repos

* Fix repo updating on older devices with little memory

* Rework search to be incremental and without a separate activity

* Add English to the list of languages to use in the settings

* Fix "database is locked" seemingly random crash

* Cache installed app information in the database

* Add counter to the installed tab

* Improve repo update test coverage

* Translation updates

* Display license and author information in app details where appropriate

### 0.97 (2015-11-07)

* Add option to prompt for unstable updates globally

* Add support for free Certificate Authorities: cert.startcom.org and
  letsencrypt.org

* Rework the privileged installer to use an extension as a privileged app
  instead of F-Droid itself

* Add a new night theme

* Fix crash when trying to install incompatible apps with the privileged
  installer

* Fix downloading from HTTP servers that did not send a Content-Length

* Material design tweaks and fixes, including repo and app screens makeovers

* Add StrictMode to debug builds

* Make the GitLab CI run the tests in an emulator

* Use gradle-witness to ensure the reproducibility of the build with libraries
  pulled from maven repositories

* Switched to Weblate for translations

* Translation updates

### 0.96.1 (2015-09-24)

* Fix crash when updating repos on Android 2.3.7 or older

### 0.96 (2015-09-03)

* Move the repo index update to a notification

* Handle APK downloads without a dialog

* Don't let users try to uninstall system apps that haven't been updated

* Various bugfixes in the process that installs F-Droid as a privileged app

* Fix privileged installer confirmation screen issues on Android 2.X/3.X/4.X

* Disable HTTPS support in swap until it's stable

* Fix a few crashes, including package receivers and NFC actions

* Translation updates

### 0.95.1 (2015-08-10)

* Disable prompt to install F-Droid into system until it's more stable

* Fix crash when updating from an older release if using the "Light with dark
  ActionBar" theme

* Re-add SuperUser third-party permission to the manifest since some systems
  require it

* Fix privileged installer confirmation screen crash on Android < 3.0

### 0.95 (2015-08-04)

* Start porting UI to Material Design, including a new launcher icon

* Add support for app changelog links, which will appear for apps that have
  them once the repositories have been updated again

* Redesign the App Details view with larger icons, expandable description and
  links with icons

* Add ability to make F-Droid install itself as a privileged app on /system
  via root, allowing the use of the system installer

* Remove the root installer, since the system installer is safer, more stable
  and now easy to set up with root privileges

* Speed up and simplify repo update process by streaming the data out of the
  JAR file directly

* Can now manually add swap repo via "Repositories" screen

* Using NFC during swap now initiates a proper swap, rather than redirecting to
  the "Repositories" screen

* Drop Ant support to greatly simplify the build process and its maintenance

### 0.92 (2015-06-08)

* Make swap only in portrait mode to prevent crashes and issues where UI elements are obscured

* Update Universal-Image-Loader to 1.9.4

* Make APK downloads progress be measured in kilobytes instead of bytes

* Add missing Sardinian language to the preferences

* Fix minimum SDK to be 8 (2.2) instead of 7 (2.1) since support for version 7
  was dropped long ago

### 0.91 (2015-05-18)

* Don't request the "Write to external storage" permission on 4.4 and later
  since it's not needed to use our own external app directory

* Fix a crash occuring if the user triggered a repo update that got rid of
  more than 450 APKs at once

* Properly cache APK files on the SD card if configured this way

* Drop support for unsigned repos in favour of signed ones and TOFU support

* Provide better feedback when adding repos with same name of existing repo

* Add support for special Google Play search terms like "pub:" and "pname:"

* Fix regression where adding repos via URLs would not actually add a new repo

* Normalize and check URLs of repos being added

* Don't crash if links on descriptions cannot be handled by any application

* Support building as part of a ROM via an Android.mk using Gradle

### 0.88 (2015-04-28)

* Show list of apps in the update notification (on devices with
  Android 4.1 or higher)

* User interface language can now be changed from inside the F-Droid
  preferences without changing the system language (locale)

* Fix an issue where XML files could pile up in the data directory

* Improve app and search link handling while also adding supporting for Amazon
  and Google Play links

* Fix regression where F-Droid web repo links would trigger an "Add new repo"
  action

* Show a message to the user when there are no apps to display.

* Swapping is now two-way. Connecting to a swap on one device will
  initiate a swap on the other device

* Small UI fixes to avoid overlapping text and improve app version ellipsizing

* Split up search terms when querying the app database—"fire fox" now
  matches FireFox

* Ignore trailing paces in search terms introduced by some input methods

* Fixed bug where categories were always empty on non-english locales

* Only log some verbose messages that are of little use to users in debug builds

* Misc fixes to the "swap" workflow (especially on Android 2.3 devices)

### 0.83 (2015-03-26)

* Fix possible crashes when installing or uninstalling apps

* Fix issue that caused the installed state label to sometimes not be updated

* Support for future devices with more than two CPU architectures

* Show when packages are installed but not via F-Droid (mismatching signature)

* Fix possible backround crash concerning the category list change listener

* Add an option to check for repository updates less often

* Get rid of the confusing checkbox on/off descriptions

* Enable building F-Droid without having to build all dependencies yourself

* Temporarily remove partially translated arrays to avoid potential crashes

* Translation updates

### 0.78 (2014-12-31)

* Fix repo updates on 5.0 (which caused no apps to show on clean installs)

* "Local repo" has an improved interface making it simpler to swap apps between
  devices and the "Start Swap" menu item opens a wizard to help with the process

* Be more verbose when encountering repo index update errors

* Bump the Target SDK to 21

* Update Universal-Image-Loader and the Support libraries

* Switch the directory structure to better suit building with Gradle

* Translation updates

### 0.76 (2014-10-08)

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
  (NFC+Bluetooth), also compatible with Samsung/HTC S Beam

* Initial support for root and system installers, allowing the client to
  install APKs directly on its own

* Increased performance when updating from repository with many apps

* Switch to AppCompat from the Support library

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

* NFC support: Beam repo configs from the repo detail view (Android 4.0+),
  beam the F-Droid.apk from F-Droid's main screen (Android 4.1+)

* Support for repositories using self-signed HTTPS certificates through
  a Trust-on-first-use popup

* Support for TLS Subject-Public-Key-Identifier pinning

* Add native Right-to-Left support on devices running 4.2 and later

* Filter app compatibility by maxSdkVersion too

* Major internal changes to enable F-Droid to handle repos with thousands
  of apps without slowing down too much. These internal changes will also make
  new features easier to implement

* Various fixes to layout issues introduced in 0.58

* Translation updates

### 0.58 (2014-01-11)

* Download icons with a resolution that matches the device's screen density,
  which saves resources on smaller devices and gets rid of unnecessary
  blurriness on larger devices

* Tweaked some layouts, especially the app lists and their compact layout

* App lists now show more useful version information: Current version names,
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

* Support for Dogecoin donation method added (wow)

* Don't keep app icons older than 30 days in disc cache

* Always include incompatible APKs in memory to avoid issues with apps
  seemingly not having any APKs available

* Fixed a crash when trying to access a non-existing app

* F-Droid registers with Android to receive F-Droid URIs https://\*/fdroid/repo
  and fdroidrepos://

* Support including signing key fingerprint in repo URIs

* When adding new repos that include the fingerprint, check to see whether
  that repo exists in F-Droid already, and if the fingerprints match

* Other minor bug fixes

* Lots of translation updates

### 0.55 (2013-11-11)

* Fixed problems with category selection and permission lists on Android 2.X devices.

* Lots of translation updates, including new Norwegian translation

### 0.54 (2013-11-05)

* New options on the App Details screen to ignore all future updates for that
  particular app, or ignore just the current update

* Apps with Anti-features are no longer hidden, and the corresponding
  preferences to unhide them are removed. Instead they are clearly marked on the
  App Details screen.

* Apps with incompatible native code architecture requirements are now correctly
  filtered.

* A bug that prevented update notifications from appearing has been fixed

* Theming support, with Light and Dark themes.

* New launcher and notification icons, and new default/loading app icon. Icons
  are now retrieved dynamically, drastically improving startup time on first
  installation.

* All app donation options have been grouped into a submenu, and Litecoin
  donation support has been added

* App filter settings now take effect immediately

* APK native code ABIs are now shown in expert mode

* Search URIs for market://search and fdroid.search: are now handled

* A problem with ActionBar Up navigation on some devices has been fixed

* Other minor bug fixes, and adjustments to spacings and layouts

* Lots of translation updates

### 0.50 (2013-08-20)

* New basic app sharing functionality

* Handle f-droid.org web repo as well as market:// app URIs

* Search by just typing on main screen and search results screen

* Flattr and bitcoin donation methods added

* Noticeable speedups when returning from installs and uninstalls

* Add back to home buttons to the ActionBar

* Don't recommend versions newer than the current or incompatible with the device

* Use standard Android cache locations rather than .fdroid on the SD card

* Fix for crash at boot time where the SD card was slow to initialise

* Lots of bug fixes

* Lots of translation updates
