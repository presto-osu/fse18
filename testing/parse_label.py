#!/usr/bin/env python

from concurrent.futures import ThreadPoolExecutor
from bs4 import BeautifulSoup
import glob
import os
import subprocess
from unicodecsv import UnicodeWriter, UnicodeReader


aapt = '%s/build-tools/28.0.0/aapt' % os.environ['ANDROID_SDK']
apkanalyzer = '%s/tools/bin/apkanalyzer' % os.environ['ANDROID_SDK']

executor = ThreadPoolExecutor(max_workers=4)


def get_label(label, pkg, apk):
    cmd = [aapt, 'd', '--values', '-c', 'default', 'resources', apk]
    output = subprocess.check_output(
        cmd, stderr=subprocess.STDOUT).decode('utf-8')
    found = False
    name = ''
    for line in output.split('\n'):
        if label in line:
            found = True
        elif found:
            if '(string8)' in line:
                if len(name) > 0:
                    print('.............')
                name = line.strip()[len('(string8) '):][1:-1]
            found = False
    return name


i = 0
apps = []


def run(i, apk):
    # get package name
    cmd = [apkanalyzer, 'manifest', 'application-id', apk]
    pkg = subprocess.check_output(
        cmd, stderr=subprocess.STDOUT,).decode('utf-8').strip()
    # decode manifest
    cmd = [apkanalyzer, '--human-readable', 'manifest', 'print', apk]
    output = subprocess.check_output(cmd,
                                     stderr=subprocess.STDOUT).decode('utf-8')
    soup = BeautifulSoup(output, 'xml')
    for tag in soup.find_all(u'application'):
        try:
            app_label = tag[u'android:label']
        except KeyError:
            app_label = ''
    for tag in soup.find_all(u'service'):
        try:
            permission = tag[u'android:permission']
        except KeyError:
            continue
        if permission != u'android.permission.BIND_WALLPAPER':
            continue
        try:
            label = tag[u'android:label']
        except KeyError:
            label = app_label
        if label.startswith(u'@ref/'):
            label = get_label(label[len(u'@ref/'):], pkg, apk)
        name = tag[u'android:name']
        if name.startswith('.'):
            name = pkg + name
        star = apk.split('/')[-1][:-len('.apk')]
        print('[%4d] %s %s "%s"' % (i, star, name, label))
        apps.append([star, name, label])


for apk in glob.glob('../apks/wear/*.apk'):
    i += 1
    run(i, apk)

with open('pkg_wfs_label.csv', 'wb') as f:
    writer = UnicodeWriter(f)
    # writer = csv.writer(f, encoding='utf-8')
    for wf in apps:
        print('[write] %s' % wf)
        writer.writerow(wf)

with open('pkg_wfs_label.csv', 'rb') as f:
    reader = UnicodeReader(f)
    # reader = (f, encoding='utf-8')
    for row in reader:
        print('[read] %s' % row)
