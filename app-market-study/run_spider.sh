#!/bin/bash

scrapy runspider --logfile=.log ./awc_spider_wf.py
scrapy runspider --logfile=.log ./awc_spider_app.py
scrapy runspider --logfile=.log ./goko_spider_wf.py
scrapy runspider --logfile=.log ./goko_spider_app.py

ls html/watchfaces/ | sed 's/\.html//g' > .pkg_name
cat .pkg_name | sort | uniq > pkg_name_wf.txt

ls html/apps/ | sed 's/\.html//g' >> .pkg_name
cat .pkg_name | sort | uniq > .pkg_name_all.txt

declare -a wfarray
readarray -t wfarray < pkg_name_wf.txt

declare -a allarray
readarray -t allarray < .pkg_name_all.txt

echo ${allarray[@]} ${wfarray[@]} | tr ' ' '\n' | sort | uniq -u > pkg_name_app.txt

