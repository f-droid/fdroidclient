#!/usr/bin/env python3

import glob
import json
import os
import sys
import jsonschema

os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
with open('app/src/test/resources/cimp.schema.json') as fp:
    schema = json.load(fp)

errors = 0
files = sys.argv[1:]
if not files:
    files = glob.glob(os.path.join(os.getenv('HOME'), 'Downloads', '*.json'))
if not files:
    print('Usage: %s file.json ...' % __file__)
    exit(1)
for f in files:
    print('checking', f)
    with open(f) as fp:
        report = json.load(fp)
    if jsonschema.validate(report, schema) is not None:
        print('ERROR: %s did not validate' % f)
        errors += 1
exit(errors)
