#!/usr/bin/env monkeyrunner

from __future__ import with_statement
from monkey import WearDevice
import subprocess
import os
import sys
import glob
from unicodecsv import UnicodeReader

# TODO: please replace with the actual serial number of the handheld device
HANDHELD_SERIALNO = '00944424b877b2ce'

APK_DIR = '../apks/wear'
HANDHELD_APK_DIR = '../apks/handheld'
WEAR_SERIALNO = '127.0.0.1:4444'

apks = {}
labels = {}
done = []


def read_labels():
    with open('pkg_wfs_label.csv', 'rb') as f:
        i = 0
        reader = UnicodeReader(f)
        for row in reader:
            i += 1
            pkg = row[0]
            label = row[2]
            labels[pkg] = label


def read_done():
    for f in glob.glob('./screenshots/*.ambient.png'):
        pkg = f.split('/')[-1][:-len('.ambient.png')]
        done.append(pkg)


def read_apks(d):
    with open(d) as f:
        for line in f:
            line = line.strip()
            if len(line) == 0 or line.startswith('#'):
                continue
            line = line.split(' ')[0]
            pkg = line.split('/')[-1][:-len('.apk')]
            # print('.............' + pkg)
            if pkg in done:
                continue
            apks[pkg] = labels[pkg]


def install_wait_screenshot(i, pkg, wearable):
    print('[%5d] %s' % (i, pkg))
    apk = '%s/%s.apk' % (APK_DIR, pkg)
    handheldApk = '%s/%s.apk' % (HANDHELD_APK_DIR, pkg)
    wearable.install_on_device(HANDHELD_SERIALNO, pkg, handheldApk)
    wearable.adb_install(pkg, apk)
    wearable.sleep(5)

    # print(apks)
    wearable.select_on_handheld(HANDHELD_SERIALNO, apks[pkg])

    wearable.wake()
    wearable.sleep(8)
    wearable.wake()
    screenshot = wearable.takeSnapshot()
    screenshot.writeToFile('screenshots/%s.interactive.png' % pkg, 'png')
    print('Snapshot: screenshots/%s.interactive.png' % pkg)
    wearable.sleep(26)
    screenshot = wearable.takeSnapshot()
    screenshot.writeToFile('screenshots/%s.ambient.png' % pkg, 'png')
    print('Snapshot: screenshots/%s.ambient.png' % pkg)

    wearable.adb_uninstall(pkg)
    wearable.uninstall_on_device(HANDHELD_SERIALNO, pkg)


def main():
    subprocess.call(['./connect_wear.sh'])
    read_labels()
    read_done()
    read_apks('./display.leak.txt')
    wearable = WearDevice(WEAR_SERIALNO, debug=True)
    i = 0
    for apk in sorted(apks):
        i += 1
        install_wait_screenshot(i, apk, wearable)


if __name__ == '__main__':
    main()
