#!/usr/bin/env python3
#
# Takes localized screenshots on a device and saves them to the proper directory.
#
# Careful: this removes ../screenshots folder if it exists. Also removes screenshots from device.
#
import shutil
import subprocess
from pathlib import Path

script_dir = Path(__file__).parent.resolve()
screenshots_dir = script_dir.parent / 'screenshots'

shutil.rmtree(screenshots_dir, ignore_errors=True)
subprocess.run(["adb", "shell", "rm", "-r", "/sdcard/Android/data/org.fdroid.basic.debug/files/screenshots"])
subprocess.run(["./screenshot-demo-mode.sh", "on"], cwd=script_dir)
try:
    print("Running connectedAndroidTest to take screenshots...")
    subprocess.run(["./gradlew", ":app:connectedBasicDefaultDebugAndroidTest",
                    "-Pandroid.testInstrumentationRunnerArguments.fdroid_screenshots=true"],
                   cwd=script_dir.parent, check=True)

    print("Downloading screenshots...")
    screenshots_dir = script_dir.parent / 'screenshots'
    subprocess.run(
        ["adb", "pull", "/sdcard/Android/data/org.fdroid.basic.debug/files/screenshots", script_dir.parent],
        cwd=script_dir.parent, check=True)

    print("Running pngquant on screenshots...")
    screenshots = [str(f) for f in screenshots_dir.glob('basic/fastlane/metadata/android/*/images/*/*')]
    for screenshot in screenshots:
        subprocess.run(["pngquant", "--force", "--ext", ".png", "--skip-if-larger", "--speed", "1", screenshot])

    target_dir = script_dir.parent / 'src'
    old_screenshots = [str(f) for f in target_dir.glob('basic/fastlane/metadata/android/*/images/phoneScreenshots/*')]
    for s in old_screenshots:
        print(f"Removing old screenshot {s}")
        Path(s).unlink()

    print("Copying new screenshots to target directory...")
    shutil.copytree(screenshots_dir, target_dir, dirs_exist_ok=True)

    subprocess.run(["git", "add", target_dir], cwd=script_dir.parent)
    subprocess.run(["git", "status"], cwd=script_dir.parent)

finally:
    subprocess.run(["./screenshot-demo-mode.sh", "off"], cwd=script_dir)
