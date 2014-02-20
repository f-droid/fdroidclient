These issues are a must-fix before the next stable release:

* Right after updating a repo, `Recently Updated` shows the apps correctly but
  the new apks don't show up on App Details until the whole app is restarted
  (or until the repos are wiped and re-downloaded)

* `App.curVersion` is now used in some places where before we used
  `App.curApk.version`, which means that e.g. app lists now show the current
  version at upstream and not the latest stable version in the repository
  (highly misleading to users, who might end up looking for versions not in
  the repo yet)
