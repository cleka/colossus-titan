#!/usr/bin/env python
# Probably needs 2.3

# Convert battlemap file to XML

import sys
import os.path

class BattleHex:
    def __init__(self, line):
        atoms = line.split()
        self.x = atoms.pop(0)
        self.y = atoms.pop(0)
        self.terrain = atoms.pop(0)
        self.elevation = atoms.pop(0)
        self.borders = {}
        while atoms:
            border_num = atoms.pop(0)
            border_type = atoms.pop(0)
            self.borders[border_num] = border_type

    def __str__(self):
        s = '    <battlehex x="%s" y="%s" terrain="%s" elevation="%s">\n' % (
          self.x, self.y, self.terrain, self.elevation)
        border_nums = self.borders.keys()
        border_nums.sort()
        for border_num in border_nums:
            border_type = self.borders[border_num]
            s += '        <border number="%s" type="%s" />\n' % \
              (border_num, border_type)
        s += '    </battlehex>' 
        return s

def print_header(terrain, istower, subtitle):
    print '<?xml version="1.0"?>'
    if subtitle:
        print '<battlemap terrain="%s" tower="%s" subtitle="%s">' % (
          terrain, istower, subtitle)
    else:
        print '<battlemap terrain="%s" tower="%s">' % (terrain, istower)

def print_footer():
    print '</battlemap>'

def handle_line(line):
    line = line.strip()
    if not line:
        pass
    elif line.startswith('#'):
        print '<!-- ' + line[1:] + ' -->'
    elif line.startswith('TOWER') or line.startswith('SUBTITLE'):
        pass  # Dealt with this at the start
    elif line.startswith('STARTLIST'):
        atoms = line.split()[1:]
        print '    <startlist>'
        for label in atoms:
            print '        <battlehex label="%s" />' % label
        print '    </startlist>'
    else:
        battlehex = BattleHex(line)
        print battlehex


def main(filename):
    if os.path.isdir(filename):
        return
    fil = file(filename)
    lines = fil.readlines()
    istower = False
    subtitle = None
    for line in lines:
        if line.startswith('TOWER'):
            istower = True
        elif line.startswith('SUBTITLE'):
            atoms = line.split(' ', 1)
            subtitle = atoms[1]
            subtitle = subtitle.strip()
            subtitle = subtitle.strip('"')
            subtitle = subtitle.strip()
    terrain = os.path.basename(filename)
    print_header(terrain, istower, subtitle)
    for line in lines:
        handle_line(line)
    print_footer()


if __name__ == '__main__':
    main(sys.argv[1])
