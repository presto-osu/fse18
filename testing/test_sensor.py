#!/usr/bin/env monkeyrunner

from __future__ import with_statement
from monkey import WearDevice, logger
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

labels = {}


def read_labels():
    with open('pkg_wfs_label.csv', 'rb') as f:
        i = 0
        reader = UnicodeReader(f)
        for row in reader:
            i += 1
            pkg = row[0]
            label = row[2]
            labels[pkg] = label


def install_test(pkg, wearable):
    print('%s' % pkg)

    orig_sensors = wearable.read_associated_sensors()
    # for s in orig_sensors:
    #     logger.i('Before sensor: %s' % s)

    apk = '%s/%s.apk' % (APK_DIR, pkg)
    handheldApk = '%s/%s.apk' % (HANDHELD_APK_DIR, pkg)
    wearable.install_on_device(HANDHELD_SERIALNO, pkg, handheldApk)
    wearable.adb_install(pkg, apk)
    wearable.sleep(8)

    wearable.select_on_handheld(HANDHELD_SERIALNO, labels[pkg])
    wearable.wake()
    wearable.sleep(8)
    wearable.wake()

    ###################################################
    ###################################################
    # MAIN TEST BODY
    # TODO: please copy the generated test cases in the log
    # For example:
    wearable.deselect_on_handheld(HANDHELD_SERIALNO)

    ###################################################
    ###################################################
    # verify sensor leaks
    wearable.sleep(8)
    curr_sensors = wearable.read_associated_sensors()
    # for s in orig_:
    #     logger.i('After sensor: %s' % s)
    diffset = curr_sensors - orig_sensors
    if len(diffset) != 0:
        logger.w("Verified: leak following sensor resources:")
        for item in diffset:
            logger.w(item)
    else:
        logger.i("Verified: no leak of sensors.")

    # uninstall
    wearable.adb_uninstall(pkg)
    wearable.uninstall_on_device(HANDHELD_SERIALNO, pkg)


def main():
    subprocess.call(['./connect_wear.sh'])
    read_labels()
    wearable = WearDevice(WEAR_SERIALNO, debug=True)

    # TODO: please replace the package name
    # install_test('package.name', wearable)
    # For example:
    install_test('wear.trombettonj.trombt1pearlfree', wearable)


if __name__ == '__main__':
    main()
