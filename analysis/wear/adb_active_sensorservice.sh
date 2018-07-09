#!/bin/bash

adb -s 127.0.0.1:4444 shell dumpsys sensorservice | sed -n '/Active sensors/,/Previous Registrations/p'
