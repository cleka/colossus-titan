#!/usr/bin/env python
# Probably needs 2.3

# Convert .var file to XML

import sys
import os.path


def doMap(filename):
    print '    <strategic_map filename="%s" />' % filename

def doCre(filename):
    print '    <creatures filename="%s" />' % filename

def doTer(filename):
    print '    <terrain_recruits filename="%s" />' % filename

def doHint(hints):
    li = hints.split(',')
    li = [hint.strip() for hint in li]
    print '    <hints>'
    for hint in li:
        print '        <hint filename="%s" />' % hint
    print '    </hints>'

def doMaxPlayers(num):
    print '    <max_players num="%s" />' % num

def doDepend(depends):
    li = depends.split(',')
    li = [depend.strip() for depend in li]
    print '    <depends>'
    for depend in li:
        print '        <depend variant="%s" />' % depend
    print '    </depends>'

dispatcher = {
    'MAP': doMap,
    'CRE': doCre,
    'TER': doTer,
    'HINT': doHint,
    'MAXPLAYERS': doMaxPlayers,
    'DEPEND': doDepend,
}


def print_header(variant):
    print '<?xml version="1.0"?>'
    print '<variant name="%s">' % variant

def print_footer():
    print '</variant>'

def handle_line(line):
    line = line.strip()
    if not line:
        pass
    elif line.startswith('#'):
        print '<!-- ' + line[1:] + ' -->'
    else:
        atoms = line.split(':')
        atoms = [atom.strip() for atom in atoms]
        fun = atoms[0]
        args = atoms[1:]
        if dispatcher.has_key(fun):
            funct = dispatcher[fun]
            funct(*args)


def main(filename):
    basename = os.path.basename(filename)
    variant = basename.split('.')[0]
    print_header(variant)
    fil = file(filename)
    for line in fil:
        handle_line(line)
    print_footer()


if __name__ == '__main__':
    main(sys.argv[1])
