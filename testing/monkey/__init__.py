from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
from com.android.monkeyrunner.recorder import MonkeyRecorder
import subprocess


class Logger:

    def __init__(self, tag, debug=False):
        self.tag = tag
        self.debug = debug

    def i(self, msg):
        print('[ INFO] %s: %s' % (self.tag, msg))

    def d(self, msg):
        if self.debug:
            print('[DEBUG] %s: %s' % (self.tag, msg))

    def e(self, msg):
        print('[ERROR] %s: %s' % (self.tag, msg))

    def w(self, msg):
        print('[ WARN] %s: %s' % (self.tag, msg))


TAG = 'PAI'
logger = Logger(TAG)


class UnsupportedEventException(Exception):

    def __init__(self, msg):
        super(UnsupportedEventException, self).__init__(
            'Unsupported event: %s' % msg)


class SensorElement:
    def __init__(self, pSensorType, pListener):
        self.sensorType = pSensorType
        self.listener = pListener

    def __eq__(self, other):
        if self.sensorType == other.sensorType and self.listener == other.listener:
            return True
        return False

    def __hash__(self):
        return hash(self.sensorType) + hash(self.listener)

    def __str__(self):
        return 'Sensor: %s, Listener: %s.' % (self.sensorType, self.listener)


class WearDevice:

    @property
    def LONG_TIMEOUT(self):
        return 45  # seconds

    @property
    def MED_TIMEOUT(self):
        return 5

    @property
    def SHORT_TIMEOUT(self):
        return 1

    @property
    def HOLD_TIMEOUT(self):
        return 1

    @property
    def SWIPE_DURATION(self):
        return 0.1

    @property
    def SWIPE_STEPS(self):
        return 10

    def __init__(self, device_name='127.0.0.1:4444', debug=False):
        self.mDebug = debug
        if debug:
            logger.debug = True
        while True:
            try:
                self.mDeviceName = device_name
                self.mDevice = MonkeyRunner.waitForConnection(
                    deviceId=device_name)
                self.mWidth = int(self.getProperty('display.width'))
                self.mHeight = int(self.getProperty('display.height'))
            except Exception:
                logger.d('Timeout. Reconnecting %s...' % device_name)
                continue
            break

    def __getattr__(self, name):
        """
        Forwards unknown methods to the original MonkeyDevice object.
        """
        return self.mDevice.__getattribute__(name)

    @staticmethod
    def adb_for_device(device, cmd):
        actual_cmd = ['adb', '-s', device]
        actual_cmd.extend(cmd)
        logger.d(' '.join(actual_cmd))
        subprocess.call(actual_cmd)

    @staticmethod
    def adb_cmd_output(device, cmd):
        actual_cmd = ['adb', '-s', device]
        actual_cmd.extend(cmd)
        logger.d(' '.join(actual_cmd))
        p = subprocess.Popen(
            actual_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        return p.communicate()

    @staticmethod
    def cmd_with_retry(cmd):
        try:
            subprocess.check_call(cmd)
        except subprocess.CalledProcessError:
            logger.i('Selection failed. Second try..')
            subprocess.check_call(cmd)

    @staticmethod
    def install_on_device(serialno, pkg, apkPath):
        if WearDevice.adb_pkg_installed(serialno, pkg):
            logger.w('"%s" already installed on "%s"' % (pkg, serialno))
        else:
            WearDevice.adb_for_device(serialno, ['install', apkPath])

    @staticmethod
    def uninstall_on_device(serialno, pkg):
        WearDevice.adb_for_device(serialno, ['uninstall', pkg])

    @staticmethod
    def adb_pkg_installed(serialno, pkg):
        pkg = 'package:%s' % pkg
        output, err = WearDevice.adb_cmd_output(
            serialno, ['shell', 'pm', 'list', 'packages'])
        for line in output.split('\n'):
            if line == pkg:
                return True
        return False

    def adb_cmd(self, cmd):
        self.adb_for_device(self.mDeviceName, cmd)

    def adb_install(self, pkg, apkPath):
        WearDevice.install_on_device(self.mDeviceName, pkg, apkPath)

    def adb_uninstall(self, pkg):
        WearDevice.uninstall_on_device(self.mDeviceName, pkg)

    def select_on_handheld(self, serialno, title):
        title = title.replace('\\', '')
        python_command = 'import sys\n\
import subprocess\n\
from uiautomator import Device\n\
handheld = Device("%s")\n\
handheld.wakeup()\n\
while not handheld(text="Watch faces",\
                   className="android.widget.TextView",\
                   packageName="com.google.android.wearable.app").exists:\n\
    print("Open Android Wear companion app...")\n\
    subprocess.call(["adb", "-s", "%s", "shell", "am", "start", "-n",\n\
                     "com.google.android.wearable.app/com.google.android.clockwork.companion.launcher.LauncherActivity"])\n\
    handheld(text="More").click() # open watch face list\n\
handheld(scrollable=True).scroll.toEnd() # scroll down to the end\n\
# print("Scroll to [%s]...")\n\
handheld(scrollable=True).scroll.to(textStartsWith="%s", resourceId="com.google.android.wearable.app:id/watch_face_title")\n\
print("Click  on [%s]...")\n\
handheld(textStartsWith="%s").click()' % (serialno, serialno, title, title, title, title)
        logger.d('Selecting %s on %s' % (title, serialno))
        self.cmd_with_retry(['python', '-c', python_command])

    def deselect_on_handheld(self, serialno, title='Astral'):
        '''
        'Astral' watch face is Google's pre-installed watch face. By selecting it, we can simulate the deselection of the 
        current watch face.
        '''
        self.sleep(self.MED_TIMEOUT)
        self.swipe_left()
        self.sleep(self.MED_TIMEOUT)
        title = title.replace('\\', '')
        python_command = 'import sys\n\
import subprocess\n\
from uiautomator import Device\n\
handheld = Device("%s")\n\
handheld.wakeup()\n\
while not handheld(text="Watch faces",\
                   className="android.widget.TextView",\
                   packageName="com.google.android.wearable.app").exists:\n\
    print("Open Android Wear companion app...")\n\
    subprocess.call(["adb", "-s", "%s", "shell", "am", "start", "-n",\n\
                     "com.google.android.wearable.app/com.google.android.clockwork.companion.launcher.LauncherActivity"])\n\
    handheld(text="More").click() # open watch face list\n\
handheld(scrollable=True).scroll.toEnd() # scroll down to the end\n\
# print("Scroll to [%s]...")\n\
handheld(scrollable=True).scroll.to(textStartsWith="%s", resourceId="com.google.android.wearable.app:id/watch_face_title")\n\
print("Click  on [%s]...")\n\
handheld(textStartsWith="%s").click()' % (serialno, serialno, title, title, title, title)
        self.cmd_with_retry(['python', '-c', python_command])

    def record(self):
        MonkeyRecorder.start(self.mDevice)

    def clear_logcat(self):
        self.shell('logcat -c')

    def sleep(self, seconds=5):
        MonkeyRunner.sleep(seconds, self.mDevice)

    def print_properties(self):
        for property in self.getPropertyList():
            logger.i('%s = %s' % (property, self.getProperty(property)))

    def go_back_home(self):
        act = 'android.intent.action.MAIN'
        category = 'android.intent.category.HOME'
        pack = 'com.google.android.wearable.app'
        activity = 'com.google.android.clockwork.home2.activity.HomeActivity2'
        watchface = pack + '/' + activity
        self.startActivity(action=act, component=watchface,
                           categories=[category])

    def standby(self):
        logger.d('event triggered: standby')
        self.sleep(self.LONG_TIMEOUT)

    def swipe(self, start, end, duration, steps):
        xgap = (end[0] - start[0]) / steps
        ygap = (end[1] - start[1]) / steps
        logger.d('gap (%s,%s)' % (xgap, ygap))
        self.touch(start[0], start[1], MonkeyDevice.DOWN)
        for i in range(1, steps + 1):
            self.touch(start[0] + xgap * i,
                       start[1] + ygap * i, MonkeyDevice.MOVE)
            logger.d('move (%s,%s)' %
                     (start[0] + xgap * i, start[1] + ygap * i))
            self.sleep(duration / steps)
        self.touch(end[0], end[1], MonkeyDevice.UP)

    def swipe_right(self):
        logger.d('event triggered: swipe_right (%s,%s)->(%s,%s)' % (50, self.mHeight / 2,
                                                                    self.mWidth - 25, self.mHeight / 2))
        self.swipe((50, self.mHeight / 2),
                   (self.mWidth - 25, self.mHeight / 2),
                   self.SWIPE_DURATION, self.SWIPE_STEPS)

    def swipe_left(self):
        logger.d('event triggered: swipe_left (%s,%s)->(%s,%s)' % (self.mWidth - 50, self.mHeight / 2,
                                                                   50, self.mHeight / 2))
        self.swipe((self.mWidth - 50, self.mHeight / 2),
                   (50, self.mHeight / 2),
                   self.SWIPE_DURATION, self.SWIPE_STEPS)

    def swipe_up(self):
        logger.d('event triggered: swipe_up (%s,%s)->(%s,%s)' % (self.mWidth / 2, self.mHeight - 10,
                                                                 self.mWidth / 2, self.mHeight / 2))
        self.swipe((self.mWidth / 2, self.mHeight - 10),
                   (self.mWidth / 2, self.mHeight / 2),
                   self.SWIPE_DURATION, self.SWIPE_STEPS)
        self.sleep(self.SHORT_TIMEOUT)

    def swipe_down(self):
        logger.d('event triggered: swipe_down (%s,%s)->(%s,%s)' % (self.mWidth / 2, 0,
                                                                   self.mWidth / 2, self.mHeight / 2 + 50))
        self.swipe((self.mWidth / 2, 0),
                   (self.mWidth / 2, self.mHeight / 2 + 50),
                   self.SWIPE_DURATION, self.SWIPE_STEPS)
        self.sleep(self.SHORT_TIMEOUT)

    def palm(self):
        raise UnsupportedEventException('palm')

    def press_side_button(self):
        logger.d('event triggered: press_side_button')
        self.press('KEYCODE_HOME', MonkeyDevice.DOWN_AND_UP)
        self.sleep(self.SHORT_TIMEOUT)

    def press_and_hold_side_button(self):
        logger.d('event triggered: press_and_hold_side_button')
        self.press('KEYCODE_HOME', MonkeyDevice.DOWN)
        self.sleep(self.HOLD_TIMEOUT)
        self.press('KEYCODE_HOME', MonkeyDevice.UP)

    def tap_screen(self, x, y):
        logger.d('event triggered: tap_screen (%s,%s)' % (x, y))
        self.touch(x, y, MonkeyDevice.DOWN_AND_UP)

    def tap_and_hold_screen(self, x, y):
        logger.d('event triggered: tap_and_hold_screen (%s,%s)' % (x, y))
        self.touch(x, y, MonkeyDevice.DOWN)
        self.sleep(self.HOLD_TIMEOUT)
        self.touch(x, y, MonkeyDevice.UP)

    def tap_screen_center(self):
        '''
        Only valid in interactive mode.
        '''
        self.tap_screen(self.mWidth / 2, self.mHeight / 2)

    def tap_and_hold_screen_center(self):
        self.tap_and_hold_screen(self.mWidth / 2, self.mHeight / 2)


    def read_associated_sensors(self):
        output, err = WearDevice.adb_cmd_output(self.mDeviceName, ['shell', 'dumpsys', 'sensorservice'])
        output = output.decode('utf-8', 'strict')
        ret = set()
        first = second = third = False
        for line in output.split('\n'):
            if len(line) != 0:
                if line[:18] == 'Connection Number:':
                    first = True
                elif first:
                    second = True
                    first = False
                elif second:
                    items = line.split('|')
                    listener = items[0].strip()
                    third = True
                    second = False
                elif third:
                    items = line.split('|')
                    sensor_type = items[0].strip()
                    if not self.ignore_sensor(sensor_type, listener):
                        ret.add(SensorElement(sensor_type, listener))
                    third = False
        return ret

    def ignore_sensor(self, sensor_type, listener):
        if listener.startswith('com.android') or listener.startswith('com.google') or listener == 'ick' or len(listener) == 0:
            return True
        return False

    supported_events = {'press_side_button': press_side_button,
                        # 'press_and_hold_side_button': press_and_hold_side_button,
                        'swipe_up': swipe_up,
                        'swipe_down': swipe_down,
                        'swipe_left': swipe_left,
                        'swipe_right': swipe_right,
                        # 'palm_over_screen': palm_over_screen,
                        # 'flick_wrist_in': flick_wrist_in,
                        # 'flick_wrist_out': flick_wrist_out,
                        # 'shake': shake,
                        # 'tilt': tilt,
                        'tap_screen_center': tap_screen_center,
                        # 'tap_and_hold_screen_center': tap_and_hold_screen_center,
                        'standby': standby}
