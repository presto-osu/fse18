#!/usr/bin/env python3

from bs4 import BeautifulSoup
from glob import glob
import json
import concurrent.futures
import subprocess


ncores = int(int(subprocess.check_output(['nproc', '--all'])))
print('Using %d processes' % ncores)
executor = concurrent.futures.ProcessPoolExecutor(max_workers=ncores)


class Review:

    def __init__(self, author, date, permlink, star, title, comment, reply=None):
        self.author = author
        self.date = date
        self.permlink = permlink
        self.star = star
        self.title = title
        self.comment = comment
        self.reply = reply


class Reply:

    def __init__(self, author, date, content):
        self.author = author
        self.date = date
        self.content = content


def percentage2star(s):
    percentage = s.split(' ')[1][:-2]
    if percentage == '20':
        star = 1
    elif percentage == '40':
        star = 2
    elif percentage == '60':
        star = 3
    elif percentage == '80':
        star = 4
    elif percentage == '100':
        star = 5
    else:
        star = 0
    return star


def run(i, html_doc, which):
    # print('[%4d] >>> %s' % (i, html_doc))
    with open(html_doc, 'r') as f:
        reviews = []
        soup = BeautifulSoup(f, 'html.parser')
        all_reviews_div = soup.find('div', class_='all-reviews')
        if all_reviews_div is not None:
            for single_review_div in all_reviews_div.find_all('div', class_='single-review'):
                review_info_div = single_review_div.find(
                    'div', class_='review-info')
                author = review_info_div.find(
                    'span', class_='author-name').string
                if author is None:
                    author = review_info_div.find(
                        'span', class_='author-name').text
                author = author.strip()
                date = review_info_div.find(
                    'span', class_='review-date').string.strip()
                link = review_info_div.find(
                    'a', class_='reviews-permalink')['href']
                rating = review_info_div.find(
                    'div', class_='current-rating')['style']
                rating = percentage2star(rating)
                review_body_div = single_review_div.find(
                    'div', class_='review-body with-review-wrapper')
                title = review_body_div.find('span', class_='review-title')
                comment = title.next_sibling.strip()
                title = title.string
                reply_div = single_review_div.find_next_sibling(
                    'div', class_='developer-reply')
                reply = None
                if reply_div is not None:
                    reply_author = reply_div.find(
                        'span', class_='author-name')
                    reply_date = reply_div.find(
                        'span', class_='review-date')
                    reply_content = reply_author.parent.next_sibling
                    reply = Reply(reply_author.string.strip(
                    ), reply_date.string.strip(), reply_content.string.strip())
                reply = reply.__dict__ if reply is not None else None
                r = Review(author, date, link, rating,
                           title, comment, reply)
                reviews.append(r.__dict__)
        outfile = ('json/%s/reviews/' % which) + \
            html_doc[len('html/%s/' % which):-len('.html')] + '.json'
        with open(outfile, 'w') as out:
            print('[%4d] %s >>> %s' % (i, html_doc, outfile))
            json.dump(reviews, out, sort_keys=True, indent=4)


i = 0
def main(which):
    global i
    for html_doc in glob('html/%s/*.html' % which):
        i += 1
        executor.submit(run, i, html_doc, which)
        # run(i, html_doc, which)


main('watchfaces')
main('apps')
