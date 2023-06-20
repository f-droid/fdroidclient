### 1.17-alpha0 (2023-06-15)

* Many bug fixes
* Show repository icons in list of repositories
* Show reasons for anti-features (where available)
* Unattended upgrades without privileged extension (F-Droid Basic only for now)
* Show app size on app details screen (expert mode only)
* Slightly darker app names in app lists to improve readability
* new targetSdk: 28 for full and 33 for basic

### 1.16.1 (2023-02-27)

* Add IPFS as opt-in Expert Setting, with selected IPFS Gateways built-in #2504

* Migrate ignored updates from old DB, #2538

* Big translation update

### 1.16 (2023-02-08)

* Fix crashes reported via ACRA

* Fix extra install prompt for zero permission apps #2525

### 1.16-alpha4 (2023-01-21)

* Improved swap support when connecting with older F-Droid releases. 

* Stop downloading updates for app with known-vulnerabilities but no update.  #2488

* Migrate repo names #2513

* Wait for database to be available when adding a new repo on slow devices.

### 1.16-alpha3 (2023-01-13)

* Fix assorted crashes, including one related to WiFi Hotspot mode #2477

* Fix including additional repos from the ROM #2503

* Handle users with a thousand apps or more installed #2505

* Fix release channel logic so stable updates are always allowed #2508

### 1.16-alpha2 (2022-12-30)

* Show upgrade notice after resetting the DB due to F-Droid or OS update.

* Stop logging app URLs and filenames (#2500)

* Use new strict release process which does not load dev tools during build

* Fix numerous crashes

### 1.16-alpha1 (2022-12-19)

* Migrate custom repo configuration to new database (#2485)

* Fix random crashes related to WiFi (#2495 #2477)

* Fix disabling of repo mirrors when there are many (#2490)

* Fix OpenCollective links

* Small bits of code modernization

### 1.16-alpha0 (2022-12-02)

* Huge overhaul of download, index, and database code.

* Fixes: #649 #860 #1206 #1588 #1710 #1989 #2080 #2081 #2157 #2322 #2353 #2370
  #2412 #2436 #2442 #2443 #2444 #1971

* Known bugs: #2446 #2447 #2475

### 1.15.6 (2023-01-13)

* Fix crash when app is downloading an app or updating repos in background #2482

* Only load WifiApControl if it is going to be used, avoiding potential crashes.

### 1.15.5 (2022-12-19)

* Fix swap to use index-v1.jar when available #2476

* Fix random crashes related to WifiApControl

### 1.15.4 (2022-11-30)

* Simplify swap index.jar signing to use plain SHA1withRSA

* Ignore XML DTDs and External Entities in index.jar

* Set F-Droid Privileged Extension as the APK installer

* Fix swap, it does not work yet with index-v1.jar

### 1.15.3 (2022-10-25)

* Handle API 33 split-permissions

* Fix crash when loading icons for apps without repo

* Allow nearby swap using modern index formats.

### 1.15.2 (2022-05-18)

* Re-enable HTTP for nearby swapping, fixing #2402

### 1.15.1 (2022-05-11)

* Revert to commons-io v2.6, fixing #2406

### 1.15 (2022-05-03)

* Set navigation bar color to black in dark theme (@ConnyDuck)

* Published org.fdroid:download lib to share our privacy and mirroring tricks.

* Bumped Jackson to v2.11.4 and commons-io to v2.7

### 1.15-alpha1 (2022-04-19)

* Fix crashes in new download code

* Fix Anti-Feature listing being broken in RTL layouts #2279 (thanks @TheLastProject!)

### 1.15-alpha0 (2022-04-06)

* Total overhaul of downloading logic for improved performance and mirrors support.

* Screenshots and graphics are now downloaded from mirrors.

* Reduced a privacy leak with TLS Sessions

### 1.14 (2022-01-24)

* Install Osmand map files without requiring Osmand have Storage permission.

* Fix crash in cache cleanup when disk space is running low.

* Fix UI coloring in Panic settings.

### 1.14-alpha5 (2022-01-19)

* Fix missing back button on Panic and Manage Repos Settings (@splitowo)

* Fix Nearby crash on Android 12

* Handle API 31 split-permissions

* Fix navigation in Basic flavor (@raphaelm)

* Add support for other barcode scanners (@Lugerun)

### 1.14-alpha4 (2021-12-09)

* Hide unwanted antifeatures from latest and categories

* Allow custom Anti-Features from repos to be unhidden

### 1.14-alpha3 (2021-10-22)

* Remove "Allow Push Requests" expert setting, replace with compile-time configuration

* Allow choosing which antifeatures to show

### 1.14-alpha2 (2021-09-10)

* Improved icon management with limited Data/WiFi settings

* Sharing an app URL now uses the correct repo (#1946)

* Nearby swap fixes

### 1.14-alpha1 (2021-09-03)

* Improved Nearby workflow and navigation, with lots of bug fixes.

* Apps without PNG app icons now show in Latest Tab (@ashutoshgngwr)

### 1.14-alpha0 (2021-08-27)

* Overhaul Share menu to use built-in options like Nearby.

* Material design improvements (@proletarius101)

* Improve offline, nearby sharing

* Block adding new repos when device admin disallows "Unknown Sources"

* Fix crash when using repos with username/password

### 1.13.1 (2021-08-05)

* Better tested fix for repeated updates of Trichrome library.

### 1.13 (2021-07-05)

* Overhaul icon loading using Glide (@proletarius101)

* New adaptive icon (@12people)

* Code modernization (@Isira-Seneviratne)

### 1.13-alpha1 (2021-06-02)

* Stop repeated updates of Trichrome Library

* More changes to follow Material Design (@proletarius101)

* Improve OpenCollective badge (@ConnyDuck)

### 1.13-alpha0 (2021-04-22)

* Theme support tied to built-in Android themes (@proletarius101)

* New top banner notifications: "No Internet" and "No Data or WiFi enabled"

* Improved handling of USB-OTG and SD Card repos and mirrors

### 1.12.1 (2021-04-12)

* Fix trove4j verification error

### 1.12 (2021-04-06)

* Sync translations

### 1.12-alpha3 (2021-03-10)

* Opt-in F-Droid Metrics

### 1.12-alpha2 (2021-03-03)

* Overhaul clean up of cached files

* Support updating "shared library packages" like Trichrome (@uldiniad)

### 1.12-alpha1 (2021-02-25)

* Add extra sanitation to search terms to prevent vulnerabilities.

* Fix Nearby Swap's close button (@proletarius101)

* Bump to compileSdkVersion 29 to support Java8

* Set up WorkManager on demand to avoid slowing down starts

* Prefer system keys when APKs are signed by them (@glennmen)

### 1.12-alpha0 (2021-02-08)

* App description localization now fully respects lists of languages in Android
  Language Settings

* Latest Tab lists results based on the Language Settings

* Latest Tab now shows results ordered newest first (@TheLastProject @IzzySoft)

* Theme support modernized and tied to the built-in Android themes (@proletarius101)

* Search results greatly improved (@Tvax @gcbrown76)

* Let Android efficiently schedule background cache cleanup operations (@Isira-Seneviratne)

* Overhaul repo URL parsing for reliable repo adding (@projectgus)

### 1.11 (2020-12-29)

* Improved linkifying of URLs in app descriptions

* Improved handling of SDCards and USG-OTG in Nearby

* Modernized code and switched PNGs to vectors (thanks @isira-seneviratne!)

* Recognize longer repo URLs to support GitCDN/RawGit/etc mirrors

### 1.10 (2020-10-20)

* Improved language selection with multiple locales
  (thanks @spacecowboy and @bubu!)

### 1.10-alpha1 (2020-09-29)

* use notification channels for fine-grained control (@Isira-Seneviratne)

### 1.10-alpha0 (2020-07-20)

* Latest Tab will show better results on non-English devices

* updates to core libraries (Jackson, androidx, gradle, etc)

* use Gradle's new dependency verification

* polish whitelabeling support

### 1.9 (2020-06-25)

* Removed "Android App Link" support since it cannot work with
  F-Droid, and it was triggering DNS leaks.

* Archive Repos are now lower priority than the Repo (higher on the
  Manage Repos screen), fixing issues where it looked for icons,
  screenshots and other information in the Archive rather than the
  Repo itself.

* Fixed hopefully all occurrences where F-Droid client couldn't show an icon.
  The remaining cases of missing icons are now caused either by 
  icons not included in upstream repo or by temporary network failures.
  (After updating this requires one additional repo update to take effect.)

* Fixed a problem where repository updates would never trigger
  when either "Over Data" or "Over Wifi" were disabled.

* Support OpenCollective donation option and highlight 
  free software donation platforms

* Fix for when the app update button wasn't showing up or working 
  in some cases (thanks @di72nn)

* Stop cropping feature header image (thanks @ByteHamster!)

* Make navigation bar match dark mode (thanks @MatthieuB!)

* Cleaned out obsolete code (thanks @Isira-Seneviratne!)

### 1.8-alpha2 (2020-02-04)

* stop showing Unknown Sources with Privileged Extension on Android 10 #1833

* add standard ripple effect to links on app details activity

* fix displaying default icon for apps without icons

### 1.8-alpha1 (2020-01-10)

* handle Android 10 permission config to stop Unknown Sources prompts

* keyboard opens when search is cleared

* translation sync with Android strings

* force common repo domains to HTTPS (GitLab, GitHub, Amazon)

### 1.8-alpha0 (2019-11-20)

* fix seekbar preference on recent Android versions (thanks @dkanada)

* handle API 29 split-permissions: fine location now implies coarse location

* define backup rules to avoid saving the swap repo

### 1.7.1 (2019-07-31)

* fix crashes from ACRA report emails

### 1.7 (2019-07-06)

* fix crash in Panic Settings

* catch random crashes related to WifiApControl

### 1.7-alpha2 (2019-06-18)

* USB OTG flash drives can be used as nearby repos and mirrors 

### 1.7-alpha1 (2019-06-14)

* overhauled nearby swap using the device's hotspot AP

* add new panic responses: app uninstalls and reset repos to default

* fix proxy support on first start

### 1.7-alpha0 (2019-05-20)

* major refactor of "Nearby" UI code, to prepare for rewriting guts

* show "undo" after swiping away items from the Updates tab (thanks @Hocuri!)

* fix ETag handling when connecting to nginx mirrors #1737

* fix issues with "Latest" display caused by mishandling time zones #1757

* ignore all unimportant crashes in background services

* do not use Privileged Extension if it was disabled in Settings

### 1.6.2 (2019-05-20)

* fixed issue where cached indexes were wrongly redownloaded (#1737),
  thanks to @amiraliakbari for tracking it down!

* fixed wrong string for the translated title of the Updates Tab (#1785)

* fixed crashes on very low memory when starting

### 1.6.1 (2019-05-10)

* Updated translations

* fixed button size issues #1678

* stopped random background crashes

### 1.6 (2019-04-10)

* update F-Droid after all other updates (#1556)

* Improve adding repos from the clipboard (e.g. Firefox Klar)

* swap usability improvements

* many crash fixes in swap and background services

### 1.6-alpha2 (2019-03-28)

* Latest Tab now highlights apps that provide descriptions,
  translations, screenshots

* Auto-download from mirrors, to speed up downloads and reduce load on
  f-droid.org

* More efficient download caching (per-repo; across different
  webservers #1708)

* Fix problems canceling downloads (#1727, #1736, #1742)

* Fix downloading OBB files from repos (#1403)

### 1.6-alpha1 (2019-02-20)

* add switches in RepoDetails to disable any or all mirrors (#1696)

* choose random mirror for each package/APK download

* make all APK downloads be cached per-repo, not per-mirror

* handle Apache and Nginx ETags when checking if index is current (#1708)

### 1.6-alpha0 (2019-02-15)

* handle implied READ_EXTERNAL_STORAGE permissions, which trigger a
  permissions prompt on installs with Privileged Extension (#1702)

* sanitize index data to reduce the threats from the server

* set Read Timeout to trigger mirror use when reads are slow

* fix missing icons for those who do not use WiFi (#1592)

* use separate titles for Updates pref and Updates tab, so that they
  can be better translated

* UI fixes from @ConnyDuck (#1636, #1618)

### 1.5.1 (2019-01-07)

* Removed incomplete translations that were accidentally added in 1.5

* Fix screenshot background on dark themes (#1618)

### 1.5 (2018-12-26)

* Nearby swap bug fixes and improvements

* update language and translations about Nearby and swap

* Fix displaying of icons for self-built apps (#1108)

### 1.5-alpha2 (2018-12-21)

* support swapping via SD Cards

* display versionCode in expanded Versions list entries in Expert Mode

### 1.5-alpha1 (2018-12-12)

* UX and language cleanup of App Details

### 1.5-alpha0 (2018-10-19)

* add repos via additional_repos.xml from ROM, OEM, Vendor.

### 1.4 (2018-09-12)

* polish up new "Versions" list and other UI fixes

### 1.4-alpha1 (2018-08-30)

* huge overhaul of the "Versions" list in the App Details screen, and
  many other UI improvements, thanks to new contributor @wsdfhjxc

* fixes to allow keyboard/d-pad navigation in more places, thanks to
  new contributor @doeffinger

### 1.4-alpha0 (2018-08-17)

* show "Open" button when media is installed and viewable

* retry index downloads from mirrors

* add Share button to "Installed Apps" to export CSV list

* add clickable list of APKs to the swap HTML index page 

### 1.3.1 (2018-08-07)

* big overhaul of core nearby/swap plumbing

* TLSv1.3 support, when the device supports it

### 1.3 (2018-07-31)

* large overhaul to make status updates more reliable

* fixed many bugs around the wrong button showing

### 1.3-alpha5 (2018-07-21)

* overhaul install button logic to avoid false presses

* improved first time run experience

* export install/uninstall history

* more whitelabeling improvements

### 1.3-alpha4 (2018-07-13)

* fix Data/WiFi preferences to properly schedule Updates

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

* Detect app updates via system OTA updates (#819)

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

* Fix a crash occurring if the user triggered a repo update that got rid of
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

* Fix possible background crash concerning the category list change listener

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
