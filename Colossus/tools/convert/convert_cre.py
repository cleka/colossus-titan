#!/usr/bin/env python
# Probably needs 2.3

# Convert .cre file to XML

import sys

class Creature:
    display_order = ['name', 'power', 'skill', 'rangestrikes', 'flies', 
      'bramble', 'drift', 'bog', 'sanddune', 'slope', 'volcano', 'river', 
      'stone', 'tree', 'water', 'magic_missile', 'summonable', 'lord', 
      'demilord', 'count', 'plural_name', 'base_color']

    def __init__(self, line):
        (self.name, self.power, self.skill, self.rangestrikes, 
        self.flies, self.bramble, self.drift, self.bog, self.sanddune, 
        self.slope, self.volcano, self.river, self.stone, self.tree, 
        self.water, self.magic_missile, self.summonable, self.lord, 
        self.demilord, self.count, self.plural_name, 
        self.base_color) = line.split()

    def __str__(self):
        li = []
        li.append('<creature')
        for key in self.display_order:
            li.append('%s="%s"' % (key, getattr(self, key)))
        li.append('/>')
        return ' '.join(li)

def print_header():
    print '<?xml version="1.0"?>'
    print '<creatures>'

def print_footer():
    print '</creatures>'

def handle_line(line):
    line = line.strip()
    if line.startswith('#'):
        line = line.strip()
        if line:
            print '<!-- ' + line[1:] + ' -->'
    elif not line:  # skip blank lines
        pass
    else:
        creature = Creature(line)
        print '    ' + str(creature)

def main(filename):
    fil = file(filename)
    print_header()
    for line in fil:
        handle_line(line)

    print_footer()

if __name__ == '__main__':
    main(sys.argv[1])
