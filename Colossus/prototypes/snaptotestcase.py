#!/usr/bin/env python2
# Needs Python 2.3
# Needs PyXML

"""snaptotestcase.py [-j] [-p] [-t] filename

-j Output Java (default)
-p Output Python
-t Test case number (default 1)

Outputs a skeleton split prediction unit test based on the history 
within the given Colossus savegame.
"""

__version__ = '$Id$'

import sys
from optparse import OptionParser
from xml.sax import make_parser
from xml.sax.saxutils import DefaultHandler
from sets import Set
import wrap

options = None

def normalize_whitespace(text):
    return ' '.join(text.split())

def wrap_line(line):
    return wrap.wrap_line(line)


class SnapHandler(DefaultHandler):
    def __init__(self):
        DefaultHandler.__init__(self)
        self.inReveal = False
        self.inSplit = False
        self.inCreature = False
        self.creatureNames = None
        self.creatureName = None
        self.doneTurn = Set()
        self.legions = Set()

    def startElement(self, name, attrs):
        if name == "History":
            self.startHistory(attrs)
        elif name == "Reveal":
            self.startReveal(attrs)
        elif name == "Split":
            self.startSplit(attrs)
        elif name == "AddCreature":
            self.startAddCreature(attrs)
        elif name == "RemoveCreature":
            self.startRemoveCreature(attrs)
        elif name == "Merge":
            self.startMerge(attrs)
        elif name == "creature":
            self.startCreature(attrs)

    def characters(self, ch):
        if self.inCreature:
            self.charactersCreature(ch)

    def endElement(self, name):
        if name == "History":
            self.endHistory()
        elif name == "Reveal":
            self.endReveal()
        elif name == "Split":
            self.endSplit()
        elif name == "creature":
            self.endCreature()


    def startHistory(self, attrs):
        if options.python:
            print "    def testPredictSplits%s(self):" % options.testcase
            print "        print '\\ntest %s begins'" % options.testcase
            print
            print "        aps = AllPredictSplits()"
        else:
            print '    public void testPredictSplits%s()' % options.testcase
            print '    {'

    def printLeaves(self):
        if options.python:
            print "        aps.printLeaves()"
        else:
            print '        aps.printLeaves();'

    def addAsserts(self):
        sorted = list(self.legions)
        sorted.sort()
        if options.python:
            longstr = "aps.getLeaf('%s').numUncertainCreatures(), 0"
            for legion in sorted:
                expr = longstr % legion
                print "        self.assertEqual(%s)" % expr
        else:
            longstr = 'aps.getLeaf("%s").numUncertainCreatures(), 0'
            for legion in sorted:
                expr = longstr % legion
                print '        assertEquals(%s);' % expr

    def printLeavesAndAddAsserts(self):
        self.printLeaves()
        self.addAsserts()
        self.doneTurn.add(self.turn)


    def printTurnHeader(self):
        if not self.turn in self.doneTurn:
            self.printLeavesAndAddAsserts()
            if options.python:
                print
                print "        turn = %s" % self.turn
                print "        print '\\nTurn', turn"
            else:
                print
                print '        turn = %s;' % self.turn
                print '        Log.debug("Turn " + turn);'

    def endHistory(self):
        self.printLeavesAndAddAsserts()
        if options.python:
            print
            print "        print '\\ntest %s ends'" % options.testcase
        else:
            print
            print '        Log.debug("\\ntest %s ends");'
            print '    }'

    def startReveal(self, attrs):
        self.turn = attrs.get("turn")
        self.printTurnHeader()
        self.markerId = attrs.get("markerId")
        self.legions.add(self.markerId)
        self.wholeLegion = attrs.get("wholeLegion")
        self.allPlayers = attrs.get("allPlayers")
        self.creatureNames = []
        if self.allPlayers == "true":
            self.inReveal = True

    def endReveal(self):
        if self.inReveal:
            self.doReveal()
            self.creatureNames = None
            self.inReveal = False

    def doReveal(self):
        if options.python:
            if len(self.creatureNames) == 8:
                line = "        ps = PredictSplits('%s', '%s', %s)" % (
                    self.markerId[:2], self.markerId, self.creatureNames)
                print wrap_line(line)
                print "        aps.append(ps)"
            line = "        aps.getLeaf('%s').revealCreatures(%s)" % (
                self.markerId, self.creatureNames)
            print wrap_line(line)
        else:
            if len(self.creatureNames) == 8:
                print '        cnl.clear();'
                for name in self.creatureNames:
                    print '        cnl.add("%s");' % name
                print '        ps = new PredictSplits("%s", "%s", cnl);' % (
                    self.markerId[:2], self.markerId)
                print '        aps.add(ps);'
            print '        cnl.clear();'
            for name in self.creatureNames:
                print '        cnl.add("%s"); ' % name
            print '        aps.getLeaf("%s").revealCreatures(cnl);' % (
                self.markerId, )

    def startCreature(self, attrs):
        if self.inReveal or self.inSplit:
            self.inCreature = True
            self.creatureName = ""

    def charactersCreature(self, ch):
        if self.inReveal or self.inSplit:
            self.creatureName += ch

    def endCreature(self):
        if self.inReveal or self.inSplit:
            self.creatureName = normalize_whitespace(self.creatureName)
            self.creatureName = self.creatureName.encode('ascii')
            self.creatureNames.append(normalize_whitespace(self.creatureName))
            self.creatureName = None
            self.inCreature = False


    def startSplit(self, attrs):
        self.turn = attrs.get("turn")
        self.printTurnHeader()
        self.parentId = attrs.get("parentId")
        self.childId = attrs.get("childId")
        self.legions.add(self.childId)
        self.creatureNames = []
        self.inSplit = True

    def endSplit(self):
        if self.inSplit:
            self.doSplit()
            self.creatureNames = None
            self.inSplit = False

    def doSplit(self):
        numSplitoffs = len(self.creatureNames)
        if options.python:
            print "        aps.getLeaf('%s').split(%s, '%s', turn)" % (
                self.parentId, numSplitoffs, self.childId)
        else:
            print '        aps.getLeaf("%s").split(%s, "%s", turn);' % (
                self.parentId, numSplitoffs, self.childId)

    def startAddCreature(self, attrs):
        self.turn = attrs.get("turn")
        self.printTurnHeader()
        self.markerId = attrs.get("markerId")
        self.creatureName = attrs.get("creatureName")
        self.doAddCreature()

    def doAddCreature(self):
        if options.python:
            print "        aps.getLeaf('%s').addCreature('%s')" % (
                self.markerId, self.creatureName)
        else:
            print '        aps.getLeaf("%s").addCreature("%s");' % (
                self.markerId, self.creatureName)


    def startRemoveCreature(self, attrs):
        self.turn = attrs.get("turn")
        self.printTurnHeader()
        self.markerId = attrs.get("markerId")
        self.creatureName = attrs.get("creatureName")
        self.doRemoveCreature()

    def doRemoveCreature(self):
        if options.python:
            print "        aps.getLeaf('%s').removeCreature('%s')" % (
                self.markerId, self.creatureName)
        else:
            print '        aps.getLeaf("%s").removeCreature("%s");' % (
                self.markerId, self.creatureName)


    def startMerge(self, attrs):
        self.turn = attrs.get("turn")
        self.splitoffId = attrs.get("splitoffId")
        self.survivorId = attrs.get("survivorId")
        self.doMerge()

    def doMerge(self):
        if options.python:
            print "        aps.getLeaf('%s').merge(aps.getLeaf('%s'), turn)" \
              % (self.survivorId, self.splitoffId)
        else:
            print '        aps.getLeaf("%s").merge(aps.getLeaf("%s"), turn);' \
              % (self.survivorId, self.splitoffId)

def main():
    usage = "usage: %prog [options] filename"
    optik = OptionParser(usage=usage)
    optik.add_option("-p", "--python", action="store_true", dest="python", 
            default=False, help="output python")
    optik.add_option("-j", "--java", action="store_false", dest="python", 
            help="output java")
    optik.add_option("-t", "--testcase", action="store", type="int", 
            dest="testcase", default=1, help="number of test case")
    global options
    (options, args) = optik.parse_args()
    if len(args) > 1:
        optik.error("incorrect number of arguments")
        return 1

    if len(args) == 0:
        infile = sys.stdin
    else:
        infile = open(args[0])

    parser = make_parser()
    parser.setContentHandler(SnapHandler())
    parser.parse(infile)
    return 0

if __name__ == '__main__':
    sys.exit(main())
