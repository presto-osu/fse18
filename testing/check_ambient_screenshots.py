#!/usr/bin/env python

from __future__ import print_function
from PIL import Image, ImageOps, ImageDraw # pip install Pillow
import glob
from colorama import init, Back # pip install colorama

init()  # intialize colorama
BLACK = 0xa  # what is black? upper bound for rgb
THRESHOLD = 0.90 # below is considered warning


def crop(inFile):
    img = Image.open(inFile)
#    bigsize = (img.size[0] * 3, img.size[1] * 3)
    bigsize = (img.size[0], img.size[1])
    mask = Image.new('L', bigsize, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0) + bigsize, fill=255)
    mask = mask.resize(img.size, Image.ANTIALIAS)
    img.putalpha(mask)
    img.save('crop/%s' % inFile)
    return img


def blk_ratio(inFile):
    img = crop(inFile)
    pixels = img.getdata()
    nblack = 0
    n = len(pixels)
    for i in range(0, n):
        pixel = pixels[i]
        # only consider solid colors
        factor = pixel[3] / 255.0
        if pixel[3] != 255:
            n -= 1
            continue
        if pixel[0] <= BLACK and pixel[1] <= BLACK and pixel[2] <= BLACK:
            nblack += 1
    return (nblack, n, nblack / float(n))

P_R = 1.0/255/3
P_G = 1.0/255/3
P_B = 1.0/255/3

def prf(x):
    return 76.23*x+0.23

def pgf(x):
    return 89.97*x+2.34

def pbf(x):
    return 148.40*x + 7.30

def power_ratio(inFile):
    img = crop(inFile)
    pixels = img.getdata()
    power = 0.0
    solidPixelCount = len(pixels)
    for i in range(0, len(pixels)):
        pixel = pixels[i]
        if pixel[3] != 255:
            solidPixelCount -= 1
            continue
        p_p = 0.0
        p_p += prf(pixel[0] / 255.0)
        p_p += pgf(pixel[1] / 255.0)
        p_p += pbf(pixel[2] / 255.0)
        power += p_p
    return (power, solidPixelCount)


def test_blk_pixels():
    ok = 0
    bug = 0
    for f in sorted(glob.glob('./screenshots/*.ambient.png')):
        (_, _, ratio) = blk_ratio(f)
        if ratio < THRESHOLD:
            bug += 1
            print(Back.RED + '%.4f' % ratio + Back.RESET + ',', end='')
        else:
            ok += 1
            print(Back.GREEN + '%.4f' % ratio + Back.RESET + ',', end='')
        print(f[len('./screenshots/'):-len('.ambient.png')])
    print('#bug: %d' % bug)
    print('#ok:  %d' % ok)


def test_gemma():
    lastSolid = 0
    stats = {}
    for f in sorted(glob.glob('./screenshots/*.ambient.png')):
        (power, solidCount) = power_ratio(f)
        lastSolid = solidCount
        whitelevel = power / (solidCount * (prf(1.0) + pgf(1.0) + pbf(1.0)))
        stats[f[len('./screenshots/'):-len('.ambient.png')]] = whitelevel
        print("file: {0}, power: {1} , level: {2}".format(f[len('./screenshots/'):-len('.ambient.png')], power, whitelevel))
    th = lastSolid / 10 * (prf(1.0) + pgf(1.0) + pbf(1.0)) + lastSolid / 100 * 90 * (prf(0) + pgf(0) + pbf(0))
    th = th / (lastSolid * (prf (1.0) + pgf(1.0) + pbf(1.0)) * 1.0)
    print("Threadthold: %.4f" % th)
    ok = 0
    bug = 0
    for k, v in sorted(stats.items()):
        if v < th:
            ok += 1
            print(Back.GREEN + '%.4f' % v + Back.RESET + ',' + k)
        else:
            bug += 1
            print(Back.RED + '%.4f' % v + Back.RESET + ',' + k)
    print('#bug: %d' % bug)
    print('#ok:  %d' % ok)

if __name__ == "__main__":
    test_blk_pixels()
    test_gemma()


