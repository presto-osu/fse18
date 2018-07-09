#!/usr/bin/env python3

from glob import glob
import json

with open('.reviews_wf.txt', 'w') as rf:
    for json_doc in glob('json/watchfaces/reviews/*.json'):
        with open(json_doc, 'r') as f:
            reviews = json.load(f)
            for r in reviews:
                # print(r[u'comment'].encode('utf-8'), file=rf)
                print(r[u'comment'], file=rf)

