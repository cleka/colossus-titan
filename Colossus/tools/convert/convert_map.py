#!/usr/bin/env python
# Probably needs 2.3

# Convert .map file to XML

import sys

class Hex:
    def __init__(self, atoms):
        self.label = atoms.pop(0)
        self.terrain = atoms.pop(0)
        self.exit_labels = []
        self.exit_types = []
        for unused in range(3):
            self.exit_labels.append(atoms.pop(0))
            self.exit_types.append(atoms.pop(0))
        while self.exit_labels[-1] == '0':
            self.exit_labels.pop()
            self.exit_types.pop()
        self.xpos = atoms.pop(0)
        self.ypos = atoms.pop(0)

    def __str__(self):
        s = '    <hex label="%s" terrain="%s" xpos="%s" ypos="%s">\n' % (
          self.label, self.terrain, self.xpos, self.ypos)
        for ii, exit_label in enumerate(self.exit_labels):
            exit_type = self.exit_types[ii]
            s += '        <exit type="%s" label="%s" />\n' % (exit_type, 
              exit_label)
        s += '    </hex>' 
        return s

def print_header():
    print '<?xml version="1.0"?>'

def print_footer():
    print '</board>'

def handle_line(line):
    line = line.strip()
    if not line:
        pass
    elif line.startswith('#'):
        print '<!-- ' + line[1:] + ' -->'
    else:
        atoms = line.split()
        if len(atoms) == 2:
            width = atoms[0]
            height = atoms[1]
            print '<board width="%s" height="%s" >' % (width, height)
        else:
            hex = Hex(atoms)
            print hex



def main(filename):
    fil = file(filename)
    print_header()
    for line in fil:
        handle_line(line)
    print_footer()


if __name__ == '__main__':
    main(sys.argv[1])
