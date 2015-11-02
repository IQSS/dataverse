#!/usr/bin/env python
import urllib2
import json
import datetime
import csv
import os
from shutil import move
base_url = 'http://localhost:4848/monitoring/domain/server/resources/dvnDbPool'
request = urllib2.Request(base_url, headers = { 'Accept' : 'application/json'})
json1 = urllib2.urlopen(request).read()
data1 = json.loads(json1)
#print json.dumps(data1, indent=2)
war_file = data1['extraProperties']['childResources'].keys()[0]
request = urllib2.Request(base_url + '/' + war_file, headers = { 'Accept' : 'application/json'})
json2 = urllib2.urlopen(request).read()
data2 = json.loads(json2)
#print json.dumps(data2, indent=2)

def highwater(data, metric):
    columns = ['lastsampletime', 'current', 'highwatermark']
    obj = data['extraProperties']['entity'][metric]
    time_readable = epoch2readable (obj, columns[0])
    current = obj[columns[1]]
    highwater = obj[columns[2]]
    filename = metric + '.tsv'
    values = [[time_readable, current, highwater]];
    write_file(metric, columns, values)

def count(data, metric):
    columns = ['lastsampletime', 'count']
    obj = data['extraProperties']['entity'][metric]
    time_readable = epoch2readable (obj, columns[0])
    count = obj['count']
    values = [[time_readable, count]];
    write_file(metric, columns, values)

def epoch2readable(obj, key):
    time_epochsec = obj[key] / 1000.0
    time_readable = datetime.datetime.fromtimestamp(time_epochsec).strftime('%Y-%m-%d %H:%M:%S.%f')
    return time_readable

def write_file(metric, columns, values):
    filename = metric + '.tsv'
    if not os.path.isfile(filename):
        write_header(columns, filename)
    write_values(values, filename)
    uniq(filename)

def write_header(headers, filename):
    with open(filename, 'a') as fp:
        a = csv.writer(fp, delimiter='\t');
        a.writerows([headers]);

def write_values(values, filename):
    with open(filename, 'a') as fp:
        a = csv.writer(fp, delimiter='\t');
        a.writerows(values);

def uniq(filename):
    tmpfile = filename + '.tmp'
    lines_seen = set() # holds lines already seen
    outfile = open(tmpfile, 'w')
    for line in open(filename, 'r'):
        if line not in lines_seen: # not a duplicate
            outfile.write(line)
            lines_seen.add(line)
    outfile.close()
    move(tmpfile, filename)

highwater(data1, 'numconnused')
highwater(data1, 'connrequestwaittime')
count(data1, 'numconnacquired')
count(data1, 'numconnreleased')
