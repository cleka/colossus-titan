#!/usr/bin/env python2.3
# Needs Python 2.3 for optparse.  (Could use 2.2 and optik)
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


(True, False) = (1, 0)


def normalize_whitespace(text):
    return ' '.join(text.split())


class snapHandler(DefaultHandler):
    def __init__(self):
        self.inReveal = False
        self.inSplit = False
        self.inCreature = False
        self.creatureNames = None
        self.creatureName = None
        self.doneTurn = {}

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
            print '    def testPredictSplits%s(self):' % (options.testcase,)
            print '        print "\\ntest %s begins"' % (options.testcase,)
        else:
            print '    public void testPredictSplits%s()' % (options.testcase,)
            print '    {'

    def printLeaves(self):
        if options.python:
            print '        aps.printLeaves()'
        else:
            print '        aps.printLeaves();'

    def endHistory(self):
        self.printLeaves()
        if options.python:
            print '        print "\\ntest %s ends"' % (options.testcase,)
        else:
            print '    }'

    def startReveal(self, attrs):
        self.markerId = attrs.get("markerId")
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
                print '        ps = PredictSplits("%s", "%s", %s)' % (
                    self.markerId[:2], self.markerId, self.creatureNames)
                print '        aps.add_ps(ps)'
            print '        aps.getLeaf("%s").revealCreatures(%s)' % (
                self.markerId, self.creatureNames)
        else:
            if len(self.creatureNames) == 8:
                print '        cnl.clear();'
                for name in self.creatureNames:
                    print '        cnl.add("%s");' % (name,)
                print '        ps = new PredictSplits("%s", "%s", cnl);' % (
                    self.markerId[:2], self.markerId)
                print '        aps.add_ps(ps);'
            print '        cnl.clear();'
            for name in self.creatureNames:
                print '        cnl.add("%s"); ' % (name,)
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
        self.parentId = attrs.get("parentId")
        self.childId = attrs.get("childId")
        self.turn = attrs.get("turn")
        self.creatureNames = []
        self.inSplit = True

    def endSplit(self):
        if self.inSplit:
            self.doSplit()
            self.creatureNames = None
            self.inSplit = False

    def printTurn(self):
        return not self.doneTurn.has_key(self.turn)

    def doSplit(self):
        numSplitoffs = len(self.creatureNames)
        if options.python:
            if self.printTurn():
                self.printLeaves()
                print '        turn = %s' % (self.turn,)
                print '        print "\\nTurn", turn'
            print '        aps.getLeaf("%s").split(%s, "%s", turn)' % (
                self.parentId, numSplitoffs, self.childId)
        else:
            if self.printTurn():
                self.printLeaves()
                print '        turn = %s;' % (self.turn,)
                print '        Log.debug("Turn " + turn);'
            print '        aps.getLeaf("%s").split(%s, "%s", turn);' % (
                self.parentId, numSplitoffs, self.childId)
        self.doneTurn[self.turn] = 1
            

    def startAddCreature(self, attrs):
        self.markerId = attrs.get("markerId")
        self.creatureName = attrs.get("creatureName")
        self.doAddCreature()

    def doAddCreature(self):
        if options.python:
            print '        aps.getLeaf("%s").addCreature("%s")' % (
                self.markerId, self.creatureName)
        else:
            print '        aps.getLeaf("%s").addCreature("%s");' % (
                self.markerId, self.creatureName)


    def startRemoveCreature(self, attrs):
        self.markerId = attrs.get("markerId")
        self.creatureName = attrs.get("creatureName")
        self.doRemoveCreature()

    def doRemoveCreature(self):
        if options.python:
            print '        aps.getLeaf("%s").removeCreature("%s")' % (
                self.markerId, self.creatureName)
        else:
            print '        aps.getLeaf("%s").removeCreature("%s");' % (
                self.markerId, self.creatureName)


    def startMerge(self, attrs):
        self.splitoffId = attrs.get("splitoffId")
        self.survivorId = attrs.get("survivorId")
        self.turn = attrs.get("turn")
        self.doMerge()

    def doMerge(self):
        if options.python:
            if self.printTurn():
                print '        turn = %s' % (self.turn,)
                print '        print "\\nTurn", turn'
            print '        aps.getLeaf("%s").merge(aps.getLeaf("%s"), 
                turn)' % (self.survivorId, self.splitoffId)
        else:
            if self.printTurn():
                print '        turn = %s;' % (self.turn,)
                print '        Log.debug("Turn " + turn);'
            print '        aps.getLeaf("%s").merge(aps.getLeaf("%s"), 
                turn);' % (self.survivorId, self.splitoffId)
        self.doneTurn[self.turn] = 1



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

    if len(args) == 0:
        infile = sys.stdin
    else:
        infile = open(args[0])

    dh = snapHandler()
    parser = make_parser()
    parser.setContentHandler(dh)
    parser.parse(infile)

if __name__ == '__main__':
    main()

