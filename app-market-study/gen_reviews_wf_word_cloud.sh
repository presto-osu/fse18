#!/bin/bash

./flatten_wf_reviews.py
wordcloud_cli.py --background white --stopwords stop_words.txt --text .reviews_wf.txt --imagefile wordcloud.png
