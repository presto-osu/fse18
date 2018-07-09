import scrapy
from bs4 import BeautifulSoup
from glob import glob

class WatchfaceHtmlSpider(scrapy.Spider):
    name = 'watchface-html-spider'

    def start_requests(self):
        '''http://www.androidwearcenter.com/androidwear/watchfaces'''
        url_base = 'http://www.androidwearcenter.com/androidwear/top/apps/page/'
        for x in range(1,200):
            url = url_base + str(x)
            yield scrapy.Request(url=url, callback=self.parse_1)

    def parse_1(self, response):
        url_base = 'http://www.androidwearcenter.com'
        soup = BeautifulSoup(response.body, 'html.parser')
        for h4 in soup.find_all('h4', class_='store-title'):
            href = h4.a['href']
            url = url_base + href
            yield scrapy.Request(url=url, callback=self.parse_2)

    def parse_2(self, response):
        url_base = 'http://www.androidwearcenter.com'
        soup = BeautifulSoup(response.body, 'html.parser')
        for a in soup.find_all('a', attrs={'class': 'btn', 'target': '_blank', 'rel': 'nofollow'}):
            href = a['href']
            url = url_base + href
            yield scrapy.Request(url=url, callback=self.parse_3)

    def parse_3(self, response):
        # the response is the google play page
        base = response.url.split('=')[-1]
        filename = '%s.html' % base
        with open('html/apps/' + filename, 'wb') as f:
            f.write(response.body)
        print('Saved file %s' % filename)

