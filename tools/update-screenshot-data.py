#!/usr/bin/env python3
#
# Update data for screenshot creation

import os
import re
import subprocess

import requests

os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def main():
    top_apps = get_top_apps()
    index = get_index()
    packages = index.get("packages", {})

    # Extract packages from index that are in top_apps
    metadata_tuples = [(pkg_name, packages[pkg_name]) for pkg_name in top_apps if
                       pkg_name in packages and "icon" in packages[pkg_name].get("metadata", {})]
    discover_items(metadata_tuples)
    categories(index)
    my_apps(metadata_tuples)
    package_name = "org.fdroid.basic"
    app_details(package_name, packages[package_name]["metadata"])

    # Format files according to official coding style
    subprocess.run(["./gradlew", "ktfmtFormat", "--rerun-tasks"], check=True)


def get_top_apps():
    response = requests.get("https://grote.gitlab.io/fdroid-metrics-distilled/top/50.json")
    response.raise_for_status()
    return response.json()


def get_index():
    response = requests.get("https://f-droid.org/repo/index-v2.json")
    response.raise_for_status()
    return response.json()


def discover_items(metadata_tuples):
    # Sort by 'added' descending, take top 6
    sorted_by_added = sorted(metadata_tuples, key=lambda x: x[1]["metadata"].get("added", 0), reverse=True)
    apps_new = [(package_name, package["metadata"]) for package_name, package in sorted_by_added[:6]]
    already_added = {pkg for pkg, _ in apps_new}

    # take top 6 after those as most downloaded
    remaining = [(package_name, package["metadata"]) for package_name, package in metadata_tuples if
                 package_name not in already_added]
    apps_most_downloaded = remaining[:6]
    for pkg, _ in apps_most_downloaded:
        already_added.add(pkg)

    # take top 6 after those as last updated
    remaining = [(package_name, package["metadata"]) for package_name, package in metadata_tuples if
                 package_name not in already_added]
    apps_last_updated = remaining[:6]

    with open('app/src/androidTest/java/org/fdroid/ui/screenshots/DiscoverItems.kt', "w") as f:
        f.write("""package org.fdroid.ui.screenshots

import androidx.core.os.LocaleListCompat
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.ui.discover.AppDiscoverItem

""")
        f.write(generate_discover_items("getNewApps", apps_new))
        f.write(generate_discover_items("getRecentlyUpdatedApps", apps_last_updated))
        f.write(generate_discover_items("getMostDownloadedApps", apps_most_downloaded))


def generate_discover_items(function_name, pkg_list):
    items = []
    for pkg_name, metadata in pkg_list:
        name_entries = get_localized_map("name", metadata)
        icon_url = get_localized_file("icon", metadata)
        is_installed = "false" if (int(metadata["added"]) / 1000) % 2 == 0 else "true"

        item = (
            f'    AppDiscoverItem(\n'
            f'      packageName = "{pkg_name}",\n'
            f'      name =\n'
            f'        {name_entries}.getBestLocale(localeList) ?: "Unknown App",\n'
            f'      isInstalled = {is_installed},\n'
            f'      imageModel =\n'
            f'        "{icon_url}",\n'
            f'    )'
        )
        items.append(item)

    joined = ",\n".join(items)
    return (
        f"fun {function_name}(localeList: LocaleListCompat) =\n"
        "  listOf(\n"
        f"{joined}\n"
        "  )\n\n"
    )


def categories(index):
    with open('app/src/androidTest/java/org/fdroid/ui/screenshots/CategoryItems.kt', "w") as f:
        f.write("""package org.fdroid.ui.screenshots

import androidx.core.os.LocaleListCompat
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.ui.categories.CategoryItem

""")
        f.write(generate_categories(index))


def generate_categories(index):
    c = index.get("repo", {}).get("categories", {})
    lines = []
    for cat_id, cat_data in c.items():
        name_entries = get_localized_map("name", cat_data)
        lines.append(
            f'CategoryItem(id = "{cat_id}", name = {name_entries}.getBestLocale(localeList) ?: "Unknown Category")'
        )
    joined = ",\n".join(lines)
    return (
        f"fun getCategoryItems(localeList: LocaleListCompat) =\n"
        "  listOf(\n"
        f"{joined}\n"
        "  )\n\n"
    )


def my_apps(pkg_list):
    with open('app/src/androidTest/java/org/fdroid/ui/screenshots/MyAppsItems.kt', "w") as f:
        f.write("""package org.fdroid.ui.screenshots

import androidx.core.os.LocaleListCompat
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.ui.apps.AppUpdateItem
import org.fdroid.ui.apps.InstalledAppItem
import org.fdroid.ui.utils.getPreviewVersion

fun getUpdates(localeList: LocaleListCompat) =
  listOf(
""")
        f.write(generate_app_update_items(pkg_list[:3]))
        f.write(""")
fun getInstalledApps(localeList: LocaleListCompat) =
  listOf(\n""")
        f.write(generate_installed_app_items(pkg_list[3:13]))
        f.write(")")


def generate_app_update_items(pkg_list):
    items = []
    for package_name, data in pkg_list:
        metadata = data["metadata"]
        name_entries = get_localized_map("name", metadata)
        icon_url = get_localized_file("icon", metadata)

        versions = data.get("versions", {})
        first_version_key = next(iter(versions))
        version = versions[first_version_key]
        version_name = version.get("manifest", {}).get("versionName", "1.0")
        size = version.get("file", {}).get("size")

        whats_new_map = version.get("whatsNew", {})
        if whats_new_map:
            whats_new = '"foo bar"'
        else:
            whats_new = "null"

        item = (
            f'AppUpdateItem(\n'
            f'  repoId = 1L,\n'
            f'  packageName = "{package_name}",\n'
            f'  name = {name_entries}.getBestLocale(localeList) ?: "Unknown App",\n'
            f'  installedVersionName = "{reduce_version(version_name)}",\n'
            f'  update = getPreviewVersion(\n'
            f'    versionName = "{version_name}",\n'
            f'    size = {size},\n'
            f'  ),\n'
            f'  whatsNew = {whats_new},\n'
            f'  iconModel = "{icon_url}",\n'
            f'),'
        )
        items.append(item)
    return "\n\n".join(items)


def reduce_version(version: str) -> str:
    parts = re.split(r'(\D+)', version)
    numeric_indices = [i for i, p in enumerate(parts) if p.isdigit()]

    for i in reversed(numeric_indices):
        if int(parts[i]) > 0:
            parts[i] = str(int(parts[i]) - 1)
            break
        else:
            parts[i] = "9"

    return "".join(parts)


def generate_installed_app_items(pkg_list):
    items = []
    for package_name, data in pkg_list:
        metadata = data["metadata"]
        name_entries = get_localized_map("name", metadata)
        icon_url = get_localized_file("icon", metadata)

        versions = data.get("versions", {})
        first_version_key = next(iter(versions))
        version = versions[first_version_key]
        version_name = version["manifest"]["versionName"]
        last_updated = metadata["lastUpdated"]

        item = (
            f'InstalledAppItem(\n'
            f'  packageName = "{package_name}",\n'
            f'  name = {name_entries}.getBestLocale(localeList) ?: "Unknown App",\n'
            f'  installedVersionName = "{version_name}",\n'
            f'  installedVersionCode = 1,\n'
            f'  lastUpdated = {last_updated}L,\n'
            f'  iconModel = "{icon_url}",\n'
            f'),'
        )
        items.append(item)
    return "\n\n".join(items)


def app_details(package_name: str, metadata: dict):
    with open('app/src/androidTest/java/org/fdroid/ui/screenshots/DetailsItem.kt', "w") as f:
        f.write("""package org.fdroid.ui.screenshots

import org.fdroid.database.AppMetadata
""")
        f.write(generate_app_metadata(package_name, metadata))


def generate_app_metadata(package_name: str, metadata: dict) -> str:
    def quoted(key: str) -> str:
        val = metadata.get(key)
        return f'"{val}"' if val else "null"

    cats = metadata.get("categories")
    categories_str = f'listOf({", ".join(f"{chr(34)}{c}{chr(34)}" for c in cats)})' if cats else "null"

    donate = metadata.get("donate")
    donate_str = f'listOf({", ".join(f"{chr(34)}{d}{chr(34)}" for d in donate)})' if donate else "null"

    return (
        f'val appMetadata = AppMetadata(\n'
        f'  repoId = 1L,\n'
        f'  packageName = "{package_name}",\n'
        f'  added = {metadata.get("added", 0)}L,\n'
        f'  lastUpdated = {metadata.get("lastUpdated", 0)}L,\n'
        f'  name = {get_localized_map("name", metadata)},\n'
        f'  summary = {get_localized_map("summary", metadata)},\n'
        f'  description = {get_localized_map("description", metadata)},\n'
        f'  webSite = {quoted("webSite")},\n'
        f'  changelog = {quoted("changelog")},\n'
        f'  license = {quoted("license")},\n'
        f'  sourceCode = {quoted("sourceCode")},\n'
        f'  issueTracker = {quoted("issueTracker")},\n'
        f'  translation = {quoted("translation")},\n'
        f'  preferredSigner = {quoted("preferredSigner")},\n'
        f'  authorName = {quoted("authorName")},\n'
        f'  authorEmail = {quoted("authorEmail")},\n'
        f'  authorWebSite = {quoted("authorWebSite")},\n'
        f'  authorPhone = {quoted("authorPhone")},\n'
        f'  donate = {donate_str},\n'
        f'  liberapayID = {quoted("liberapayID")},\n'
        f'  liberapay = {quoted("liberapay")},\n'
        f'  openCollective = {quoted("openCollective")},\n'
        f'  bitcoin = {quoted("bitcoin")},\n'
        f'  litecoin = {quoted("litecoin")},\n'
        f'  flattrID = {quoted("flattrID")},\n'
        f'  categories = {categories_str},\n'
        f'  isCompatible = true,\n'
        f')\n\n'
        f'val appDetailsIcon = "{get_localized_file("icon", metadata)}"\n'
        f'val appDetailsFeatureGraphic = "{get_localized_file("featureGraphic", metadata)}"\n'
        f'val appDetailsScreenshots = {get_screenshots(metadata)}\n'
    )


def get_localized_map(key: str, data: dict) -> str:
    value = data.get(key)
    entries = ", ".join(
        f'"{locale}" to "{text.replace('"', '\\"').replace(chr(10), "\\n")[:256].rstrip('\\')}"' for locale, text in
        value.items())
    return f"mapOf({entries})"


def get_localized_file(key, metadata):
    icon_data = metadata.get(key, {})
    for locale, file_info in icon_data.items():
        if isinstance(file_info, dict) and "name" in file_info:
            return f"https://f-droid.org/repo/{file_info['name'].lstrip('/')}"
    return ""


def get_screenshots(metadata: dict) -> dict[str, list[str]]:
    phone = metadata.get("screenshots", {}).get("phone", {})
    screenshots = {
        locale: [f"https://f-droid.org/repo{s['name']}" for s in shots]
        for locale, shots in phone.items()
    }
    entries = ", ".join(
        f'"{locale}" to listOf({", ".join(f"{chr(34)}{url}{chr(34)}" for url in urls)})'
        for locale, urls in screenshots.items()
    )
    return f"mapOf({entries})"


if __name__ == "__main__":
    main()
