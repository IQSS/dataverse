#!/usr/bin/env python
#
# Download features.tsv like this:
# curl -L "https://docs.google.com/spreadsheets/d/1EIFGAfDfZAboFa3_ShRfgoT6xSDpKohDH2_iCyO5MtA/export?gid=729532473&format=tsv" > features.tsv 
#
# The gid above is a specific tab in this spreadsheet:
# https://docs.google.com/spreadsheets/d/1EIFGAfDfZAboFa3_ShRfgoT6xSDpKohDH2_iCyO5MtA/edit?usp=sharing
#
# Here's the README for the spreadsheet:
# https://docs.google.com/document/d/1wqLVoEpnD93Y_wQtA2cQEkAuC0QstC6XVs9XlA7yvbM/edit?usp=sharing
import sys
from optparse import OptionParser
import csv

parser = OptionParser()
options, args = parser.parse_args()

if args:
    tsv_file = open(args[0])
else:
    tsv_file = sys.stdin

print("""# Features

An overview of Dataverse features can be found at <https://dataverse.org/software-features>. This is a more comprehensive list.

```{contents} Contents:
:local:
:depth: 3
```

""")

reader = csv.DictReader(tsv_file, delimiter="\t")
rows = [row for row in reader]
missing = []
for row in rows:
    title = row["Title"]
    description = row["Description"]
    url = row["URL"]
    dtype = row["DocLinkType"]
    target = row["DocLinkTarget"]
    print("## %s" % title)
    print()
    print("%s" % description)
    if target == 'url':
        print("[More information.](%s)" % (url))
    elif target != '':
        print("{%s}`More information.<%s>`" % (dtype, target))
        print()
    else:
        missing.append(url)
tsv_file.close()
for item in missing:
    print(item)
