# Study of Watch Faces in App Market

This folder contains scripts for the following tasks:
1. Crawling Android Wear apps and watch faces from 
[Android Wear Center](http://www.androidwearcenter.com/) 
and [Goko Store](https://goko.me/), including HTMLs,
details and reviews;
2. Generate statistics of apps and watch faces.
3. Generate the word cloud of reviews of watch faces.

**Note** that the numbers reported by running the scripts
might be different from those in the paper, as the paper
presents the numbers of a market snapshot of March 2018.

## Prerequisites

```bash
$ # install Python 3 with Tk and node.js
$ pip install scrapy wordcloud beautifulsoup4
$ npm install google-play-scraper
```

## Run

```bash
$ ./run_spider.sh # fetch apps from the two stores with Play pages
$ ./fetch_details.js # fetch app details from Play
$ ./gen_numapps_fig.py # generate statistics in Figure 3
$ ./extract_reviews.py # dump reviews from Play pages
$ ./gen_reviews_wf_word_cloud.sh # generate word cloud in Figure 4
```

All the intermediate restuls are stored in the following folders:

- `html` includes the Play pages for all available Android Wear apps
and watch faces;
- `json` contains the details and reviews for the apps and watch faces;

The results of the study can be concluded into two graphs:

- `number-of-apps.pdf` is a chart of market size in every quarter since
the relase of Android Wear;
- `wordcloud.png` shows the word cloud of reviews of all watch faces.

