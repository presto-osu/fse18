#!/usr/bin/env python3
import numpy as np
import matplotlib.pyplot as plt
import csv
import json
import glob
from datetime import datetime
import math
from collections import OrderedDict


def plot(wf, app):
    keys = wf.keys() & app.keys()
    wf = {k: v for k, v in wf.items() if k in keys}
    app = {k: v for k, v in app.items() if k in keys}

    N = len(wf)
    ind = np.arange(N)    # the x locations for the groups
    width = 0.5       # the width of the bars: can also be len(x) sequence

    p1 = plt.bar(ind, wf.values(), width) #, color='#888888')
    p2 = plt.bar(ind, app.values(), width, bottom=tuple(wf.values())) #, color='#bbbbbb')

    plt.ylabel('#Apps')
    plt.xticks(ind, wf.keys())
    plt.xticks(rotation=45)
    plt.yticks(np.arange(0, 6000, 1000))
    plt.legend((p1[0], p2[0]), ('Watch faces', 'Other AW apps'))
    plt.savefig('number-of-apps.pdf', bbox_inches='tight')
    print('Saved to number-of-apps.pdf')


class CumOrderedDict(OrderedDict):
    def __init__(self, *args, **kwargs):
        self.sum = 0
        super(CumOrderedDict, self).__init__(*args, **kwargs)

    def __setitem__(self, key, value):
        self.sum += value
        OrderedDict.__setitem__(self, key, self.sum)


def cummulate(json_dir):
    app = {}
    for fname in glob.glob('%s/*.json' % json_dir):
        with open(fname, 'r') as f:
            a = json.loads(f.read())
            date = datetime.strptime(a['updated'], '%B %d, %Y')
            quarter = math.ceil(date.month/3.)
            key = '%sQ%s' % (date.strftime('%y'), quarter)
            app[key] = app.get(key, 0) + 1
    return CumOrderedDict(sorted(app.items()))


def main():
    app = cummulate('json/apps/details')
    wf = cummulate('json/watchfaces/details')

    plot(wf, app)
    pass


if __name__ == '__main__':
    main()
