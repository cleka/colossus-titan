#!/usr/bin/env python
# Probably needs 2.3

# Convert .ter file to XML

import sys

class Terrain:
    display_order = ['name', 'display_name', 'color', 'regular_recruit']
    def __init__(self, line):
        atoms = line.split()
        self.color = atoms[0]
        self.name = atoms[1]
        self.regular_recruit = (atoms[2] == 'true')
        self.recruits = []
        pairs = atoms[3:]
        if len(pairs) & 1 == 1:
            self.display_name = pairs.pop()
        while pairs:
            number = pairs.pop(0)
            name = pairs.pop(0)
            self.recruits.append((name, number))

    def __str__(self):
        s = '<terrain '
        for key in self.display_order:
            if hasattr(self, key):
                s += ('%s="%s" ' % (key, getattr(self, key)))
        s += '>\n'
        for (name, number) in self.recruits:
            s += '        <recruit name="%s" number="%s" />\n' % (name, number)
        s += '    </terrain>'
        return s

class Acquirable:
    def __init__(self, atoms):
        self.points = atoms.pop(0)
        self.name = atoms.pop(0)
        self.terrain = None
        if atoms:
            next = atoms.pop(0)
            try:
                int(next)
            except:
                self.terrain = next
            else:
                atoms.insert(0, next)

    def __str__(self):
        s = '<acquirable name="%s" points="%s"' % (self.name, self.points)
        if self.terrain:
            s += ' terrain="%s"' % self.terrain
        s += ' />' 
        return s

def print_header():
    print '<?xml version="1.0"?>'
    print '<terrains>'

def print_footer():
    print '</terrains>'

def handle_line(line):
    line = line.strip()
    if not line:
        pass
    elif line.startswith('#'):
        print '<!-- ' + line[1:] + ' -->'
    elif line.startswith("ACQUIRABLE"):
        atoms = line.strip().split()[1:]
        while atoms:
            acquirable = Acquirable(atoms)
            print '    ' + str(acquirable)

    elif line.startswith("TITANIMPROVE"):
        atoms = line.strip().split()
        points = atoms[1]
        print '    ' + '<titan_improve points="%s" />' % points
    elif line.startswith("TITANTELEPORT"):
        atoms = line.strip().split()
        points = atoms[1]
        print '    ' + '<titan_teleport points="%s" />' % points
    else:
        terrain = Terrain(line)
        print '    ' + str(terrain)

def main(filename):
    fil = file(filename)
    print_header()
    for line in fil:
        handle_line(line)
    print_footer()


if __name__ == '__main__':
    main(sys.argv[1])
