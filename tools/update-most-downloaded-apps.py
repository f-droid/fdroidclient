#!/usr/bin/env python3
#
# Fetch the list of most downloaded apps

import os
import requests
import json

os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

r = requests.get('https://grote.gitlab.io/fdroid-metrics-distilled/top/50.json')
with open('app/src/main/assets/most_downloaded_apps.json', "w") as f:
    json.dump(r.json(), f, indent=2)
