#!/usr/bin/env python

""" Convert an image to 2-color, undoing the effect of any previous
    anti-aliasing, to make it easier to convert to SVG.
"""

__version__ = '$Id$'

import sys
import os
import Image  # Requires Python Imaging Library 1.1.3


def convertToPNG(infile):
    outfile = os.path.splitext(infile)[0] + '.png'
    if infile != outfile:
        try:
            Image.open(infile).save(outfile)
        except IOError:
            print 'Cannot convert', infile, 'to PNG'

def convertToSVG(infile):
    """Convert infile to SVG using kvec, which must be in the PATH."""
    kvec = 'kvec'
    SVGfile = os.path.splitext(infile)[0] + '.svg'
    options = '-resolution high -grit 0 -reduce all -quantize 2'
    cmd = '%s %s %s %s' % (kvec, infile, SVGfile, options)
    print cmd
    os.system(cmd)

def brightness(rgb):
    """Return the sum of a (r,g,b) tuple"""
    (r, g, b) = rgb
    return r + g + b

def findBackgroundForeground(im):
    """Return a tuple of the image's two most common colors, with the
       lighter one first.
    """
    counts = {}
    for pixel in im.getdata():
        counts[pixel] = counts.setdefault(pixel, 0) + 1
    countList = [(count, pixel) for (pixel, count) in counts.items()]
    countList.sort()
    countList.reverse()
    bg = countList[0][1]
    fg = countList[1][1]
    if brightness(fg) > brightness(bg):
        (bg, fg) = (fg, bg)
    return (bg, fg)

def shaveBorder(im, bg):
    """Set all edge pixels to the background color"""
    # Top
    y = 0
    for x in range(im.size[0]):
        im.putpixel((x, y), bg)
    # Bottom
    y = im.size[1] - 1
    for x in range(im.size[0]):
        im.putpixel((x, y), bg)
    # Left
    x = 0
    for y in range(im.size[1]):
        im.putpixel((x, y), bg)
    # Right
    x = im.size[0] - 1
    for y in range(im.size[1]):
        im.putpixel((x, y), bg)

def roundPixel(pixel, bg, fg):
    """Change pixel to closer of background or foreground"""
    average = (brightness(fg) + brightness(bg)) / 2.
    if brightness(pixel) > average:
        return bg
    else:
        return fg

def roundAllPixels(im, bg, fg):
    for x in range(im.size[0]):
        for y in range(im.size[1]):
            xy = (x, y)
            pixel = im.getpixel(xy)
            if pixel != bg and pixel != fg:
                newpixel = roundPixel(pixel, bg, fg)
                im.putpixel(xy, newpixel)

def processOneFile(filename):
    im = Image.open(filename).convert('RGB')
    print 'filename', filename, 'format', im.format, 'size', im.size
    (bg, fg) = findBackgroundForeground(im)
    shaveBorder(im, bg)
    roundAllPixels(im, bg, fg)
    outfile = os.path.splitext(filename)[0] + '.gif'
    im.save(outfile)
    convertToSVG(outfile)

def processFiles(filenames):
    for filename in filenames:
        processOneFile(filename)


if __name__ == '__main__':
    filenames = sys.argv[1:]
    processFiles(filenames)
