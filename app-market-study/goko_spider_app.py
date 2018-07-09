import scrapy
from bs4 import BeautifulSoup
from glob import glob

class WatchfaceHtmlSpider(scrapy.Spider):
    name = 'watchface-html-spider'

    def start_requests(self):
        '''
        https://goko.me/index.php?show=store_browse&id_goko=5&mode=watchfaces
        price=0: free and priced
        price=1: free
        price=2: priced
        '''
        url_base = 'https://goko.me/index.php?show=store_browse&id_goko=5&search=&mode=apps&cat=0&price=0&orderby=&page='
        for x in range(1,200):
            url = url_base + str(x)
            yield scrapy.Request(url=url, callback=self.parse_1)

    def parse_1(self, response):
        url_base = 'https://play.google.com/store/apps/details?id='
        soup = BeautifulSoup(response.body, 'html.parser')
        for a in soup.find_all('a'):
            try:
                href = a['href']
            except:
                continue
            if href.startswith(url_base):
                url = href[:-len('&referrer=utm_source=wear_store_by_goko&utm_medium=website')]
                yield scrapy.Request(url=url, callback=self.parse_2)

    def parse_2(self, response):
        base = response.url.split('=')[-1]
        filename = '%s.html' % base
        with open('html/apps/' + filename, 'wb') as f:
            f.write(response.body)
            print('Saved file %s' % filename)

