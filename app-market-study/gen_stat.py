#!/usr/bin/env python3

import json
import glob
from datetime import datetime


def run(json_dir, stat_file):
    with open(stat_file, 'w') as sf:
        print(','.join(['app','date','free','min','max','score','reviews']), file=sf)

        details = []
        for fname in glob.glob('%s/*.json' % json_dir):
            with open(fname, 'r') as f:
                details.append(json.loads(f.read()))

        for app in details:
            d = []
            d.append(app['appId'])
            d.append(datetime.strptime(app['updated'],
                                       '%B %d, %Y').strftime('%Y-%m-%d'))
            if app['free']:
                d.append('free')
            else:
                d.append('paid')
            d.append(str(app['minInstalls']))
            d.append(str(app['maxInstalls']))
            d.append(str(app['score']))
            d.append(str(app['reviews']))
            print(','.join(d), file=sf)


def main():
    run('json/watchfaces/details', 'stat-wf.csv')
    run('json/apps/details', 'stat-app.csv')


if __name__ == '__main__':
    main()
