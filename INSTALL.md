# Installation Guide

This is the procedures to run the experiment in the paper. Detailed
description can be found in the `README.md` file in each sub-folder.
We have run all the experiments in advance. All results are included
in the artifact.

## App Market Study

Node.js and Python 3 built with Tk are required. We recommend to download the 
latest stable version of Node.js and use *pyenv* to
[install Python](https://github.com/pyenv/pyenv/wiki/common-build-problems).
Please follow the instructions on their websites for installation. We have
seen problems using some libraries installed via `pip` in Python 2. We suggest
to install them through `apt`, e.g., `apt install python-scrapy`.
If that does not solve the problem, we encourage
you to upgrade to Python 3.

Following libraries are required:

```bash
$ cd app-market-study
$ npm install google-play-scraper
$ pip install scrapy wordcloud beautifulsoup4
```

To fetch the list of Wear apps and watch faces from 
[Android Wear Center](http://www.androidwearcenter.com/)  and [Goko Store](https://goko.me/):

```bash
$ ./run_spider.sh
```

The lists will be saved in `pkg_name_app.txt` and `pkg_name_wf.txt` accordingly.

Next, fetch the details of the apps by:

```bash
$ ./fetch_details.js
```

Note that due to Google's limitations on usage of thier APIs, you might need to 
re-run this script for several times on different machines to fetch all the details.

To generate the chart in Figure 3 in paper:

```bash
$ ./gen_numapps_fig.py
```

The result is located at `number-of-apps.pdf`.

To dump all the reviews of watch faces:

```bash
$ ./extract_reviews.py
```

They are stored in `.reviews_wf.txt`.

To generate the word cloud of Figure 4 in paper:

```bash
$ ./gen_reviews_wf_word_cloud.sh
```

The resulting image is saved as `wordcloud.png`.

## APKs

Chrome is required for downloading APKs from [ApkPure](http://apkpure.com).
First select `apks/handheld` as the default download folder in Chrome. Next
install and run the extension at `apks/chrome_tool`. It will try to download all
the watch face APKs listed in `apks/chrome_tool/pkg_name.txt`. Note that not
all of them are downloadable. We were able to download 1490 of them.

To install the extension, please follow [Chrome's instructions](https://developer.chrome.com/extensions/getstarted):
1. Open the Extension Management page by navigating to `chrome://extensions`.
    * The Extension Management page can also be opened by clicking on the Chrome menu, hovering over **More Tools** then selecting **Extensions**.
2. Enable Developer Mode by clicking the toggle switch next to **Developer mode**.
3. Click the **LOAD UNPACKED** button and select the extension directory `apks/chrome_tool`.
4. Click on the newly added icon ![icon](apks/chrome_tool/icon.png) in the toolbar and it will automatically start to download apps.

After downloading the APKs, run

```bash
$ cd apks
$ ./find_wear_apk.py
```

to extract and store all Wear APKs in `apks/wear`. These APKs contain the
actual watch faces and are used for static analysis. This process may take a
while as APKs need to be unpacked by ApkTool.

## Static Analysis

The latest version (u171) of JDK8 is required.

To build the static analysis:

```bash
$ cd analysis
$ ./gradlew :wear:shadowJar
$ cd ..
```

To run the analysis on one watch face:

```bash
$ ./analysis/gator w /path/to/some.apk
```

To run the analysis on all experimental subjects:

```bash
$ for apk in ./apks/wear/*.apk; do\
$     echo '------------' $apk '-------------';\
$     ./analysis/gator w $apk | tee analysis-logs/$(basename $apk).log;\
$ done
```

All logs will be saved to `analysis-logs`. This may take quite a long time to
finish. Fortunately, we have run the analysis in advance on all APKs and saved
the logs to `analysis-logs`.

To get a list of watch faces containing sensor-related inefficiencies:

```bash
$ grep 'SENSOR LEAK' analysis-logs/*.log | sed 's/.log:.*//g' |\
$     sed 's/^analysis-logs\///g' | sort | uniq
```

To get a list of watch faces containing display-related inefficiencies:

```bash
$ grep 'colors equal=true\|colors not equal, flows to ambient' analysis-logs/*.log |\
$     sed 's/.log:.*//g' | sed 's/^analysis-logs\///g' |\
$     sort | uniq
```

Two pre-generated lists are stored in `testing/sensor.leak.txt` and
`testing/display.leak.txt`.

Test cases can be fetched by grepping 'LEAK TESTCASE' from logs. For example:

```bash
$ grep 'LEAK TESTCASE' analysis-logs/wear.trombettonj.trombt1pearlfree.apk.log |\
$     sed 's/.*: LEAK //g'
```

By copying the test cases to `testing/test_sensor.py`, we can run and validate the
static reports.

## Testing

Latest Android SDK and Python 2.7.15 are required. A handheld device and a
Android Wear smartwatch are a must. The following libraries are also needed:

```bash
$ cd testing
$ pip install beautifulsoup4 Pillow colorama uiautomator futures lxml
```

Make `monkeyrunner` in the SDK executable from commandline:

```bash
$ export PATH="$ANDROID_SDK/tools/bin:$PATH"
```

Before executing tests, first run

```bash
$ ./parse_label.py
```

to fetch the titles for all watch faces. A pre-generated list is stored in
`pkg_wfs_label.csv`. This process may take a while.

After connecting both the handheld device and the smartwatch to the machine
via debug bridge, run

```bash
$ ./test_display.py
```

to automatically install and take screenshots for watch faces that are reported
by static analysis to have potential display-related energy inefficiencies.
`screenshots` contains the pre-generated screenshots.

Run

```bash
$ ./check_ambient_screenshots.py
```

to get final reports of display-related inefficiencies. It crops the square shaped
screenshots and analyze the pixels in them. `crop` contains the cropped images. The
reports will be displayed with color in the output. *Green* means the watch fase is
OK. *Red* means the watch face indeed contains an energy inefficiency.

To run tests for sensor-related energy inefficiencies, copy & paste the test cases
in the log from the static analysis of a watch face, set the package name in
`test_sensor.py`, and run

```bash
$ ./test_sensor.py
```

If there is a sensor leak, you will see *Verified: leak following sensor resources:*
followed by the leaking sensors in the output.

Please refer to the `README.md` in `testing` for more details.

