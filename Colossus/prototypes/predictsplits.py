#!/usr/bin/env python2.2

__version__ = "$Id$"

import copy
import sys
import unittest

"""Prototype of Colossus split prediction"""

killValue = { 'Titan':1242, 'Angel':248, 'Archangel':366, 'Behemoth':240,
        'Centaur':122, 'Colossus':404, 'Cyclops':181, 'Dragon':299,
        'Gargoyle':124, 'Giant':285, 'Gorgon':189, 'Griffon':208,
        'Guardian':245, 'Hydra':309, 'Lion':150, 'Minotaur':165,
        'Ogre':121, 'Ranger':169, 'Serpent':361, 'Troll':161,
        'Unicorn':244, 'Warbear':180, 'Warlock':213, 'Wyvern':214 }


def creatureComp(creature1, creature2):
    """Sort creatures in decreasing order of importance.  Keep identical
    creatures together with a secondary sort by creature name."""
    diff = killValue[creature2.name] - killValue[creature1.name]
    if diff != 0:
        return diff
    return cmp(creature1.name, creature2.name)

def superset(big, little):
    """Return True if list big is a superset of list little."""
    for el in little:
        if big.count(el) < little.count(el):
            return False
    return True

def subtractLists(big, little):
    li = big[:]
    for el in little:
        li.remove(el)
    return li

def numCreature(li, creatureName):
    count = 0
    for ci in li:
        if ci.name == creatureName:
            count += 1
    return count

def removeCreatureByName(li, creatureName):
    """Remove the first CreatureInfo matching the passed name.
       Return True if found, False if not."""
    for ci in li:
        if ci.name == creatureName:
            li.remove(ci)
            return True
    return False

def getCreatureInfo(li, creatureName):
    """Return the first CreatureInfo that matches the passed name."""
    for ci in li:
        if ci.name == creatureName:
            return ci
    return None

def getCreatureNames(li):
    names = []
    for ci in li:
        names.append(ci.name)
    return names

def removeLastUncertainCreature(li):
    li.reverse()
    i = 0
    for ci in li:
        if not ci.certain:
            del li[i]    # li.remove(ci) can remove the wrong one
            li.reverse()
            return
        i += 1
    li.reverse()
    raise RuntimeError, "No uncertain creatures"

def mergeCreatureInfoLists(li1, li2):
    """Return a list containing the union of both lists, where the number
       of any duplicate element is the maximum number of that element found
       in either list."""
    if li1 == None and li2 == None:
        return []
    elif li1 == None:
        return li2[:]
    elif li2 == None:
        return li1[:]

    liout = li1[:]
    for ci in li2:
        count = max(numCreature(li1, ci.name), numCreature(li2, ci.name))
        if numCreature(liout, ci.name) < count:
            liout.append(ci)
    return liout


def minCount(lili, name):
    """ lili is a list of lists.  Return the minimum number of times
        name appears in any of the lists contained in lili. """
    min_ = sys.maxint
    for li in lili:
        min_ = min(li.count(name), min_)
    # If we never saw it, reduce to zero
    if min_ == sys.maxint:
        min_ = 0
    return min_


class Perms:
    """ Based on Gagan Saksena's code in online Python cookbook
        Not reentrant or thread-safe.
    """
    def __init__(self):
        self.retlist = []

    def findCombinations(self, alist, numb):
        self.retlist = []
        self._findCombinations(alist, numb, [])
        return self.retlist

    def _findCombinations(self, alist, numb, blist):
        if not numb:
            self.retlist.append(blist[:])
            return
        for i in range(len(alist)):
            blist.append(alist[i])
            self._findCombinations(alist[i+1:], numb-1, blist)
            blist.pop()

    def findPermutations(self, alist, numb):
        self.retlist = []
        self._findPermutations(alist, numb, [])
        return self.retlist

    def _findPermutations(self, alist, numb, blist):
        if not numb:
            self.retlist.append(blist[:])
            return
        for i in range(len(alist)):
            blist.append(alist.pop(i))
            self._findPermutations(alist, numb-1, blist)
            alist.insert(i, blist.pop())


class CreatureInfo:
    def __init__(self, name, certain, atSplit):
        self.name = name
        self.certain = certain
        self.atSplit = atSplit

    def __eq__(self, other):
        """Two CreatureInfo objects match if the names match."""
        return self.name == other.name

    def __str__(self):
        s = self.name
        if not self.certain:
            s += '?'
        if not self.atSplit:
            s += '*'
        return s

    def __repr__(self):
        return self.__str__()

    def __hash__(self):
        """Two CreatureInfo objects match if the names match."""
        return self.name




class Node:
    def __init__(self, markerId, turnCreated, creatures, parent):
        self.markerId = markerId        # Not unique!
        self.turnCreated = turnCreated
        self.creatures = creatures      # list of CreatureInfo
        self.removed = []         # list of CreatureInfo, only if atSplit
        self.parent = parent
        self._clearChildren()

    def _clearChildren(self):
        # All are at the time this node was split.
        self.childSize1 = 0   # childSize1 >= childSize2
        self.childSize2 = 0
        self.child1 = None    # child1 is the presumed "better" legion
        self.child2 = None
        self.turnSplit = -1

    def fullname(self):
        return self.markerId + '(' + str(self.turnCreated) + ")"

    def __str__(self):
        self.creatures.sort(creatureComp)
        s = self.fullname() + ":"
        for ci in self.creatures:
            s += " " + ci.__str__()
        return s

    def __repr__(self):
        return self.__str__()

    def getCertain(self):
        """Return list of CreatureInfo where certain is true."""
        li = []
        for ci in self.creatures:
            if ci.certain:
                li.append(ci)
        return li

    def allCertain(self):
        """Return True if all creatures are certain."""
        for ci in self.creatures:
            if not ci.certain:
                return False
        return True

    def setAllCertain(self):
        """Set all creatures to certain."""
        for ci in self.creatures:
            ci.certain = True

    def getAtSplitCreatures(self):
        """Return list of CreatureInfo where atSplit is true."""
        li = []
        for ci in self.creatures:
            if ci.atSplit:
                li.append(ci)
        return li

    def getAfterSplitCreatures(self):
        """Return list of CreatureInfo where atSplit is false."""
        li = []
        for ci in self.creatures:
            if not ci.atSplit:
                li.append(ci)
        return li

    def getCertainAtSplitCreatures(self):
        """Return list of CreatureInfo where both certain and atSplit are
            true."""
        li = []
        for ci in self.creatures:
            if ci.certain and ci.atSplit:
                li.append(ci)
        return li

    def getOtherChild(self, child):
        if child == self.child1:
            return self.child2
        if child == self.child2:
            return self.child1
        raise RuntimeError, "Not my child in Node.getOtherChild()"

    def getOtherChildMarkerId(self):
        if self.child1.markerId != self.markerId:
            return self.child1.markerId
        else:
            return self.child2.markerId

    def getCreatureNames(self):
        li = []
        for ci in self.creatures:
            li.append(ci.name)
        return li

    def getHeight(self):
        return len(self.creatures)


    def revealCreatures(self, cnl):
        """cnl is a list of creature names"""
        print "revealCreatures() for", self, cnl

        cil = []
        for name in cnl:
            cil.append(CreatureInfo(name, True, True))

        # Use a copy rather than the original so we can remove
        # creatures as we check for multiples.
        dupe = copy.deepcopy(cil)

        # Confirm that all creatures that were certain still fit
        # along with the revealed creatures.
        count = len(dupe)
        certain = self.getCertain()
        for ci in certain:
            if ci in dupe:
                dupe.remove(ci)
            else:
                count += 1

        if count > self.getHeight():
            err = ("Certainty error in Node.revealCreatures() count=%d "
                  "height=%d") % (count, self.getHeight())
            raise RuntimeError, err

        # Then mark passed creatures as certain and then
        # communicate this to the parent, to adjust other legions.

        if len(self.getCertain()) == self.getHeight():
            # No need -- we already know everything.
            return

        dupe = copy.deepcopy(cil)
        count = 0
        for ci in dupe:
            ci.certain = True
            ci.atSplit = True   # If not atSplit, would be certain.
            if numCreature(self.creatures, ci.name) < numCreature(dupe,
                    ci.name):
                self.creatures.append(ci)
                count += 1

        # Ensure that the creatures in cnl are now marked as certain
        dupe = copy.deepcopy(cil)
        certain = self.getCertain()
        for ci in certain:
            if ci in dupe:
                dupe.remove(ci)
        for ci in dupe:
            for ci2 in self.creatures:
                if not ci2.certain and ci2.name == ci.name:
                    ci2.certain = True
                    break

        print "revealCreatures() before remove", self
        # Need to remove count uncertain creatures.
        for unused in range(count):
            removeLastUncertainCreature(self.creatures)
        print "end revealCreatures()", self
        if self.parent is not None:
            self.parent.tellChildContents(self)


    def childCreaturesMatch(self):
        """Return True if creatures in children are consistent with self."""
        if self.child1 == None:
            return True
        all = (self.child1.getAtSplitCreatures() + self.child1.removed +
                self.child2.getAtSplitCreatures() + self.child2.removed)
        for ci in all:
            if numCreature(all, ci.name) != numCreature(
                    self.creatures, ci.name):
                return False
        return True


    def isLegalInitialSplitoff(self):
        if self.getHeight() != 4:
            return False
        names = self.getCreatureNames()
        return (names.count("Titan") + names.count("Angel") == 1)

    def findAllPossibleSplits(self, childSize, knownKeep, knownSplit):
        """ Return a list of all legal combinations of splitoffs. """
        print "findAllPossibleSplits() for", self, childSize, \
               knownKeep, knownSplit
        # Sanity checks
        if len(knownSplit) > childSize:
            raise RuntimeError, "More known splitoffs than splitoffs"
        creatures = self.creatures
        if len(creatures) > 8:
            raise RuntimeError, "More than 8 creatures in legion at split"
        elif len(creatures) == 8:
            if childSize != 4:
                raise RuntimeError, "Illegal initial split"
            if not "Titan" in self.getCreatureNames():
                raise RuntimeError, "No titan in 8-high legion"
            if not "Angel" in self.getCreatureNames():
                raise RuntimeError, "No angel in 8-high legion"
        knownCombo = knownSplit + knownKeep
        if not superset(creatures, knownCombo):
            print "Known creatures not in parent legion"
            self.revealCreatures(getCreatureNames(knownCombo))
            return self.findAllPossibleSplits(childSize, knownKeep, knownSplit)

        unknowns = copy.deepcopy(creatures)
        for ci in knownCombo:
            unknowns.remove(ci)

        numUnknownsToSplit = childSize - len(knownSplit)

        perms = Perms()
        unknownCombos = perms.findCombinations(unknowns, numUnknownsToSplit)

        possibleSplits = []
        for combo in unknownCombos:
            pos = copy.deepcopy(knownSplit) + copy.deepcopy(combo)
            if self.getHeight() != 8:
                possibleSplits.append(pos)
            else:
                posnode = Node(None, None, pos, self)
                if posnode.isLegalInitialSplitoff():
                    possibleSplits.append(pos)
        return possibleSplits


    def chooseCreaturesToSplitOut(self, pos):
        """Decide how to split this legion, and return a list of Creatures to
        remove.  Return null on error."""
        print "chooseCreaturesToSplitOut() for", self, pos
        maximize = (2 * len(pos[0]) > self.getHeight())

        bestKillValue = None
        creaturesToRemove = []
        for li in pos:
            totalKillValue = 0
            for creature in li:
                totalKillValue += killValue[creature.name]
            if ((bestKillValue is None) or 
                    (maximize and totalKillValue > bestKillValue) or
                    (not maximize and totalKillValue < bestKillValue)):
                bestKillValue = totalKillValue
                creaturesToRemove = li
        return creaturesToRemove


    def split(self, childSize, otherMarkerId, turn=-1):
        print "split() for", self, childSize, otherMarkerId, turn

        if len(self.creatures) > 8:
            raise RuntimeError, "More than 8 creatures in legion"

        if turn == -1:
            turn = self.turnSplit   # Re-predicting earlier split
        else:
            self.turnSplit = turn   # New split

        knownKeep = []
        knownSplit = []
        if self.child1 != None:
            knownKeep += (self.child1.getCertainAtSplitCreatures() +
                    self.child1.removed)
            knownSplit += (self.child2.getCertainAtSplitCreatures() +
                    self.child2.removed)

        pos = self.findAllPossibleSplits(childSize, knownKeep, knownSplit)
        splitoffCreatures = self.chooseCreaturesToSplitOut(pos)
        print "splitoffCreatures is", splitoffCreatures

        splitoffNames = getCreatureNames(splitoffCreatures)

        if self.allCertain():
            creatureNames = getCreatureNames(self.creatures)
            posSplitNames = []
            posKeepNames = []
            for li in pos:
                names = getCreatureNames(li)
                posSplitNames.append(names)
                posKeepNames.append(subtractLists(creatureNames, names))
            knownKeepNames = []
            knownSplitNames = []
            for name in creatureNames:
                if not name in knownKeepNames:
                    minKeep = minCount(posKeepNames, name)
                    for unused in range(minKeep):
                        knownKeepNames.append(name)
                if not name in knownSplitNames:
                    minSplit = minCount(posSplitNames, name)
                    for unused in range(minSplit):
                        knownSplitNames.append(name)
        else:
            knownKeepNames = getCreatureNames(knownKeep)
            knownSplitNames = getCreatureNames(knownSplit)

        # lists of CreatureInfo
        strongList = []
        weakList = []
        for ci in self.creatures:
            name = ci.name
            newinfo = CreatureInfo(name, False, True)
            if name in splitoffNames:
                weakList.append(newinfo)
                splitoffNames.remove(name)
                # If in knownSplit, set certain
                if name in knownSplitNames:
                    knownSplitNames.remove(name)
                    newinfo.certain = True
            else:
                strongList.append(newinfo)
                # If in knownKeep, set certain
                if name in knownKeepNames:
                    knownKeepNames.remove(name)
                    newinfo.certain = True

        afterSplit1 = []
        afterSplit2 = []
        removed1 = []
        removed2 = []
        if self.child1 != None:
            afterSplit1 = self.child1.getAfterSplitCreatures()
            afterSplit2 = self.child2.getAfterSplitCreatures()
            removed1 = self.child1.removed
            removed2 = self.child2.removed

        marker1 = self.markerId
        marker2 = otherMarkerId

        li1 = strongList + afterSplit1
        for ci in removed1:
            li1.remove(ci)
        li2 = weakList + afterSplit2
        for ci in removed2:
            li2.remove(ci)

        if self.child1 == None:
            self.child1 = Node(marker1, turn, li1, self)
            self.child2 = Node(marker2, turn, li2, self)
        else:
            self.child1.creatures = li1
            self.child2.creatures = li2

        if self.childSize1 == 0:
            self.childSize1 = self.child1.getHeight()
            self.childSize2 = self.child2.getHeight()
        print "child1:", self.child1
        print "child2:", self.child2


    def merge(self, other, turn):
        """Recombine this legion and other, because it was
        not possible to move either one. The two legions must share
        the same parent. If either legion has the parent's markerId,
        then that legion will remain. Otherwise this legion will
        remain."""
        print "merge() for", self, other
        parent = self.parent
        print "parent:", parent
        print "other.parent:", other.parent
        assert parent == other.parent
        if (parent.markerId == self.markerId or
                parent.markerId == other.markerId):
            # Remove self and other from parent, as if the split never
            # happened.  The parent will then be a leaf node.
            parent._clearChildren()
        else:
            # Remove self and other, then resplit parent into self.
            parent._clearChildren()
            parent.split(self.getHeight() + other.getHeight(),
                    self.markerId, turn)


    def tellChildContents(self, child):
        """ Tell this parent legion the known contents of one of its
        children. """

        print "tellChildContents() for node", self, "from node", child

        childCertainAtSplit = child.getCertainAtSplitCreatures()
        self.revealCreatures(getCreatureNames(childCertainAtSplit))

        # XXX Resplitting "too often" fixes some certainty issues.
        self.split(self.childSize2, self.getOtherChildMarkerId())


    def addCreature(self, creatureName):
        print "addCreature()", self, ':', creatureName
        if (self.getHeight() >= 7 and self.child1 == None):
            raise RuntimeError, "Tried adding to 7-high legion"
        ci = CreatureInfo(creatureName, True, False)
        self.creatures.append(ci)


    def removeCreature(self, creatureName):
        print "removeCreature()", self, ':', creatureName
        if (self.getHeight() <= 0):
            raise RuntimeError, "Tried removing from 0-high legion"
        self.revealCreatures([creatureName])
        ci = getCreatureInfo(self.creatures, creatureName)

        # Only need to track the removed creature for future parent split
        # predictions if it was here at the time of the split.
        if ci.atSplit:
            self.removed.append(ci)
        removeCreatureByName(self.creatures, creatureName)

    def removeCreatures(self, creatureNames):
        self.revealCreatures(creatureNames)
        for name in creatureNames:
            self.removeCreature(name)



class PredictSplits:
    def __init__(self, playerName, rootId, creatureNames):
        self.playerName = playerName
        # All creatures in root legion must be known

        infoList = []
        for name in creatureNames:
            info = CreatureInfo(name, True, True)
            infoList.append(info)
        self.root = Node(rootId, 0, infoList, None)

    def getNodes(self, node):
        """Return all non-empty nodes in tree."""
        nodes = []
        if len(node.creatures) > 0:
            nodes.append(node)
        if node.child1 != None:
            nodes += self.getNodes(node.child1) + self.getNodes(node.child2)
        return nodes

    def getLeaves(self, node):
        """Return all non-empty childless nodes in tree."""
        leaves = []
        if node.child1 == None:
            if len(node.creatures) > 0:
                leaves.append(node)
        else:
            leaves += self.getLeaves(node.child1) + self.getLeaves(node.child2)

        # If duplicate markerIds, prune the older node.
        for leaf1 in leaves:
            for leaf2 in leaves:
                if leaf1 != leaf2 and leaf1.markerId == leaf2.markerId:
                    if leaf1.turnCreated == leaf2.turnCreated:
                        raise (RuntimeError,
                                "Two leaf nodes with same markerId and turn")
                    elif leaf1.turnCreated < leaf2.turnCreated:
                        leaves.remove(leaf1)
                    else:
                        leaves.remove(leaf2)

        return leaves

    def printLeaves(self):
        """Print all childless nodes in tree, in string order. """
        print
        leaves = self.getLeaves(self.root)
        leaves.sort(lambda a,b: cmp(str(a), str(b)))
        for leaf in leaves:
            print leaf
        print

    def printNodes(self):
        """Print all nodes in tree, in string order. """
        print
        nodes = self.getNodes(self.root)
        nodes.sort(lambda a,b: cmp(str(a), str(b)))
        for node in nodes:
            print node
        print

    def getLeaf(self, markerId):
        """Return the leaf node with matching markerId"""
        leaves = self.getLeaves(self.root)
        for leaf in leaves:
            if leaf.markerId == markerId:
                return leaf
        return None


class AllPredictSplits:
    def __init__(self):
        self.pslist = []

    def add_ps(self, ps):
        self.pslist.append(ps)

    def getLeaf(self, markerId):
        for ps in self.pslist:
            leaf = ps.getLeaf(markerId)
            if leaf is not None:
                return leaf
        return None

    def printLeaves(self):
        for ps in self.pslist:
            ps.printLeaves()


class PredictSplitsTestCase(unittest.TestCase):

    def setUp(self):
        global aps
        aps = AllPredictSplits()


    def testPredictSplits1(self):
        print "\ntest 1 begins"

        markerId = "Rd01"
        playerName = "dripton"
        creatures = ["Titan", "Angel", "Ogre", "Ogre", "Centaur", "Centaur",
                     "Gargoyle", "Gargoyle"]

        ps = PredictSplits(playerName, markerId, creatures)
        ps.printLeaves()

        turn = 1
        print "Turn", turn
        ps.getLeaf("Rd01").split(4, "Rd02", turn)
        ps.getLeaf("Rd01").merge(ps.getLeaf("Rd02"), turn)
        ps.getLeaf("Rd01").split(4, "Rd02", turn)
        ps.getLeaf("Rd01").revealCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Rd01").addCreature("Troll")
        ps.getLeaf("Rd02").revealCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd02").addCreature("Lion")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        ps.printLeaves()

        turn = 2
        print "Turn", turn
        ps.getLeaf("Rd01").revealCreatures(["Gargoyle"])
        ps.getLeaf("Rd01").addCreature("Gargoyle")
        ps.getLeaf("Rd02").revealCreatures(["Lion"])
        ps.getLeaf("Rd02").addCreature("Lion")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        ps.printLeaves()

        turn = 3
        print "Turn", turn
        ps.getLeaf("Rd01").revealCreatures(["Titan"])
        ps.getLeaf("Rd01").addCreature("Warlock")
        ps.getLeaf("Rd02").addCreature("Gargoyle")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd02").allCertain())
        ps.printLeaves()

        turn = 4
        print "Turn", turn
        ps.getLeaf("Rd01").split(2, "Rd03", turn)
        ps.getLeaf("Rd02").split(2, "Rd04", turn)
        ps.getLeaf("Rd01").revealCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd01").addCreature("Cyclops")
        ps.getLeaf("Rd02").revealCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd02").addCreature("Cyclops")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(not ps.getLeaf("Rd03").allCertain())
        assert(not ps.getLeaf("Rd04").allCertain())
        ps.printLeaves()

        turn = 5
        print "Turn", turn
        ps.getLeaf("Rd01").revealCreatures(["Warlock"])
        ps.getLeaf("Rd01").addCreature("Warlock")
        ps.getLeaf("Rd02").addCreature("Ogre")
        ps.getLeaf("Rd03").revealCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Rd03").addCreature("Troll")
        ps.getLeaf("Rd04").revealCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd04").addCreature("Lion")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        ps.printLeaves()

        turn = 6
        print "Turn", turn
        ps.getLeaf("Rd02").split(2, "Rd05", turn)
        ps.getLeaf("Rd01").revealCreatures(["Titan", "Warlock", "Warlock",
            "Cyclops", "Troll", "Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd01").removeCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd02").removeCreature("Angel")
        ps.getLeaf("Rd01").addCreature("Angel")
        ps.getLeaf("Rd02").revealCreatures(["Lion", "Lion"])
        ps.getLeaf("Rd02").addCreature("Minotaur")
        ps.getLeaf("Rd04").revealCreatures(["Lion"])
        ps.getLeaf("Rd04").addCreature("Lion")
        ps.getLeaf("Rd02").revealCreatures(["Cyclops", "Minotaur", "Lion",
            "Lion", "Ogre"])
        ps.getLeaf("Rd02").addCreature("Minotaur")
        ps.getLeaf("Rd02").removeCreatures(["Cyclops", "Minotaur", "Minotaur",
            "Lion", "Lion", "Ogre"])
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        ps.printLeaves()

        turn = 7
        print "Turn", turn
        ps.getLeaf("Rd01").addCreature("Angel")
        ps.getLeaf("Rd03").revealCreatures(["Troll"])
        ps.getLeaf("Rd03").addCreature("Troll")
        ps.getLeaf("Rd04").revealCreatures(["Lion"])
        ps.getLeaf("Rd04").addCreature("Lion")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        ps.printLeaves()

        turn = 8
        print "Turn", turn
        ps.getLeaf("Rd01").split(2, "Rd02", turn)
        ps.getLeaf("Rd01").revealCreatures(["Cyclops"])
        ps.getLeaf("Rd01").addCreature("Cyclops")
        ps.getLeaf("Rd05").revealCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd05").addCreature("Cyclops")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        ps.printLeaves()

        turn = 9
        print "Turn", turn
        ps.getLeaf("Rd01").revealCreatures(["Troll"])
        ps.getLeaf("Rd01").addCreature("Troll")
        ps.getLeaf("Rd03").revealCreatures(["Troll"])
        ps.getLeaf("Rd03").addCreature("Troll")
        ps.getLeaf("Rd04").revealCreatures(["Lion", "Lion", "Lion"])
        ps.getLeaf("Rd04").addCreature("Griffon")
        ps.getLeaf("Rd05").revealCreatures(["Cyclops"])
        ps.getLeaf("Rd05").addCreature("Cyclops")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        ps.printLeaves()

        turn = 10
        print "Turn", turn
        ps.getLeaf("Rd01").split(2, "Rd06", turn)
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(not ps.getLeaf("Rd06").allCertain())
        ps.printLeaves()

        turn = 11
        print "Turn", turn
        ps.getLeaf("Rd04").revealCreatures(["Griffon", "Lion", "Lion", "Lion",
            "Centaur", "Centaur"])
        ps.getLeaf("Rd01").revealCreatures(["Cyclops"])
        ps.getLeaf("Rd01").addCreature("Cyclops")
        ps.getLeaf("Rd03").revealCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd03").addCreature("Ranger")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(not ps.getLeaf("Rd06").allCertain())
        ps.printLeaves()

        turn = 12
        print "Turn", turn
        ps.getLeaf("Rd02").addCreature("Centaur")
        ps.getLeaf("Rd03").revealCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd03").addCreature("Warbear")
        ps.getLeaf("Rd05").revealCreatures(["Cyclops"])
        ps.getLeaf("Rd05").addCreature("Cyclops")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(not ps.getLeaf("Rd06").allCertain())
        ps.printLeaves()

        turn = 13
        print "Turn", turn
        ps.getLeaf("Rd01").revealCreatures(["Titan", "Warlock", "Warlock",
            "Cyclops", "Cyclops", "Cyclops"])
        ps.getLeaf("Rd05").revealCreatures(["Cyclops", "Cyclops", "Cyclops"])
        ps.getLeaf("Rd05").addCreature("Behemoth")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        ps.printLeaves()

        turn = 14
        print "Turn", turn
        ps.getLeaf("Rd04").revealCreatures(["Griffon", "Lion", "Lion", "Lion",
                                              "Centaur", "Centaur"])
        ps.getLeaf("Rd02").removeCreature("Angel")
        ps.getLeaf("Rd04").addCreature("Angel")
        ps.getLeaf("Rd04").removeCreatures(["Angel", "Lion", "Lion", "Lion",
                                            "Centaur", "Centaur"])
        ps.getLeaf("Rd04").addCreature("Angel")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        ps.printLeaves()

        print "test 1 ends"

        """
        Start Rd01: Tit, Ang, Ogr, Ogr, Cen, Cen, Gar, Gar
        Turn 1  Split off Ang, Gar, Cen, Cen into Rd02
                Rd01 recruits Tro with 2xOgr
                Rd02 recruits Lio with 2xCen
        Turn 2  Rd01 recruits Gar with Gar
                Rd02 recruits Lio with Lio
        Turn 3  Rd01 recruits Wlk with Tit
                Rd02 recruits Gar (in tower)
        Turn 4  Rd01 splits off Ogr, Ogr into Rd03
                Rd02 splits off Cen, Cen into Rd04
                Rd01 recruits Cyc with 2xGar
                Rd02 recruits Cyc with 2xGar
        Turn 5  Rd01 recruits Wlk with Wlk
                Rd02 recruits Ogr (in tower)
                Rd03 recruits Tro with 2xOgr
                Rd04 recruits Lio with 2xCen
        Turn 6  Rd02 splits off Gar, Gar into Rd05
                Rd01 attacks and is revealed as Tit, Wlk, Wlk, Cyc, Tro, Gar, Gar
                Angel summoned from Rd02 into Rd01
                2xGar killed, Rd01 revealed as Tit, Ang, Wlk, Wlk, Cyc, Tro
                Rd02 recruits Min with 2xLio
                Rd04 recruits Lio with Lio
                Rd02 attacked, revealed as Cyc, Min, Lio, Lio, Ogr
                Min recruits Min reinforcement
                Rd02 destroyed
        Turn 7  Rd01 acquires Angel after enemy flees
                Rd03 recruits Tro with Tro
                Rd04 recruits Lio with Lio
        Turn 8  Rd01 splits off Ang, Ang into Rd02
                Rd01 recruits Cyc with Cyc
                Rd05 recruits Cyc with 2xGar
        Turn 9  Rd01 recruits Tro with Tro
                Rd03 recruits Tro with Tro
                Rd04 recruits Gri with 3xLio
                Rd05 recruits Cyc with Cyc
        Turn 10 Rd01 splits off Tro, Tro into Rd06
        Turn 11 Rd04 attacks, revealed as Gri, 3xLio, 2xCen
                Rd01 recruits Cyc with Cyc
                Rd03 recruits Ran with 2xTro
        Turn 12 Rd02 recruits Cen (in tower)
                Rd03 recruits Wbe with 2xTro
                Rd05 recruits Cyc with Cyc
        Turn 13 Rd01 attacks, revealed as Tit, Wlk, Wlk, Cyc, Cyc, Cyc
                Rd05 recruits Beh with 3xCyc
        Turn 14 Rd04 attacks, revealed as Gri, Lio, Lio, Lio, Cen, Cen
                Angel summoned from Rd02 into Rd04
                Rd04 loses Ang, Lio, Lio, Lio, Cen, Cen
                Rd04 acquires Angel
                win
        """

    def testPredictSplits2(self):
        print "\ntest 2 begins"

        markerId = "Rd11"
        playerName = "dripton"
        creatures = ["Titan", "Angel", "Ogre", "Ogre", "Centaur", "Centaur",
                     "Gargoyle", "Gargoyle"]

        ps = PredictSplits(playerName, markerId, creatures)
        ps.printLeaves()

        turn = 1
        print "Turn", turn
        ps.getLeaf("Rd11").split(4, "Rd10", turn)
        ps.getLeaf("Rd10").revealCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Rd10").addCreature("Troll")
        ps.getLeaf("Rd11").revealCreatures(["Gargoyle"])
        ps.getLeaf("Rd11").addCreature("Gargoyle")
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 2
        print "Turn", turn
        ps.getLeaf("Rd10").revealCreatures(["Troll"])
        ps.getLeaf("Rd10").addCreature("Troll")
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 3
        print "Turn", turn
        ps.getLeaf("Rd10").revealCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd10").addCreature("Ranger")
        ps.getLeaf("Rd11").revealCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd11").addCreature("Cyclops")
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 4
        print "Turn", turn
        ps.getLeaf("Rd10").revealCreatures(["Titan", "Ranger", "Troll", "Troll",
            "Gargoyle", "Ogre", "Ogre"])
        ps.printLeaves()
        ps.getLeaf("Rd10").removeCreature("Gargoyle")
        ps.getLeaf("Rd11").removeCreature("Angel")
        ps.getLeaf("Rd10").addCreature("Angel")
        assert(ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 5
        print "Turn", turn
        ps.getLeaf("Rd10").split(2, "Rd01", turn)
        ps.getLeaf("Rd10").revealCreatures(["Troll"])
        ps.getLeaf("Rd10").addCreature("Troll")
        ps.getLeaf("Rd01").revealCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Rd01").addCreature("Troll")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 6
        print "Turn", turn
        ps.getLeaf("Rd01").revealCreatures(["Troll", "Ogre", "Ogre"])
        ps.getLeaf("Rd01").revealCreatures(["Troll"])
        ps.getLeaf("Rd01").addCreature("Troll")
        ps.getLeaf("Rd10").revealCreatures(["Troll", "Troll", "Troll"])
        ps.getLeaf("Rd10").addCreature("Wyvern")
        ps.getLeaf("Rd11").revealCreatures(["Cyclops"])
        ps.getLeaf("Rd11").addCreature("Cyclops")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 7
        print "Turn", turn
        ps.getLeaf("Rd10").split(2, "Rd06", turn)
        ps.getLeaf("Rd11").revealCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd11").addCreature("Lion")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd06").allCertain())
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 8
        print "Turn", turn
        ps.getLeaf("Rd11").split(2, "Rd07", turn)
        ps.getLeaf("Rd01").revealCreatures(["Troll", "Troll", "Ogre", "Ogre"])
        ps.getLeaf("Rd10").removeCreature("Angel")
        ps.getLeaf("Rd01").addCreature("Angel")
        ps.getLeaf("Rd01").removeCreatures(["Troll", "Troll", "Ogre", "Ogre"])
        ps.getLeaf("Rd01").addCreature("Angel")
        ps.getLeaf("Rd10").revealCreatures(["Wyvern"])
        ps.getLeaf("Rd10").addCreature("Wyvern")
        ps.getLeaf("Rd11").revealCreatures(["Lion"])
        ps.getLeaf("Rd11").addCreature("Lion")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd06").allCertain())
        assert(not ps.getLeaf("Rd07").allCertain())
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 9
        print "Turn", turn
        ps.getLeaf("Rd07").addCreature("Centaur")
        ps.getLeaf("Rd11").revealCreatures(["Cyclops"])
        ps.getLeaf("Rd11").addCreature("Cyclops")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd06").allCertain())
        assert(not ps.getLeaf("Rd07").allCertain())
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 10
        print "Turn", turn
        ps.getLeaf("Rd11").split(2, "Rd08", turn)
        ps.getLeaf("Rd01").revealCreatures(["Angel", "Angel"])
        ps.getLeaf("Rd06").revealCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd06").addCreature("Warbear")
        ps.getLeaf("Rd07").revealCreatures(["Centaur"])
        ps.getLeaf("Rd07").addCreature("Centaur")
        ps.getLeaf("Rd08").revealCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd08").addCreature("Lion")
        ps.getLeaf("Rd10").revealCreatures(["Ranger"])
        ps.getLeaf("Rd10").addCreature("Ranger")
        ps.getLeaf("Rd11").revealCreatures(["Cyclops", "Cyclops", "Cyclops"])
        ps.getLeaf("Rd11").addCreature("Behemoth")
        ps.getLeaf("Rd01").revealCreatures(["Angel", "Angel"])
        ps.getLeaf("Rd01").removeCreatures(["Angel", "Angel"])
        assert(ps.getLeaf("Rd06").allCertain())
        assert(ps.getLeaf("Rd07").allCertain())
        assert(ps.getLeaf("Rd08").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 11
        print "Turn", turn
        ps.getLeaf("Rd06").revealCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd06").addCreature("Ranger")
        ps.getLeaf("Rd07").revealCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd07").addCreature("Lion")
        ps.getLeaf("Rd08").revealCreatures(["Lion"])
        ps.getLeaf("Rd08").addCreature("Lion")
        ps.getLeaf("Rd10").revealCreatures(["Titan"])
        ps.getLeaf("Rd10").addCreature("Warlock")
        assert(ps.getLeaf("Rd06").allCertain())
        assert(ps.getLeaf("Rd07").allCertain())
        assert(ps.getLeaf("Rd08").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 12
        print "Turn", turn
        ps.getLeaf("Rd10").split(2, "Rd05", turn)
        ps.getLeaf("Rd05").revealCreatures(["Troll"])
        ps.getLeaf("Rd05").addCreature("Troll")
        ps.getLeaf("Rd06").revealCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd06").addCreature("Warbear")
        ps.getLeaf("Rd07").revealCreatures(["Lion"])
        ps.getLeaf("Rd07").addCreature("Lion")
        ps.getLeaf("Rd11").revealCreatures(["Lion", "Lion"])
        ps.getLeaf("Rd11").addCreature("Ranger")
        assert(not ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        assert(ps.getLeaf("Rd07").allCertain())
        assert(ps.getLeaf("Rd08").allCertain())
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 13
        print "Turn", turn
        ps.getLeaf("Rd11").split(2, "Rd04", turn)
        ps.getLeaf("Rd05").revealCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd05").addCreature("Warbear")
        ps.getLeaf("Rd07").revealCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd07").addCreature("Cyclops")
        ps.getLeaf("Rd11").revealCreatures(["Ranger"])
        ps.getLeaf("Rd11").addCreature("Ranger")
        ps.getLeaf("Rd08").revealCreatures(["Lion", "Lion", "Centaur",
                "Centaur"])
        ps.getLeaf("Rd08").removeCreatures(["Lion", "Lion", "Centaur", "Centaur"])
        assert(not ps.getLeaf("Rd04").allCertain())
        assert(not ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        assert(ps.getLeaf("Rd07").allCertain())
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 14
        print "Turn", turn
        ps.getLeaf("Rd06").revealCreatures(["Warbear", "Warbear", "Ranger",
                "Troll", "Troll"])
        ps.getLeaf("Rd04").revealCreatures(["Cyclops"])
        ps.getLeaf("Rd04").addCreature("Cyclops")
        ps.getLeaf("Rd06").revealCreatures(["Ranger"])
        ps.getLeaf("Rd06").addCreature("Ranger")
        ps.getLeaf("Rd10").revealCreatures(["Wyvern", "Wyvern"])
        ps.getLeaf("Rd10").addCreature("Hydra")
        ps.getLeaf("Rd11").revealCreatures(["Ranger"])
        ps.getLeaf("Rd11").addCreature("Ranger")
        assert(not ps.getLeaf("Rd04").allCertain())
        assert(not ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        assert(ps.getLeaf("Rd07").allCertain())
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 15
        print "Turn", turn
        ps.getLeaf("Rd07").split(2, "Rd02", turn)
        ps.getLeaf("Rd11").split(2, "Rd01", turn)
        ps.getLeaf("Rd05").revealCreatures(["Troll"])
        ps.getLeaf("Rd05").addCreature("Troll")
        ps.getLeaf("Rd06").revealCreatures(["Ranger"])
        ps.getLeaf("Rd06").addCreature("Ranger")
        ps.getLeaf("Rd11").revealCreatures(["Cyclops", "Cyclops"])
        ps.getLeaf("Rd11").addCreature("Gorgon")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(not ps.getLeaf("Rd04").allCertain())
        assert(not ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        assert(not ps.getLeaf("Rd07").allCertain())
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 16
        print "Turn", turn
        ps.getLeaf("Rd06").revealCreatures(["Warbear", "Warbear", "Ranger",
                "Ranger", "Ranger", "Troll", "Troll"])
        ps.getLeaf("Rd01").revealCreatures(["Ranger"])
        ps.getLeaf("Rd01").addCreature("Ranger")
        ps.getLeaf("Rd04").revealCreatures(["Cyclops"])
        ps.getLeaf("Rd04").addCreature("Cyclops")
        ps.getLeaf("Rd05").revealCreatures(["Ranger"])
        ps.getLeaf("Rd05").addCreature("Ranger")
        ps.getLeaf("Rd07").revealCreatures(["Lion", "Lion"])
        ps.getLeaf("Rd07").addCreature("Ranger")
        ps.getLeaf("Rd10").revealCreatures(["Ranger"])
        ps.getLeaf("Rd10").addCreature("Ranger")
        ps.getLeaf("Rd11").revealCreatures(["Ranger"])
        ps.getLeaf("Rd11").addCreature("Ranger")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(not ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        assert(not ps.getLeaf("Rd07").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 17
        print "Turn", turn
        ps.getLeaf("Rd06").split(2, "Rd08", turn)
        ps.getLeaf("Rd11").split(2, "Rd03", turn)
        ps.getLeaf("Rd08").revealCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd08").removeCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd05").revealCreatures(["Warbear"])
        ps.getLeaf("Rd05").addCreature("Warbear")
        ps.getLeaf("Rd11").revealCreatures(["Behemoth"])
        ps.getLeaf("Rd11").addCreature("Behemoth")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(not ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        assert(not ps.getLeaf("Rd07").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 18
        print "Turn", turn
        ps.getLeaf("Rd10").split(2, "Rd12", turn)
        ps.getLeaf("Rd01").revealCreatures(["Ranger"])
        ps.getLeaf("Rd01").addCreature("Ranger")
        ps.getLeaf("Rd11").revealCreatures(["Gorgon"])
        ps.getLeaf("Rd11").addCreature("Gorgon")
        ps.getLeaf("Rd12").revealCreatures(["Ranger", "Ranger"])
        ps.getLeaf("Rd12").removeCreatures(["Ranger", "Ranger"])
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(not ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        assert(not ps.getLeaf("Rd07").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 19
        print "Turn", turn
        ps.getLeaf("Rd11").split(2, "Rd08", turn)
        ps.getLeaf("Rd07").revealCreatures(["Cyclops", "Ranger", "Lion",
                "Lion", "Centaur", "Centaur"])
        ps.getLeaf("Rd07").removeCreatures(["Lion", "Centaur", "Centaur"])
        ps.getLeaf("Rd01").revealCreatures(["Ranger"])
        ps.getLeaf("Rd01").addCreature("Ranger")
        ps.getLeaf("Rd03").revealCreatures(["Cyclops"])
        ps.getLeaf("Rd03").addCreature("Cyclops")
        ps.getLeaf("Rd04").revealCreatures(["Cyclops", "Cyclops"])
        ps.getLeaf("Rd04").addCreature("Gorgon")
        ps.getLeaf("Rd07").revealCreatures(["Ranger"])
        ps.getLeaf("Rd07").addCreature("Ranger")
        ps.getLeaf("Rd08").revealCreatures(["Ranger"])
        ps.getLeaf("Rd08").addCreature("Ranger")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd02").allCertain())
        assert(not ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        assert(ps.getLeaf("Rd07").allCertain())
        assert(not ps.getLeaf("Rd08").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 20
        print "Turn", turn
        ps.getLeaf("Rd04").revealCreatures(["Gorgon", "Cyclops", "Cyclops",
                "Cyclops", "Lion"])
        ps.getLeaf("Rd10").revealCreatures(["Titan", "Hydra", "Wyvern",
                "Wyvern", "Warlock"])
        ps.getLeaf("Rd10").addCreature("Angel")
        ps.getLeaf("Rd05").revealCreatures(["Warbear", "Warbear", "Ranger",
                "Ranger", "Troll", "Troll", "Troll"])
        ps.getLeaf("Rd10").removeCreature("Angel")
        ps.getLeaf("Rd05").removeCreature("Troll")
        ps.getLeaf("Rd05").addCreature("Angel")
        ps.getLeaf("Rd05").removeCreatures(["Angel", "Warbear", "Warbear",
                "Ranger", "Ranger", "Troll"])
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd02").allCertain())
        assert(not ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        assert(ps.getLeaf("Rd06").allCertain())
        assert(ps.getLeaf("Rd07").allCertain())
        assert(not ps.getLeaf("Rd08").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        print "test 2 ends"

        """
        Start Rd11: Tit, Ang, Ogr, Ogr, Cen, Cen, Gar, Gar
        Turn 1  Split off Tit, Ogr, Ogr, Gar into Rd10
                Rd10 recruits Tro with 2xOgr
                Rd11 recruits Gar with Gar
        Turn 2  Rd10 recruits Tro with Tro
        Turn 3  Rd10 recruits Ran with 2xTro
                Rd11 recruits Cyc with 2xGar
        Turn 4  Rd10 attacks, revealed as Tit, Ran, Tro, Tro, Gar, Ogr, Ogr
                Rd10 loses Gar
                Angel summoned from Rd11 to Rd10
        Turn 5  Rd10 splits off Ogr, Ogr into Rd01
                Rd10 recruits Tro with Tro
                Rd01 recruits Tro with 2xOgr
        Turn 6  Rd01 attacks, revealed (to Green) as Tro, Ogr, Ogr
                Rd01 recruits Tro with Tro
                Rd10 recruits Wyv with 3xTro
                Rd11 recruits Cyc with Cyc
        Turn 7  Rd10 splits off Tro, Tro into Rd06
                Rd11 recruits Lio with 2xCen
        Turn 8  Rd11 splits off Gar, Gar into Rd07
                Rd01 attacks, revealed as Tro, Tro, Ogr, Ogr
                Angel summoned from Rd10 into Rd01
                Rd01 loses Tro, Tro, Ogr, Ogr
                Rd01 acquires Angel
                Rd10 recruits Wyv with Wyv
                Rd11 recruits Lio with Lio
        Turn 9  Rd07 recruits Cen
                Rd11 recruits Cyc with Cyc
        Turn 10 Rd11 splits off Cen, Cen into Rd08
                Rd01 attacks, revealed (to Green) as Ang, Ang
                Rd06 recruits Wbe with 2xTro
                Rd07 recruits Cen with Cen
                Rd08 recruits Lio with 2xCen
                Rd10 recruits Ran with Ran
                Rd11 recruits Beh with 3xCyc
                Rd01 attacked, revealed as Ang, Ang
                Rd01 eliminated, losing Ang, Ang
        Turn 11 Rd06 recruits Ran with 2xTro
                Rd07 recruits Lio with 2xCen
                Rd08 recruits Lio with Lio
                Rd10 recruits Wlk with Tit
        Turn 12 Rd10 splits off Ran, Tro into Rd05
                Rd05 recruits Tro with Tro
                Rd06 recruits Wbe with 2xTro
                Rd07 recruits Lio with Lio
                Rd11 recruits Ran with 2xLio
        Turn 13 Rd11 splits off Cyc, Lio into Rd04
                Rd05 recruits Wbe with 2xTro
                Rd07 recruits Cyc with 2xGar
                Rd11 recruits Ran with Ran
                Rd08 attacked, revealed as Lio, Lio, Cen, Cen
                Rd08 eliminated, losing Lio, Lio, Cen, Cen
        Turn 14 Rd06 attacks, revealed (to Green) as Wbe, Wbe, Ran, Tro, Tro
                Rd04 recruits Cyc with Cyc
                Rd06 recruits Ran with Ran
                Rd10 recruits Hyd with 2xWyv
                Rd11 recruits Ran with Ran
        Turn 15 Rd07 splits off Gar, Gar into Rd02
                Rd11 splits off Ran, Lio into Rd01
                Rd05 recruits Tro with Tro
                Rd06 recruits Ran with Ran
                Rd11 recruits Gor with 2xCyc
        Turn 16 Rd06 attacks, revealed as Wbe, Wbe, Ran, Ran, Ran, Tro, Tro
                Rd01 recruits Ran with Ran
                Rd04 recruits Cyc with Cyc
                Rd05 recruits Ran with Ran
                Rd07 recruits Ran with 2xLio
                Rd10 recruits Ran with Ran
                Rd11 recruits Ran with Ran
        Turn 17 Rd06 splits off Tro, Tro into Rd08
                Rd11 splits off Cyc, Cyc into Rd03
                Rd08 attacks, revealed as Tro, Tro
                Rd08 eliminated, losing Tro, Tro
                Rd05 recruits Wbe with Wbe
                Rd11 recruits Beh with Beh
        Turn 18 Rd10 splits off Ran, Ran into Rd12
                Rd01 recruits Ran with Ran
                Rd11 recruits Gor with Gor
                Rd12 attacked, revealed as Ran, Ran
                Rd12 eliminated, losing Ran, Ran
        Turn 19 Rd11 splits off Ran, Ran into Rd08
                Rd07 attacks, revealed as Cyc, Ran, Lio, Lio, Cen, Cen
                Rd07 loses Lio, Cen, Cen
                Rd01 recruits Ran with Ran
                Rd03 recruits Cyc with Cyc
                Rd04 recruits Gor with 2xCyc
                Rd07 recruits Ran with Ran
                Rd08 recruits Ran with Ran
        Turn 20 Rd04 attacks, revealed as Gor, Cyc, Cyc, Cyc, Lio
                Rd10 attacks, revealed as Tit, Hyd, Wyv, Wyv, Wlk
                Rd10 acquires Ang
                Rd05 attacks, revealed as Wbe, Wbe, Ran, Ran, Tro, Tro, Tro
                Ang summoned from Rd10 into Rd05
                Rd05 loses Ang, Wbe, Wbe, Ran, Ran, Tro, Tro
                win
        """

    def testPredictSplits3(self):
        print "\ntest 3 begins"

        markerId = "Gr07"
        playerName = "Green"
        creatures = ["Titan", "Angel", "Ogre", "Ogre", "Centaur", "Centaur",
                     "Gargoyle", "Gargoyle"]

        ps = PredictSplits(playerName, markerId, creatures)
        ps.printLeaves()

        turn = 1
        print "Turn", turn
        ps.getLeaf("Gr07").split(4, "Gr11", turn)
        ps.getLeaf("Gr07").revealCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Gr07").addCreature("Cyclops")
        ps.getLeaf("Gr11").revealCreatures(["Centaur"])
        ps.getLeaf("Gr11").addCreature("Centaur")
        assert(not ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        ps.printLeaves()

        turn = 2
        print "Turn", turn
        ps.getLeaf("Gr07").revealCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Gr07").addCreature("Cyclops")
        ps.getLeaf("Gr11").revealCreatures(["Ogre"])
        ps.getLeaf("Gr11").addCreature("Ogre")
        assert(not ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        ps.printLeaves()

        turn = 3
        print "Turn", turn
        ps.getLeaf("Gr11").revealCreatures(["Centaur", "Centaur", "Centaur"])
        ps.getLeaf("Gr11").addCreature("Warbear")
        assert(not ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        ps.printLeaves()

        print "test 3 ends"

        """
        Start Gr07: Tit, Ang, Ogr, Ogr, Cen, Cen, Gar, Gar
        Turn 1  Split off Ang, Cen, Cen, Ogr into Gr11
                Gr07 recruits Cyc with 2xGar
                Gr11 recruits Cen with Cen
        Turn 2  Gr07 recruits Cyc with 2xGar
                Gr11 recruits Ogr with Ogr
        Turn 3  Gr11 recruits Wbe with 2xCen
        """


    def testPredictSplits4(self):
        print "\ntest 4 begins"
        ps = PredictSplits("Rd", "Rd12", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Rd12").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Bk", "Bk10", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bk10").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Br", "Br05", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Br05").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Gd", "Gd08", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gd08").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        turn = 1
        print "\nTurn", turn
        aps.getLeaf("Rd12").split(4, "Rd01", 1)
        aps.getLeaf("Rd12").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Rd12").addCreature("Lion")
        aps.getLeaf("Rd01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Rd01").addCreature("Troll")
        aps.getLeaf("Bk10").split(4, "Bk03", 1)
        aps.getLeaf("Bk03").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bk03").addCreature("Lion")
        aps.getLeaf("Bk10").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk10").addCreature("Cyclops")
        aps.getLeaf("Br05").split(4, "Br01", 1)
        aps.getLeaf("Br01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br01").addCreature("Troll")
        aps.getLeaf("Br05").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br05").addCreature("Lion")
        aps.getLeaf("Gd08").split(4, "Gd01", 1)
        aps.getLeaf("Gd01").revealCreatures(['Gargoyle'])
        aps.getLeaf("Gd01").addCreature("Gargoyle")
        aps.getLeaf("Gd08").revealCreatures(['Ogre'])
        aps.getLeaf("Gd08").addCreature("Ogre")
        aps.getLeaf("Rd12").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Rd12").addCreature("Lion")
        aps.getLeaf("Rd01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Rd01").addCreature("Troll")
        aps.getLeaf("Bk10").revealCreatures(['Ogre'])
        aps.getLeaf("Bk10").addCreature("Ogre")
        aps.getLeaf("Br01").revealCreatures(['Titan'])
        aps.getLeaf("Br01").addCreature("Warlock")
        aps.getLeaf("Br05").revealCreatures(['Lion'])
        aps.getLeaf("Br05").addCreature("Lion")
        aps.getLeaf("Gd01").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd01").addCreature("Cyclops")
        aps.getLeaf("Gd08").revealCreatures(['Gargoyle'])
        aps.getLeaf("Gd08").addCreature("Gargoyle")
        aps.getLeaf("Rd12").revealCreatures(['Gargoyle'])
        aps.getLeaf("Rd12").addCreature("Gargoyle")
        aps.getLeaf("Bk03").revealCreatures(['Ogre'])
        aps.getLeaf("Bk03").addCreature("Ogre")
        aps.getLeaf("Bk10").revealCreatures(['Titan'])
        aps.getLeaf("Bk10").addCreature("Warlock")
        aps.getLeaf("Br01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br01").addCreature("Troll")
        aps.getLeaf("Br05").revealCreatures(['Centaur'])
        aps.getLeaf("Br05").addCreature("Centaur")
        aps.getLeaf("Gd01").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd01").addCreature("Cyclops")
        aps.getLeaf("Gd08").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gd08").addCreature("Troll")
        turn = 4
        print "\nTurn", turn
        aps.getLeaf("Rd12").split(2, "Rd11", 4)
        aps.getLeaf("Rd12").revealCreatures(['Titan'])
        aps.getLeaf("Rd12").addCreature("Warlock")
        aps.getLeaf("Rd11").revealCreatures(['Centaur'])
        aps.getLeaf("Rd11").addCreature("Centaur")
        aps.getLeaf("Rd01").revealCreatures(['Gargoyle'])
        aps.getLeaf("Rd01").addCreature("Gargoyle")
        aps.getLeaf("Bk10").split(2, "Bk04", 4)
        aps.getLeaf("Bk03").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk03").addCreature("Troll")
        aps.getLeaf("Bk10").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk10").addCreature("Cyclops")
        aps.getLeaf("Br01").split(2, "Br02", 4)
        aps.getLeaf("Br01").revealCreatures(['Titan'])
        aps.getLeaf("Br01").addCreature("Warlock")
        aps.getLeaf("Gd01").split(2, "Gd04", 4)
        aps.getLeaf("Gd08").split(2, "Gd02", 4)
        turn = 5
        print "\nTurn", turn
        aps.getLeaf("Rd01").split(2, "Rd07", 5)
        aps.getLeaf("Rd01").revealCreatures(['Troll'])
        aps.getLeaf("Rd01").addCreature("Troll")
        aps.getLeaf("Rd12").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd12").addCreature("Cyclops")
        aps.getLeaf("Bk03").split(2, "Bk12", 5)
        aps.getLeaf("Bk03").revealCreatures(['Troll'])
        aps.getLeaf("Bk03").addCreature("Troll")
        aps.getLeaf("Bk10").revealCreatures(['Ogre'])
        aps.getLeaf("Bk10").addCreature("Ogre")
        aps.getLeaf("Br05").split(2, "Br10", 5)
        aps.getLeaf("Br01").revealCreatures(['Titan'])
        aps.getLeaf("Br01").revealCreatures(['Titan'])
        aps.getLeaf("Br01").addCreature("Warlock")
        aps.getLeaf("Br05").revealCreatures(['Centaur'])
        aps.getLeaf("Br05").addCreature("Centaur")
        aps.getLeaf("Gd01").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gd01").addCreature("Lion")
        aps.getLeaf("Gd02").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd02").addCreature("Cyclops")
        turn = 6
        print "\nTurn", turn
        aps.getLeaf("Rd12").split(2, "Rd02", 6)
        aps.getLeaf("Rd11").revealCreatures(['Centaur', 'Centaur', 'Centaur'])
        aps.getLeaf("Gd04").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd04").removeCreature("Gargoyle")
        aps.getLeaf("Rd11").removeCreature("Centaur")
        aps.getLeaf("Gd04").removeCreature("Gargoyle")
        aps.getLeaf("Rd11").removeCreature("Centaur")
        aps.getLeaf("Rd11").revealCreatures(['Centaur'])
        aps.getLeaf("Rd11").revealCreatures(['Centaur'])
        aps.getLeaf("Rd11").addCreature("Centaur")
        aps.getLeaf("Rd02").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd02").addCreature("Cyclops")
        aps.getLeaf("Rd12").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Rd12").addCreature("Ranger")
        aps.getLeaf("Rd01").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd01").addCreature("Cyclops")
        aps.getLeaf("Bk10").split(2, "Bk09", 6)
        aps.getLeaf("Bk03").revealCreatures(['Lion'])
        aps.getLeaf("Bk03").addCreature("Lion")
        aps.getLeaf("Bk10").revealCreatures(['Ogre'])
        aps.getLeaf("Bk10").addCreature("Ogre")
        aps.getLeaf("Br01").split(2, "Br08", 6)
        aps.getLeaf("Br01").revealCreatures(['Troll'])
        aps.getLeaf("Br01").addCreature("Troll")
        aps.getLeaf("Br02").revealCreatures(['Ogre'])
        aps.getLeaf("Br02").addCreature("Ogre")
        aps.getLeaf("Br05").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br05").addCreature("Lion")
        aps.printLeaves()


    def testPredictSplits5(self):
        print "\ntest 5 begins"
        ps = PredictSplits("Rd", "Rd12", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Rd12").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Gd", "Gd12", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gd12").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Br", "Br02", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Br02").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Gr", "Gr07", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gr07").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Bk", "Bk07", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bk07").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Bu", "Bu05", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bu05").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.printLeaves()
        turn = 1
        print "\nTurn", turn
        aps.getLeaf("Rd12").split(4, "Rd09", 1)
        aps.getLeaf("Rd12").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Rd12").addCreature("Troll")
        aps.getLeaf("Rd09").revealCreatures(['Gargoyle'])
        aps.getLeaf("Rd09").addCreature("Gargoyle")
        aps.getLeaf("Gd12").split(4, "Gd08", 1)
        aps.getLeaf("Gd12").revealCreatures(['Titan'])
        aps.getLeaf("Gd08").revealCreatures(['Centaur'])
        aps.getLeaf("Gd08").addCreature("Centaur")
        aps.getLeaf("Gd12").revealCreatures(['Titan'])
        aps.getLeaf("Gd12").addCreature("Warlock")
        aps.getLeaf("Br02").split(4, "Br07", 1)
        aps.getLeaf("Br02").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br02").addCreature("Cyclops")
        aps.getLeaf("Br07").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br07").addCreature("Troll")
        aps.getLeaf("Gr07").split(4, "Gr02", 1)
        aps.getLeaf("Gr07").revealCreatures(['Titan'])
        aps.getLeaf("Gr02").revealCreatures(['Centaur'])
        aps.getLeaf("Gr02").addCreature("Centaur")
        aps.getLeaf("Gr07").revealCreatures(['Titan'])
        aps.getLeaf("Gr07").addCreature("Warlock")
        aps.getLeaf("Bk07").split(4, "Bk03", 1)
        aps.getLeaf("Bk03").revealCreatures(['Centaur'])
        aps.getLeaf("Bk03").addCreature("Centaur")
        aps.getLeaf("Bu05").split(4, "Bu01", 1)
        aps.getLeaf("Bu05").revealCreatures(['Titan'])
        aps.getLeaf("Bu01").revealCreatures(['Centaur'])
        aps.getLeaf("Bu01").addCreature("Centaur")
        aps.getLeaf("Bu05").revealCreatures(['Titan'])
        aps.getLeaf("Bu05").addCreature("Warlock")
        aps.getLeaf("Rd09").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd09").addCreature("Cyclops")
        aps.getLeaf("Rd12").revealCreatures(['Troll'])
        aps.getLeaf("Rd12").addCreature("Troll")
        aps.getLeaf("Gd08").revealCreatures(['Centaur', 'Centaur', 'Centaur'])
        aps.getLeaf("Gd08").addCreature("Warbear")
        aps.getLeaf("Gd12").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gd12").addCreature("Troll")
        aps.getLeaf("Br02").revealCreatures(['Centaur'])
        aps.getLeaf("Br02").addCreature("Centaur")
        aps.getLeaf("Gr02").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gr02").addCreature("Lion")
        aps.getLeaf("Gr07").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr07").addCreature("Cyclops")
        aps.getLeaf("Bk03").revealCreatures(['Gargoyle'])
        aps.getLeaf("Bk03").addCreature("Gargoyle")
        aps.getLeaf("Bk07").revealCreatures(['Gargoyle'])
        aps.getLeaf("Bk07").addCreature("Gargoyle")
        aps.getLeaf("Bu05").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu05").addCreature("Cyclops")
        aps.getLeaf("Rd09").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Rd09").addCreature("Lion")
        aps.getLeaf("Rd12").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Rd12").addCreature("Ranger")
        aps.getLeaf("Br02").revealCreatures(['Centaur'])
        aps.getLeaf("Br02").addCreature("Centaur")
        aps.getLeaf("Br07").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br07").addCreature("Troll")
        aps.getLeaf("Gr02").revealCreatures(['Ogre'])
        aps.getLeaf("Gr02").addCreature("Ogre")
        aps.getLeaf("Gr07").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr07").addCreature("Cyclops")
        aps.getLeaf("Bk03").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk03").addCreature("Cyclops")
        aps.getLeaf("Bk07").revealCreatures(['Titan'])
        aps.getLeaf("Bk07").addCreature("Warlock")
        aps.getLeaf("Bu05").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu05").addCreature("Cyclops")
        aps.printLeaves()
        turn = 4
        print "\nTurn", turn
        aps.getLeaf("Rd09").split(2, "Rd05", 4)
        aps.getLeaf("Rd12").split(2, "Rd11", 4)
        aps.getLeaf("Rd12").revealCreatures(['Gargoyle'])
        aps.getLeaf("Rd12").addCreature("Gargoyle")
        aps.getLeaf("Gd08").revealCreatures(['Gargoyle'])
        aps.getLeaf("Gd08").addCreature("Gargoyle")
        aps.getLeaf("Gd12").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gd12").addCreature("Troll")
        aps.getLeaf("Br02").split(2, "Br09", 4)
        aps.getLeaf("Br02").revealCreatures(['Cyclops'])
        aps.getLeaf("Br02").addCreature("Cyclops")
        aps.getLeaf("Br07").revealCreatures(['Centaur'])
        aps.getLeaf("Br07").addCreature("Centaur")
        aps.getLeaf("Gr02").split(2, "Gr12", 4)
        aps.getLeaf("Gr07").split(2, "Gr01", 4)
        aps.getLeaf("Gr02").revealCreatures(['Lion'])
        aps.getLeaf("Gr02").addCreature("Lion")
        aps.getLeaf("Bk03").split(2, "Bk06", 4)
        aps.getLeaf("Bk07").revealCreatures(['Titan'])
        aps.getLeaf("Bk07").revealCreatures(['Titan'])
        aps.getLeaf("Bk07").addCreature("Warlock")
        aps.getLeaf("Bu05").split(2, "Bu09", 4)
        aps.getLeaf("Bu05").revealCreatures(['Cyclops'])
        aps.getLeaf("Bu05").addCreature("Cyclops")
        aps.getLeaf("Rd12").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd12").addCreature("Cyclops")
        aps.printLeaves()
        turn = 5
        print "\nTurn", turn
        aps.getLeaf("Gd08").split(2, "Gd01", 5)
        aps.getLeaf("Gd01").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd01").addCreature("Cyclops")
        aps.getLeaf("Gd08").revealCreatures(['Centaur', 'Centaur', 'Centaur'])
        aps.getLeaf("Gd08").addCreature("Warbear")
        aps.getLeaf("Br07").split(2, "Br06", 5)
        aps.getLeaf("Br07").revealCreatures(['Angel', 'Troll', 'Troll', 'Ogre', 'Ogre'])
        aps.getLeaf("Rd05").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd05").removeCreature("Gargoyle")
        aps.getLeaf("Rd05").removeCreature("Gargoyle")
        aps.getLeaf("Br07").revealCreatures(['Angel', 'Troll', 'Troll', 'Ogre', 'Ogre'])
        aps.getLeaf("Br07").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br07").addCreature("Warbear")
        aps.getLeaf("Gr02").revealCreatures(['Lion'])
        aps.getLeaf("Gr02").addCreature("Lion")
        aps.getLeaf("Gr07").revealCreatures(['Ogre'])
        aps.getLeaf("Gr07").addCreature("Ogre")
        aps.getLeaf("Bk07").split(2, "Bk04", 5)
        aps.getLeaf("Bk04").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk04").addCreature("Cyclops")
        aps.getLeaf("Rd09").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Rd09").addCreature("Lion")
        aps.printLeaves()
        turn = 6
        print "\nTurn", turn
        aps.getLeaf("Gd12").split(2, "Gd11", 6)
        aps.getLeaf("Gd01").addCreature("Ogre")
        aps.getLeaf("Gd08").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gd08").addCreature("Lion")
        aps.getLeaf("Gd12").revealCreatures(['Titan'])
        aps.getLeaf("Gd12").addCreature("Warlock")
        aps.getLeaf("Br02").revealCreatures(['Cyclops'])
        aps.getLeaf("Br02").addCreature("Cyclops")
        aps.getLeaf("Br07").revealCreatures(['Warbear'])
        aps.getLeaf("Br07").addCreature("Warbear")
        aps.getLeaf("Gr02").split(2, "Gr09", 6)
        aps.getLeaf("Gr01").addCreature("Ogre")
        aps.getLeaf("Gr02").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Gr02").addCreature("Ranger")
        aps.getLeaf("Gr07").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr07").addCreature("Cyclops")
        aps.getLeaf("Bk07").revealCreatures(['Titan'])
        aps.getLeaf("Bk04").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk04").addCreature("Cyclops")
        aps.getLeaf("Bk07").addCreature("Ogre")
        aps.getLeaf("Bu05").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bu05").addCreature("Behemoth")
        aps.getLeaf("Rd09").revealCreatures(['Lion'])
        aps.getLeaf("Rd09").addCreature("Lion")
        aps.printLeaves()
        turn = 7
        print "\nTurn", turn
        aps.getLeaf("Gd08").split(2, "Gd06", 7)
        aps.getLeaf("Gd12").revealCreatures(['Troll'])
        aps.getLeaf("Gd12").addCreature("Troll")
        aps.getLeaf("Gr07").split(2, "Gr03", 7)
        aps.getLeaf("Gr01").revealCreatures(['Ogre'])
        aps.getLeaf("Gr01").addCreature("Ogre")
        aps.getLeaf("Gr02").revealCreatures(['Ranger'])
        aps.getLeaf("Gr02").addCreature("Ranger")
        aps.getLeaf("Bk07").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk07").addCreature("Troll")
        aps.printLeaves()
        turn = 8
        print "\nTurn", turn
        aps.getLeaf("Rd12").split(2, "Rd01", 8)
        aps.getLeaf("Rd09").split(2, "Rd11", 8)
        aps.getLeaf("Rd09").merge(aps.getLeaf("Rd11"), turn)
        aps.getLeaf("Rd12").revealCreatures(['Troll'])
        aps.getLeaf("Rd12").addCreature("Troll")
        aps.getLeaf("Gd01").revealCreatures(['Ogre'])
        aps.getLeaf("Gd01").addCreature("Ogre")
        aps.getLeaf("Br07").split(2, "Br01", 8)
        aps.getLeaf("Gr02").revealCreatures(['Angel', 'Ranger', 'Ranger', 'Lion', 'Lion', 'Lion', 'Centaur'])
        aps.getLeaf("Bu01").revealCreatures(['Angel', 'Centaur', 'Centaur', 'Centaur', 'Ogre'])
        aps.getLeaf("Bu01").removeCreature("Centaur")
        aps.getLeaf("Gr02").removeCreature("Ranger")
        aps.getLeaf("Bu01").removeCreature("Angel")
        aps.getLeaf("Gr02").removeCreature("Angel")
        aps.getLeaf("Bu01").removeCreature("Ogre")
        aps.getLeaf("Gr02").removeCreature("Lion")
        aps.getLeaf("Bu01").removeCreature("Centaur")
        aps.getLeaf("Gr02").removeCreature("Ranger")
        aps.getLeaf("Bu01").removeCreature("Centaur")
        aps.getLeaf("Gr02").revealCreatures(['Lion', 'Lion', 'Centaur'])
        aps.getLeaf("Gr01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gr01").addCreature("Troll")
        aps.getLeaf("Bk07").split(2, "Bk06", 8)
        aps.getLeaf("Bk06").revealCreatures(['Ogre'])
        aps.getLeaf("Bk06").addCreature("Ogre")
        aps.getLeaf("Bk07").revealCreatures(['Troll'])
        aps.getLeaf("Bk07").addCreature("Troll")
        aps.getLeaf("Bu05").revealCreatures(['Titan', 'Behemoth', 'Warlock', 'Cyclops', 'Cyclops', 'Cyclops', 'Ogre'])
        aps.getLeaf("Gd12").revealCreatures(['Titan', 'Warlock', 'Warlock', 'Troll', 'Troll', 'Troll', 'Gargoyle'])
        aps.getLeaf("Gd12").removeCreature("Gargoyle")
        aps.getLeaf("Bu05").removeCreature("Cyclops")
        aps.getLeaf("Gd12").removeCreature("Troll")
        aps.getLeaf("Bu05").removeCreature("Cyclops")
        aps.getLeaf("Gd12").revealCreatures(['Troll'])
        aps.getLeaf("Gd12").addCreature("Troll")
        aps.getLeaf("Gd12").removeCreature("Troll")
        aps.getLeaf("Bu05").removeCreature("Ogre")
        aps.getLeaf("Gd12").removeCreature("Troll")
        aps.getLeaf("Bu05").removeCreature("Warlock")
        aps.getLeaf("Gd12").removeCreature("Warlock")
        aps.getLeaf("Gd12").removeCreature("Troll")
        aps.getLeaf("Bu05").removeCreature("Behemoth")
        aps.getLeaf("Gd12").revealCreatures(['Titan', 'Warlock'])
        aps.printLeaves()
        turn = 9
        print "\nTurn", turn
        aps.getLeaf("Rd09").split(2, "Rd02", 9)
        aps.getLeaf("Rd12").revealCreatures(['Titan', 'Cyclops', 'Ranger', 'Troll', 'Troll', 'Troll'])
        aps.getLeaf("Gd12").revealCreatures(['Titan', 'Warlock'])
        aps.getLeaf("Rd12").removeCreature("Ranger")
        aps.getLeaf("Gd12").removeCreature("Warlock")
        aps.getLeaf("Rd12").removeCreature("Troll")
        aps.getLeaf("Rd09").removeCreature("Angel")
        aps.getLeaf("Rd12").addCreature("Angel")
        aps.getLeaf("Gd12").removeCreature("Titan")
        aps.getLeaf("Rd12").removeCreature("Cyclops")
        aps.getLeaf("Rd12").revealCreatures(['Titan', 'Angel', 'Troll', 'Troll'])
        aps.getLeaf("Rd12").revealCreatures(['Troll'])
        aps.getLeaf("Rd12").addCreature("Troll")
        aps.getLeaf("Br07").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Br07").addCreature("Giant")
        aps.getLeaf("Gr01").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr01").addCreature("Cyclops")
        aps.getLeaf("Gr02").revealCreatures(['Lion'])
        aps.getLeaf("Gr02").addCreature("Lion")
        aps.getLeaf("Bk03").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bk03").addCreature("Lion")
        aps.printLeaves()
        turn = 10
        print "\nTurn", turn
        aps.getLeaf("Br02").split(2, "Br04", 10)
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Behemoth")
        aps.getLeaf("Br07").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Br07").addCreature("Unicorn")
        aps.getLeaf("Gr01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gr01").addCreature("Troll")
        aps.getLeaf("Gr02").revealCreatures(['Lion', 'Lion', 'Lion'])
        aps.getLeaf("Gr02").addCreature("Griffon")
        aps.getLeaf("Gr07").revealCreatures(['Titan'])
        aps.getLeaf("Gr07").addCreature("Warlock")
        aps.getLeaf("Bk03").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk03").addCreature("Cyclops")
        aps.getLeaf("Bk07").revealCreatures(['Troll'])
        aps.getLeaf("Bk07").addCreature("Troll")
        aps.getLeaf("Rd12").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Rd12").addCreature("Ranger")
        aps.getLeaf("Rd09").revealCreatures(['Lion', 'Lion', 'Lion'])
        aps.getLeaf("Rd09").addCreature("Griffon")
        aps.getLeaf("Br02").revealCreatures(['Centaur'])
        aps.getLeaf("Br02").addCreature("Centaur")
        aps.printLeaves()
        turn = 11
        print "\nTurn", turn
        aps.getLeaf("Gr01").split(2, "Gr06", 11)
        aps.getLeaf("Gr07").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr07").addCreature("Behemoth")
        aps.getLeaf("Bk07").split(2, "Bk12", 11)
        aps.getLeaf("Bk06").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk06").addCreature("Troll")
        aps.getLeaf("Bk07").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bk07").addCreature("Ranger")
        aps.getLeaf("Rd09").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Rd09").addCreature("Ranger")
        aps.getLeaf("Rd12").revealCreatures(['Titan'])
        aps.getLeaf("Rd12").addCreature("Warlock")
        aps.printLeaves()
        turn = 12
        print "\nTurn", turn
        aps.getLeaf("Br02").split(2, "Br01", 12)
        aps.getLeaf("Br07").split(2, "Br05", 12)
        aps.getLeaf("Br07").addCreature("Angel")
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Gorgon")
        aps.getLeaf("Br05").revealCreatures(['Troll'])
        aps.getLeaf("Br05").addCreature("Troll")
        aps.getLeaf("Gr01").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr01").addCreature("Cyclops")
        aps.getLeaf("Gr03").revealCreatures(['Ogre'])
        aps.getLeaf("Gr03").addCreature("Ogre")
        aps.getLeaf("Bk03").split(2, "Bk09", 12)
        aps.getLeaf("Bk07").revealCreatures(['Ranger'])
        aps.getLeaf("Bk07").addCreature("Ranger")
        aps.getLeaf("Rd09").revealCreatures(['Ranger'])
        aps.getLeaf("Rd09").addCreature("Ranger")
        aps.getLeaf("Br06").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br06").addCreature("Lion")
        aps.getLeaf("Gr02").addCreature("Angel")
        aps.getLeaf("Gr01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gr01").addCreature("Troll")
        aps.getLeaf("Gr02").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Gr02").addCreature("Ranger")
        aps.getLeaf("Bk03").revealCreatures(['Centaur'])
        aps.getLeaf("Bk03").addCreature("Centaur")
        aps.printLeaves()
        turn = 14
        print "\nTurn", turn
        aps.getLeaf("Rd09").split(2, "Rd11", 14)
        aps.getLeaf("Rd12").split(2, "Rd03", 14)
        aps.getLeaf("Rd12").revealCreatures(['Titan'])
        aps.getLeaf("Rd12").revealCreatures(['Titan'])
        aps.getLeaf("Rd09").revealCreatures(['Ranger'])
        aps.getLeaf("Rd09").addCreature("Ranger")
        aps.getLeaf("Rd12").revealCreatures(['Troll', 'Troll', 'Troll'])
        aps.getLeaf("Rd12").addCreature("Wyvern")
        aps.getLeaf("Rd03").revealCreatures(['Ranger'])
        aps.getLeaf("Rd03").addCreature("Ranger")
        aps.getLeaf("Br05").revealCreatures(['Troll', 'Troll', 'Troll'])
        aps.getLeaf("Br05").addCreature("Wyvern")
        aps.getLeaf("Br07").revealCreatures(['Unicorn'])
        aps.getLeaf("Br07").addCreature("Unicorn")
        aps.getLeaf("Gr01").split(2, "Gr12", 14)
        aps.getLeaf("Gr07").split(2, "Gr04", 14)
        aps.getLeaf("Gr12").revealCreatures(['Ogre'])
        aps.getLeaf("Gr12").addCreature("Ogre")
        aps.getLeaf("Bk07").split(2, "Bk04", 14)
        aps.getLeaf("Bk03").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk03").addCreature("Cyclops")
        aps.getLeaf("Bk07").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bk07").addCreature("Warbear")
        aps.getLeaf("Rd09").revealCreatures(['Cyclops'])
        aps.getLeaf("Rd09").addCreature("Cyclops")
        aps.printLeaves()
        turn = 15
        print "\nTurn", turn
        aps.getLeaf("Br07").split(2, "Br10", 15)
        aps.getLeaf("Gr02").split(2, "Gr05", 15)
        aps.getLeaf("Gr03").revealCreatures(['Ogre', 'Ogre', 'Ogre'])
        aps.getLeaf("Gr03").addCreature("Minotaur")
        aps.getLeaf("Gr06").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.getLeaf("Gr07").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr07").addCreature("Cyclops")
        aps.getLeaf("Bk03").split(2, "Bk09", 15)
        aps.getLeaf("Rd09").revealCreatures(['Griffon', 'Cyclops', 'Cyclops', 'Ranger', 'Ranger', 'Ranger', 'Lion'])
        aps.getLeaf("Bk03").revealCreatures(['Angel', 'Cyclops', 'Cyclops', 'Cyclops', 'Lion'])
        aps.getLeaf("Bk03").removeCreature("Lion")
        aps.getLeaf("Bk03").removeCreature("Cyclops")
        aps.getLeaf("Rd09").removeCreature("Ranger")
        aps.getLeaf("Rd03").removeCreature("Angel")
        aps.getLeaf("Rd09").addCreature("Angel")
        aps.getLeaf("Bk03").removeCreature("Angel")
        aps.getLeaf("Rd09").removeCreature("Lion")
        aps.getLeaf("Bk03").removeCreature("Cyclops")
        aps.getLeaf("Rd09").removeCreature("Cyclops")
        aps.getLeaf("Bk03").removeCreature("Cyclops")
        aps.getLeaf("Rd09").revealCreatures(['Angel', 'Griffon', 'Cyclops', 'Ranger', 'Ranger'])
        aps.getLeaf("Rd09").addCreature("Angel")
        aps.getLeaf("Rd09").revealCreatures(['Cyclops'])
        aps.getLeaf("Rd09").addCreature("Cyclops")
        aps.getLeaf("Br10").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Br10").addCreature("Unicorn")
        aps.getLeaf("Gr07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gr07").addCreature("Gorgon")
        aps.getLeaf("Bk07").revealCreatures(['Warbear'])
        aps.getLeaf("Bk07").addCreature("Warbear")
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Behemoth")
        aps.getLeaf("Br05").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br05").addCreature("Ranger")
        aps.printLeaves()
        turn = 17
        print "\nTurn", turn
        aps.getLeaf("Gr07").split(2, "Gr03", 17)
        aps.getLeaf("Gr02").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Gr02").addCreature("Ranger")
        aps.printLeaves()
        turn = 18
        print "\nTurn", turn
        aps.getLeaf("Rd09").split(2, "Rd01", 18)
        aps.getLeaf("Rd09").revealCreatures(['Griffon'])
        aps.getLeaf("Rd09").addCreature("Griffon")
        aps.getLeaf("Rd11").revealCreatures(['Lion'])
        aps.getLeaf("Rd11").addCreature("Lion")
        aps.getLeaf("Br04").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br04").addCreature("Lion")
        aps.getLeaf("Br05").revealCreatures(['Ranger'])
        aps.getLeaf("Br05").addCreature("Ranger")
        aps.getLeaf("Gr02").revealCreatures(['Griffon'])
        aps.getLeaf("Gr02").addCreature("Griffon")
        aps.getLeaf("Gr03").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr03").addCreature("Cyclops")
        aps.getLeaf("Rd12").revealCreatures(['Titan'])
        aps.getLeaf("Rd12").addCreature("Warlock")
        aps.getLeaf("Rd03").revealCreatures(['Ranger'])
        aps.getLeaf("Rd03").addCreature("Ranger")
        aps.getLeaf("Rd09").revealCreatures(['Ranger'])
        aps.getLeaf("Rd09").addCreature("Ranger")
        aps.getLeaf("Br05").revealCreatures(['Ranger'])
        aps.getLeaf("Br05").addCreature("Ranger")
        aps.printLeaves()
        turn = 19
        print "\nTurn", turn
        aps.getLeaf("Gr02").split(2, "Gr12", 19)
        aps.getLeaf("Gr02").revealCreatures(['Ranger'])
        aps.getLeaf("Gr02").addCreature("Ranger")
        aps.getLeaf("Bk07").split(2, "Bk03", 19)
        aps.getLeaf("Bk07").addCreature("Angel")
        aps.printLeaves()
        turn = 20
        print "\nTurn", turn
        aps.getLeaf("Rd12").split(2, "Rd10", 20)
        aps.getLeaf("Rd09").split(2, "Rd05", 20)
        aps.getLeaf("Rd05").revealCreatures(['Angel', 'Ranger'])
        aps.getLeaf("Bk04").revealCreatures(['Ranger', 'Ranger'])
        aps.getLeaf("Bk04").removeCreature("Ranger")
        aps.getLeaf("Rd05").removeCreature("Ranger")
        aps.getLeaf("Rd01").removeCreature("Angel")
        aps.getLeaf("Rd05").addCreature("Angel")
        aps.getLeaf("Bk04").removeCreature("Ranger")
        aps.getLeaf("Rd05").revealCreatures(['Angel', 'Angel'])
        aps.getLeaf("Rd05").addCreature("Angel")
        aps.getLeaf("Rd09").revealCreatures(['Ranger'])
        aps.getLeaf("Rd09").addCreature("Ranger")
        aps.getLeaf("Rd12").revealCreatures(['Wyvern'])
        aps.getLeaf("Rd12").addCreature("Wyvern")
        aps.getLeaf("Br02").split(2, "Br09", 20)
        aps.getLeaf("Br02").revealCreatures(['Behemoth', 'Behemoth'])
        aps.getLeaf("Br02").addCreature("Serpent")
        aps.getLeaf("Gr02").revealCreatures(['Ranger'])
        aps.getLeaf("Gr02").addCreature("Ranger")
        aps.getLeaf("Gr05").revealCreatures(['Lion'])
        aps.getLeaf("Gr05").addCreature("Lion")
        aps.getLeaf("Gr07").revealCreatures(['Behemoth'])
        aps.getLeaf("Gr07").addCreature("Behemoth")
        aps.getLeaf("Bk03").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bk03").addCreature("Ranger")
        aps.getLeaf("Rd12").revealCreatures(['Titan', 'Wyvern', 'Wyvern', 'Warlock', 'Warlock', 'Troll'])
        aps.getLeaf("Bk07").revealCreatures(['Titan', 'Angel', 'Warlock', 'Warlock', 'Warbear', 'Warbear'])
        aps.getLeaf("Bk07").removeCreature("Warbear")
        aps.getLeaf("Rd12").removeCreature("Wyvern")
        aps.getLeaf("Rd05").removeCreature("Angel")
        aps.getLeaf("Rd12").addCreature("Angel")
        aps.getLeaf("Bk07").removeCreature("Angel")
        aps.getLeaf("Rd12").removeCreature("Angel")
        aps.getLeaf("Bk07").removeCreature("Warbear")
        aps.getLeaf("Bk07").removeCreature("Warlock")
        aps.getLeaf("Bk07").removeCreature("Warlock")
        aps.getLeaf("Rd12").removeCreature("Warlock")
        aps.getLeaf("Rd12").removeCreature("Troll")
        aps.getLeaf("Bk07").removeCreature("Titan")
        aps.getLeaf("Rd12").revealCreatures(['Titan', 'Wyvern', 'Warlock'])
        aps.getLeaf("Rd12").addCreature("Angel")
        aps.getLeaf("Rd10").revealCreatures(['Troll'])
        aps.getLeaf("Rd10").addCreature("Troll")
        aps.getLeaf("Rd03").revealCreatures(['Ranger'])
        aps.getLeaf("Rd03").addCreature("Ranger")
        aps.getLeaf("Br02").revealCreatures(['Gorgon'])
        aps.getLeaf("Br02").addCreature("Gorgon")
        aps.getLeaf("Br09").revealCreatures(['Cyclops'])
        aps.getLeaf("Br09").addCreature("Cyclops")
        aps.printLeaves()
        turn = 21
        print "\nTurn", turn
        aps.getLeaf("Gr02").split(2, "Gr04", 21)
        aps.getLeaf("Gr02").revealCreatures(['Ranger'])
        aps.getLeaf("Gr02").addCreature("Ranger")
        aps.getLeaf("Gr05").revealCreatures(['Lion'])
        aps.getLeaf("Gr05").addCreature("Lion")
        aps.getLeaf("Gr06").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.getLeaf("Rd09").revealCreatures(['Griffon', 'Griffon'])
        aps.getLeaf("Rd09").addCreature("Hydra")
        aps.getLeaf("Rd12").revealCreatures(['Titan'])
        aps.getLeaf("Rd12").addCreature("Warlock")
        aps.printLeaves()
        turn = 22
        print "\nTurn", turn
        aps.getLeaf("Br02").split(2, "Br11", 22)
        aps.getLeaf("Br05").split(2, "Br04", 22)
        aps.getLeaf("Br07").revealCreatures(['Giant'])
        aps.getLeaf("Br07").addCreature("Giant")
        aps.getLeaf("Gr02").revealCreatures(['Angel', 'Griffon', 'Griffon', 'Ranger', 'Ranger', 'Ranger'])
        aps.getLeaf("Rd05").revealCreatures(['Angel', 'Angel'])
        aps.getLeaf("Gr02").removeCreature("Griffon")
        aps.getLeaf("Rd05").removeCreature("Angel")
        aps.getLeaf("Gr02").removeCreature("Ranger")
        aps.getLeaf("Rd05").removeCreature("Angel")
        aps.getLeaf("Gr02").revealCreatures(['Angel', 'Griffon', 'Ranger', 'Ranger'])
        aps.getLeaf("Gr02").revealCreatures(['Ranger'])
        aps.getLeaf("Gr02").addCreature("Ranger")
        aps.getLeaf("Gr04").revealCreatures(['Ranger'])
        aps.getLeaf("Gr04").addCreature("Ranger")
        aps.getLeaf("Gr05").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Gr05").addCreature("Minotaur")
        aps.getLeaf("Rd10").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Rd10").addCreature("Ranger")
        aps.getLeaf("Br05").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br05").addCreature("Warbear")
        aps.getLeaf("Br09").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Br09").addCreature("Gorgon")
        aps.getLeaf("Gr02").revealCreatures(['Ranger', 'Ranger', 'Ranger'])
        aps.getLeaf("Gr02").addCreature("Guardian")
        aps.getLeaf("Gr04").revealCreatures(['Ranger', 'Ranger', 'Ranger'])
        aps.getLeaf("Gr04").addCreature("Guardian")
        aps.getLeaf("Gr05").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Gr05").addCreature("Ranger")
        aps.getLeaf("Gr06").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.getLeaf("Gr07").revealCreatures(['Gorgon'])
        aps.getLeaf("Gr07").addCreature("Gorgon")
        aps.getLeaf("Rd12").revealCreatures(['Titan'])
        aps.getLeaf("Rd12").revealCreatures(['Wyvern'])
        aps.getLeaf("Rd12").addCreature("Wyvern")
        aps.printLeaves()
        turn = 24
        print "\nTurn", turn
        aps.getLeaf("Gr07").split(2, "Gr08", 24)
        aps.getLeaf("Gr02").revealCreatures(['Ranger'])
        aps.getLeaf("Gr02").addCreature("Ranger")
        aps.getLeaf("Gr04").revealCreatures(['Ranger'])
        aps.getLeaf("Gr04").addCreature("Ranger")
        aps.printLeaves()
        turn = 25
        print "\nTurn", turn
        aps.getLeaf("Rd09").split(2, "Rd08", 25)
        aps.getLeaf("Rd09").revealCreatures(['Hydra', 'Griffon', 'Griffon', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops', 'Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").removeCreature("Gargoyle")
        aps.getLeaf("Rd12").removeCreature("Angel")
        aps.getLeaf("Rd09").addCreature("Angel")
        aps.getLeaf("Gr06").removeCreature("Cyclops")
        aps.getLeaf("Gr06").removeCreature("Cyclops")
        aps.getLeaf("Gr06").removeCreature("Gargoyle")
        aps.getLeaf("Gr06").removeCreature("Cyclops")
        aps.getLeaf("Rd09").revealCreatures(['Hydra', 'Angel', 'Griffon', 'Griffon', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Rd09").addCreature("Angel")
        aps.getLeaf("Rd03").revealCreatures(['Ranger'])
        aps.getLeaf("Rd03").addCreature("Troll")
        aps.getLeaf("Rd03").removeCreature("Troll")
        aps.getLeaf("Br05").revealCreatures(['Ranger'])
        aps.getLeaf("Br05").addCreature("Lion")
        aps.getLeaf("Br07").revealCreatures(['Unicorn'])
        aps.getLeaf("Br07").addCreature("Unicorn")
        aps.getLeaf("Br09").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Br09").addCreature("Gorgon")
        aps.printLeaves()


    def testPredictSplits6(self):
        print "\ntest 6 begins"
        ps = PredictSplits("Gd", "Gd08", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gd08").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Bu", "Bu06", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bu06").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Bk", "Bk08", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bk08").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Gr", "Gr12", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gr12").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Rd", "Rd09", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Rd09").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Br", "Br02", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Br02").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.printLeaves()
        turn = 1
        print "\nTurn", turn
        aps.getLeaf("Gd08").split(4, "Gd10", 1)
        aps.getLeaf("Gd08").revealCreatures(['Titan'])
        aps.getLeaf("Gd08").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gd08").addCreature("Troll")
        aps.getLeaf("Gd10").revealCreatures(['Centaur'])
        aps.getLeaf("Gd10").addCreature("Centaur")
        aps.getLeaf("Bu06").split(4, "Bu11", 1)
        aps.getLeaf("Bu06").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bu06").addCreature("Troll")
        aps.getLeaf("Bu11").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bu11").addCreature("Lion")
        aps.getLeaf("Bk08").split(4, "Bk01", 1)
        aps.getLeaf("Bk08").revealCreatures(['Titan'])
        aps.getLeaf("Bk01").revealCreatures(['Ogre'])
        aps.getLeaf("Bk01").addCreature("Ogre")
        aps.getLeaf("Bk08").revealCreatures(['Titan'])
        aps.getLeaf("Bk08").addCreature("Warlock")
        aps.getLeaf("Gr12").split(4, "Gr06", 1)
        aps.getLeaf("Gr06").revealCreatures(['Ogre'])
        aps.getLeaf("Gr06").addCreature("Ogre")
        aps.getLeaf("Gr12").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gr12").addCreature("Lion")
        aps.getLeaf("Rd09").split(4, "Rd07", 1)
        aps.getLeaf("Rd09").revealCreatures(['Gargoyle'])
        aps.getLeaf("Rd09").addCreature("Gargoyle")
        aps.getLeaf("Rd07").revealCreatures(['Gargoyle'])
        aps.getLeaf("Rd07").addCreature("Gargoyle")
        aps.getLeaf("Br02").split(4, "Br11", 1)
        aps.getLeaf("Br02").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br02").addCreature("Cyclops")
        aps.getLeaf("Gd10").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd10").addCreature("Cyclops")
        aps.getLeaf("Bu06").revealCreatures(['Titan'])
        aps.getLeaf("Bu06").addCreature("Warlock")
        aps.getLeaf("Bu11").revealCreatures(['Lion'])
        aps.getLeaf("Bu11").addCreature("Lion")
        aps.getLeaf("Bk01").revealCreatures(['Centaur'])
        aps.getLeaf("Bk01").addCreature("Centaur")
        aps.getLeaf("Bk08").revealCreatures(['Centaur'])
        aps.getLeaf("Bk08").addCreature("Centaur")
        aps.getLeaf("Gr12").revealCreatures(['Ogre'])
        aps.getLeaf("Gr12").addCreature("Ogre")
        aps.getLeaf("Rd09").revealCreatures(['Titan'])
        aps.getLeaf("Rd09").addCreature("Warlock")
        aps.getLeaf("Br02").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br02").addCreature("Cyclops")
        aps.getLeaf("Br11").revealCreatures(['Centaur'])
        aps.getLeaf("Br11").addCreature("Centaur")
        aps.getLeaf("Gd08").revealCreatures(['Troll'])
        aps.getLeaf("Gd08").addCreature("Troll")
        aps.getLeaf("Gd10").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd10").addCreature("Cyclops")
        aps.getLeaf("Bu06").revealCreatures(['Titan'])
        aps.getLeaf("Bu06").revealCreatures(['Titan'])
        aps.getLeaf("Bu06").addCreature("Warlock")
        aps.getLeaf("Bk01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk01").addCreature("Troll")
        aps.getLeaf("Bk08").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk08").addCreature("Cyclops")
        aps.getLeaf("Gr12").revealCreatures(['Centaur'])
        aps.getLeaf("Gr12").addCreature("Centaur")
        aps.getLeaf("Rd07").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd07").addCreature("Cyclops")
        aps.getLeaf("Br11").revealCreatures(['Ogre'])
        aps.getLeaf("Br11").addCreature("Ogre")
        aps.printLeaves()
        turn = 4
        print "\nTurn", turn
        aps.getLeaf("Gd10").split(2, "Gd01", 4)
        aps.getLeaf("Gd08").revealCreatures(['Centaur'])
        aps.getLeaf("Gd08").addCreature("Centaur")
        aps.getLeaf("Gd10").revealCreatures(['Cyclops'])
        aps.getLeaf("Gd10").addCreature("Cyclops")
        aps.getLeaf("Bu06").split(2, "Bu12", 4)
        aps.getLeaf("Bu06").revealCreatures(['Troll'])
        aps.getLeaf("Bu06").addCreature("Troll")
        aps.getLeaf("Bu11").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bu11").addCreature("Lion")
        aps.getLeaf("Bk01").split(2, "Bk07", 4)
        aps.getLeaf("Bk08").split(2, "Bk02", 4)
        aps.getLeaf("Bk01").revealCreatures(['Ogre', 'Ogre', 'Ogre'])
        aps.getLeaf("Bk01").addCreature("Minotaur")
        aps.getLeaf("Bk07").revealCreatures(['Centaur'])
        aps.getLeaf("Bk07").addCreature("Centaur")
        aps.getLeaf("Bk08").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk08").addCreature("Cyclops")
        aps.getLeaf("Gr06").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.getLeaf("Rd09").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Rd09").addCreature("Troll")
        aps.getLeaf("Br02").revealCreatures(['Ogre'])
        aps.getLeaf("Br02").addCreature("Ogre")
        aps.printLeaves()
        turn = 5
        print "\nTurn", turn
        aps.getLeaf("Gd08").split(2, "Gd03", 5)
        aps.getLeaf("Gd10").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gd10").addCreature("Behemoth")
        aps.getLeaf("Bu11").split(2, "Bu10", 5)
        aps.getLeaf("Bu11").revealCreatures(['Gargoyle'])
        aps.getLeaf("Bu11").addCreature("Gargoyle")
        aps.getLeaf("Bk08").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bk08").addCreature("Lion")
        aps.getLeaf("Gr12").split(2, "Gr07", 5)
        aps.getLeaf("Gr12").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gr12").addCreature("Troll")
        aps.getLeaf("Rd09").split(2, "Rd06", 5)
        aps.getLeaf("Rd09").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd09").addCreature("Cyclops")
        aps.getLeaf("Rd07").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd07").addCreature("Cyclops")
        aps.getLeaf("Br02").split(2, "Br10", 5)
        aps.getLeaf("Br11").revealCreatures(['Centaur', 'Centaur', 'Centaur'])
        aps.getLeaf("Br11").addCreature("Warbear")
        aps.getLeaf("Gd10").revealCreatures(['Angel', 'Behemoth', 'Cyclops', 'Cyclops', 'Cyclops', 'Centaur', 'Centaur'])
        aps.getLeaf("Gr12").revealCreatures(['Angel', 'Troll', 'Lion', 'Centaur', 'Ogre', 'Ogre'])
        aps.getLeaf("Gr12").removeCreature("Centaur")
        aps.getLeaf("Gr12").removeCreature("Troll")
        aps.getLeaf("Gr12").removeCreature("Angel")
        aps.getLeaf("Gr12").removeCreature("Ogre")
        aps.getLeaf("Gd10").removeCreature("Angel")
        aps.getLeaf("Gd10").removeCreature("Centaur")
        aps.getLeaf("Gr12").removeCreature("Lion")
        aps.getLeaf("Gr12").removeCreature("Ogre")
        aps.getLeaf("Gd10").revealCreatures(['Behemoth', 'Cyclops', 'Cyclops', 'Cyclops', 'Centaur'])
        aps.getLeaf("Gd10").addCreature("Angel")
        aps.getLeaf("Gd08").revealCreatures(['Troll'])
        aps.getLeaf("Gd08").addCreature("Troll")
        aps.getLeaf("Bu12").revealCreatures(['Ogre'])
        aps.getLeaf("Bu12").addCreature("Ogre")
        aps.printLeaves()
        turn = 6
        print "\nTurn", turn
        aps.getLeaf("Bk08").split(2, "Bk02", 6)
        aps.getLeaf("Bk01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk01").addCreature("Troll")
        aps.getLeaf("Bk08").revealCreatures(['Titan'])
        aps.getLeaf("Bk08").addCreature("Warlock")
        aps.getLeaf("Gr06").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.getLeaf("Rd07").split(2, "Rd08", 6)
        aps.getLeaf("Rd09").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd09").addCreature("Cyclops")
        aps.getLeaf("Br11").split(2, "Br04", 6)
        aps.getLeaf("Br02").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br02").addCreature("Troll")
        aps.getLeaf("Br11").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br11").addCreature("Troll")
        aps.getLeaf("Gd10").revealCreatures(['Centaur'])
        aps.getLeaf("Gd10").addCreature("Centaur")
        aps.getLeaf("Bu06").revealCreatures(['Troll'])
        aps.getLeaf("Bu06").addCreature("Troll")
        aps.getLeaf("Bu12").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bu12").addCreature("Troll")
        aps.printLeaves()
        turn = 7
        print "\nTurn", turn
        aps.getLeaf("Bk01").split(2, "Bk05", 7)
        aps.getLeaf("Bk08").revealCreatures(['Titan'])
        aps.getLeaf("Bk08").revealCreatures(['Titan'])
        aps.getLeaf("Bk08").addCreature("Warlock")
        aps.getLeaf("Gr06").split(2, "Gr04", 7)
        aps.getLeaf("Gr06").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.getLeaf("Rd09").split(2, "Rd05", 7)
        aps.getLeaf("Rd07").revealCreatures(['Cyclops'])
        aps.getLeaf("Rd07").addCreature("Cyclops")
        aps.getLeaf("Br02").revealCreatures(['Troll'])
        aps.getLeaf("Br02").addCreature("Troll")
        aps.getLeaf("Br10").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br10").addCreature("Cyclops")
        aps.printLeaves()
        turn = 8
        print "\nTurn", turn
        aps.getLeaf("Gd10").split(2, "Gd09", 8)
        aps.getLeaf("Gd03").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gd03").addCreature("Lion")
        aps.getLeaf("Gd08").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Gd08").addCreature("Ranger")
        aps.getLeaf("Gd10").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gd10").addCreature("Guardian")
        aps.getLeaf("Bk08").split(2, "Bk10", 8)
        aps.getLeaf("Bk08").revealCreatures(['Titan'])
        aps.getLeaf("Bk01").revealCreatures(['Troll'])
        aps.getLeaf("Bk01").addCreature("Troll")
        aps.getLeaf("Bk08").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk08").addCreature("Cyclops")
        aps.getLeaf("Bk10").revealCreatures(['Lion'])
        aps.getLeaf("Bk10").addCreature("Lion")
        aps.getLeaf("Br02").split(2, "Br01", 8)
        aps.getLeaf("Br01").revealCreatures(['Ogre'])
        aps.getLeaf("Br01").addCreature("Ogre")
        aps.getLeaf("Br10").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br10").addCreature("Cyclops")
        aps.getLeaf("Br11").revealCreatures(['Warbear'])
        aps.getLeaf("Br11").addCreature("Warbear")
        aps.printLeaves()
        turn = 9
        print "\nTurn", turn
        aps.getLeaf("Gd08").split(2, "Gd06", 9)
        aps.getLeaf("Gd08").revealCreatures(['Ranger'])
        aps.getLeaf("Gd08").addCreature("Ranger")
        aps.getLeaf("Gd10").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd10").addCreature("Gorgon")
        aps.getLeaf("Bu11").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu11").addCreature("Ranger")
        aps.getLeaf("Bu12").revealCreatures(['Ogre', 'Ogre', 'Ogre'])
        aps.getLeaf("Bu12").addCreature("Minotaur")
        aps.getLeaf("Bk08").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk08").addCreature("Cyclops")
        aps.getLeaf("Gr04").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr04").addCreature("Cyclops")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Gorgon")
        aps.getLeaf("Rd07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Rd07").addCreature("Gorgon")
        aps.getLeaf("Rd09").revealCreatures(['Troll'])
        aps.getLeaf("Rd09").addCreature("Troll")
        aps.getLeaf("Br11").split(2, "Br03", 9)
        aps.getLeaf("Br02").revealCreatures(['Cyclops'])
        aps.getLeaf("Br02").addCreature("Cyclops")
        aps.getLeaf("Br04").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br04").addCreature("Lion")
        aps.printLeaves()
        turn = 10
        print "\nTurn", turn
        aps.getLeaf("Gd10").split(2, "Gd11", 10)
        aps.getLeaf("Gd03").addCreature("Centaur")
        aps.getLeaf("Gd08").revealCreatures(['Troll', 'Troll', 'Troll'])
        aps.getLeaf("Gd08").addCreature("Guardian")
        aps.getLeaf("Gd10").revealCreatures(['Behemoth'])
        aps.getLeaf("Gd10").addCreature("Behemoth")
        aps.getLeaf("Bu06").split(2, "Bu10", 10)
        aps.getLeaf("Bu11").split(2, "Bu03", 10)
        aps.getLeaf("Bu06").addCreature("Angel")
        aps.getLeaf("Bu11").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu11").addCreature("Ranger")
        aps.getLeaf("Bu12").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bu12").addCreature("Troll")
        aps.getLeaf("Bk02").addCreature("Centaur")
        aps.getLeaf("Gr06").split(2, "Gr05", 10)
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Gorgon")
        aps.getLeaf("Rd07").split(2, "Rd11", 10)
        aps.getLeaf("Rd07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Rd07").addCreature("Gorgon")
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Gorgon")
        aps.getLeaf("Br03").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br03").addCreature("Troll")
        aps.printLeaves()
        turn = 11
        print "\nTurn", turn
        aps.getLeaf("Gd08").split(2, "Gd01", 11)
        aps.getLeaf("Gd03").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gd03").addCreature("Lion")
        aps.getLeaf("Gd08").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Gd08").addCreature("Ranger")
        aps.getLeaf("Bu10").revealCreatures(['Troll'])
        aps.getLeaf("Bu10").addCreature("Troll")
        aps.getLeaf("Bu11").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu11").addCreature("Minotaur")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Guardian")
        aps.getLeaf("Br02").split(2, "Br12", 11)
        aps.getLeaf("Br01").revealCreatures(['Ogre', 'Ogre', 'Ogre'])
        aps.getLeaf("Br01").addCreature("Minotaur")
        aps.getLeaf("Br03").revealCreatures(['Ogre'])
        aps.getLeaf("Br03").addCreature("Ogre")
        aps.getLeaf("Br04").revealCreatures(['Centaur'])
        aps.getLeaf("Br04").addCreature("Centaur")
        aps.getLeaf("Br11").revealCreatures(['Troll'])
        aps.getLeaf("Br11").addCreature("Troll")
        aps.getLeaf("Gd08").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Gd08").addCreature("Warbear")
        aps.getLeaf("Gd11").revealCreatures(['Cyclops'])
        aps.getLeaf("Gd11").addCreature("Cyclops")
        aps.getLeaf("Bu03").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu03").addCreature("Cyclops")
        aps.getLeaf("Bu12").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bu12").addCreature("Troll")
        aps.printLeaves()
        turn = 12
        print "\nTurn", turn
        aps.getLeaf("Bk08").split(2, "Bk10", 12)
        aps.getLeaf("Bk02").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bk02").addCreature("Lion")
        aps.getLeaf("Gr06").split(2, "Gr12", 12)
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Behemoth")
        aps.getLeaf("Rd07").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Rd07").addCreature("Behemoth")
        aps.getLeaf("Br01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br01").addCreature("Troll")
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Gorgon")
        aps.getLeaf("Br03").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br03").addCreature("Troll")
        aps.getLeaf("Br04").revealCreatures(['Lion'])
        aps.getLeaf("Br04").addCreature("Lion")
        aps.getLeaf("Br11").revealCreatures(['Centaur'])
        aps.getLeaf("Br11").addCreature("Centaur")
        aps.getLeaf("Br12").addCreature("Ogre")
        aps.printLeaves()
        turn = 13
        print "\nTurn", turn
        aps.getLeaf("Gd08").split(2, "Gd07", 13)
        aps.getLeaf("Gd10").revealCreatures(['Gorgon'])
        aps.getLeaf("Gd10").addCreature("Gorgon")
        aps.getLeaf("Bu11").split(2, "Bu03", 13)
        aps.getLeaf("Bu12").split(2, "Bu05", 13)
        aps.getLeaf("Bu10").addCreature("Gargoyle")
        aps.getLeaf("Bu11").revealCreatures(['Minotaur'])
        aps.getLeaf("Bu11").addCreature("Minotaur")
        aps.getLeaf("Bu12").revealCreatures(['Minotaur'])
        aps.getLeaf("Bu12").addCreature("Minotaur")
        aps.getLeaf("Bk08").addCreature("Centaur")
        aps.getLeaf("Br11").split(2, "Br10", 13)
        aps.getLeaf("Br01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br01").addCreature("Troll")
        aps.getLeaf("Br03").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br03").addCreature("Ranger")
        aps.getLeaf("Br10").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br10").addCreature("Lion")
        aps.getLeaf("Br11").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Br11").addCreature("Giant")
        aps.getLeaf("Br12").revealCreatures(['Troll'])
        aps.getLeaf("Br12").addCreature("Troll")
        aps.printLeaves()
        turn = 14
        print "\nTurn", turn
        aps.getLeaf("Gd10").split(2, "Gd01", 14)
        aps.getLeaf("Gd08").revealCreatures(['Ranger'])
        aps.getLeaf("Gd08").addCreature("Ranger")
        aps.getLeaf("Bu10").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu10").addCreature("Cyclops")
        aps.getLeaf("Bu12").revealCreatures(['Troll', 'Troll', 'Troll'])
        aps.getLeaf("Bu12").addCreature("Wyvern")
        aps.getLeaf("Bk08").revealCreatures(['Titan'])
        aps.getLeaf("Bk02").revealCreatures(['Lion', 'Centaur', 'Centaur', 'Centaur'])
        aps.getLeaf("Br10").revealCreatures(['Lion', 'Centaur', 'Centaur'])
        aps.getLeaf("Br10").removeCreature("Centaur")
        aps.getLeaf("Br10").removeCreature("Centaur")
        aps.getLeaf("Bk02").removeCreature("Centaur")
        aps.getLeaf("Br10").removeCreature("Lion")
        aps.getLeaf("Bk02").revealCreatures(['Lion', 'Centaur', 'Centaur'])
        aps.getLeaf("Bk01").removeCreature("Angel")
        aps.getLeaf("Bk02").addCreature("Angel")
        aps.getLeaf("Bk02").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bk02").addCreature("Lion")
        aps.getLeaf("Bk08").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk08").addCreature("Cyclops")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Behemoth")
        aps.getLeaf("Rd07").split(2, "Rd12", 14)
        aps.getLeaf("Rd09").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Rd09").addCreature("Ranger")
        aps.getLeaf("Br01").revealCreatures(['Ogre', 'Ogre', 'Ogre'])
        aps.getLeaf("Br01").addCreature("Minotaur")
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Guardian")
        aps.getLeaf("Br03").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br03").addCreature("Warbear")
        aps.getLeaf("Br11").revealCreatures(['Troll'])
        aps.getLeaf("Br11").addCreature("Troll")
        aps.getLeaf("Br12").revealCreatures(['Ogre'])
        aps.getLeaf("Br12").addCreature("Ogre")
        aps.getLeaf("Gd08").addCreature("Angel")
        aps.getLeaf("Gd11").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd11").addCreature("Gorgon")
        aps.printLeaves()
        turn = 15
        print "\nTurn", turn
        aps.getLeaf("Bu12").split(2, "Bu01", 15)
        aps.getLeaf("Bu06").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bu06").addCreature("Ranger")
        aps.getLeaf("Bu11").revealCreatures(['Minotaur', 'Minotaur'])
        aps.getLeaf("Bu11").addCreature("Unicorn")
        aps.getLeaf("Bk08").split(2, "Bk11", 15)
        aps.getLeaf("Bk08").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk08").addCreature("Cyclops")
        aps.getLeaf("Bk10").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk10").addCreature("Cyclops")
        aps.getLeaf("Gr06").revealCreatures(['Titan', 'Guardian', 'Behemoth', 'Behemoth', 'Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Rd09").revealCreatures(['Titan', 'Warlock', 'Cyclops', 'Cyclops', 'Ranger', 'Troll', 'Troll'])
        aps.getLeaf("Rd09").removeCreature("Ranger")
        aps.getLeaf("Rd09").removeCreature("Cyclops")
        aps.getLeaf("Gr06").removeCreature("Behemoth")
        aps.getLeaf("Gr06").removeCreature("Cyclops")
        aps.getLeaf("Rd09").removeCreature("Cyclops")
        aps.getLeaf("Rd09").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Rd09").addCreature("Ranger")
        aps.getLeaf("Rd09").removeCreature("Ranger")
        aps.getLeaf("Gr06").removeCreature("Behemoth")
        aps.getLeaf("Gr06").removeCreature("Cyclops")
        aps.getLeaf("Rd09").removeCreature("Troll")
        aps.getLeaf("Gr06").removeCreature("Guardian")
        aps.getLeaf("Gr06").removeCreature("Cyclops")
        aps.getLeaf("Gr06").removeCreature("Titan")
        aps.getLeaf("Rd09").revealCreatures(['Titan', 'Warlock', 'Troll'])
        aps.getLeaf("Rd09").addCreature("Angel")
        aps.getLeaf("Br01").split(2, "Br09", 15)
        aps.getLeaf("Br02").split(2, "Br10", 15)
        aps.getLeaf("Br11").split(2, "Br04", 15)
        aps.getLeaf("Gd01").addCreature("Ogre")
        aps.getLeaf("Gd10").revealCreatures(['Guardian'])
        aps.getLeaf("Gd10").addCreature("Guardian")
        aps.getLeaf("Bu03").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu03").addCreature("Minotaur")
        aps.getLeaf("Bk08").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk08").addCreature("Cyclops")
        aps.getLeaf("Bk10").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Bk10").addCreature("Gorgon")
        aps.getLeaf("Rd09").revealCreatures(['Titan', 'Angel', 'Warlock', 'Troll'])
        aps.getLeaf("Bk02").revealCreatures(['Angel', 'Lion', 'Lion', 'Centaur', 'Centaur'])
        aps.getLeaf("Bk02").removeCreature("Centaur")
        aps.getLeaf("Rd07").removeCreature("Angel")
        aps.getLeaf("Rd09").addCreature("Angel")
        aps.getLeaf("Bk02").removeCreature("Angel")
        aps.getLeaf("Bk02").removeCreature("Lion")
        aps.getLeaf("Bk02").removeCreature("Centaur")
        aps.getLeaf("Bk02").removeCreature("Lion")
        aps.getLeaf("Rd09").revealCreatures(['Titan', 'Angel', 'Angel', 'Warlock', 'Troll'])
        aps.getLeaf("Rd09").addCreature("Angel")
        aps.getLeaf("Br01").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br01").addCreature("Warbear")
        aps.getLeaf("Br04").revealCreatures(['Troll'])
        aps.getLeaf("Br04").addCreature("Troll")
        aps.getLeaf("Br09").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br09").addCreature("Troll")
        aps.getLeaf("Gd01").revealCreatures(['Gorgon'])
        aps.getLeaf("Gd01").addCreature("Gorgon")
        aps.getLeaf("Gd10").revealCreatures(['Cyclops'])
        aps.getLeaf("Gd10").addCreature("Gargoyle")
        aps.printLeaves()
        turn = 17
        print "\nTurn", turn
        aps.getLeaf("Bu11").split(2, "Bu01", 17)
        aps.getLeaf("Bu10").revealCreatures(['Gargoyle'])
        aps.getLeaf("Bu10").addCreature("Gargoyle")
        aps.getLeaf("Bu11").revealCreatures(['Ranger'])
        aps.getLeaf("Bu11").addCreature("Ranger")
        aps.getLeaf("Bk08").split(2, "Bk11", 17)
        aps.getLeaf("Rd07").revealCreatures(['Behemoth', 'Gorgon', 'Gorgon', 'Cyclops'])
        aps.getLeaf("Gd11").revealCreatures(['Gorgon', 'Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gd11").removeCreature("Gorgon")
        aps.getLeaf("Gd11").removeCreature("Cyclops")
        aps.getLeaf("Rd07").removeCreature("Gorgon")
        aps.getLeaf("Gd11").removeCreature("Cyclops")
        aps.getLeaf("Rd07").removeCreature("Behemoth")
        aps.getLeaf("Rd07").removeCreature("Cyclops")
        aps.getLeaf("Rd09").removeCreature("Angel")
        aps.getLeaf("Rd07").addCreature("Angel")
        aps.getLeaf("Gd11").revealCreatures(['Cyclops'])
        aps.getLeaf("Gd11").addCreature("Gargoyle")
        aps.getLeaf("Gd11").removeCreature("Cyclops")
        aps.getLeaf("Gd11").removeCreature("Gargoyle")
        aps.getLeaf("Rd07").removeCreature("Gorgon")
        aps.getLeaf("Rd07").revealCreatures(['Angel'])
        aps.getLeaf("Rd07").addCreature("Angel")
        aps.getLeaf("Br03").split(2, "Br12", 17)
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Behemoth")
        aps.getLeaf("Br09").revealCreatures(['Ogre'])
        aps.getLeaf("Br09").addCreature("Ogre")
        aps.printLeaves()
        turn = 18
        print "\nTurn", turn
        aps.getLeaf("Gd10").split(2, "Gd02", 18)
        aps.getLeaf("Gd01").revealCreatures(['Ogre'])
        aps.getLeaf("Gd01").addCreature("Ogre")
        aps.getLeaf("Bu01").revealCreatures(['Ranger'])
        aps.getLeaf("Bu01").addCreature("Ranger")
        aps.getLeaf("Bu10").revealCreatures(['Gargoyle'])
        aps.getLeaf("Bu10").addCreature("Gargoyle")
        aps.getLeaf("Bu11").revealCreatures(['Minotaur', 'Minotaur'])
        aps.getLeaf("Bu11").addCreature("Unicorn")
        aps.getLeaf("Bk08").revealCreatures(['Cyclops'])
        aps.getLeaf("Bk08").addCreature("Gargoyle")
        aps.getLeaf("Rd07").revealCreatures(['Angel', 'Angel'])
        aps.getLeaf("Br10").revealCreatures(['Gorgon', 'Gorgon'])
        aps.getLeaf("Br10").removeCreature("Gorgon")
        aps.getLeaf("Br10").removeCreature("Gorgon")
        aps.getLeaf("Rd07").removeCreature("Angel")
        aps.getLeaf("Rd07").revealCreatures(['Angel'])
        aps.getLeaf("Br01").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br01").addCreature("Ranger")
        aps.getLeaf("Br04").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br04").addCreature("Ranger")
        aps.getLeaf("Br11").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Br11").addCreature("Unicorn")
        aps.getLeaf("Gd01").revealCreatures(['Gorgon'])
        aps.getLeaf("Gd01").addCreature("Gorgon")
        aps.getLeaf("Gd10").revealCreatures(['Behemoth', 'Behemoth'])
        aps.getLeaf("Gd10").addCreature("Serpent")
        aps.getLeaf("Bu01").revealCreatures(['Lion'])
        aps.getLeaf("Bu01").addCreature("Lion")
        aps.printLeaves()
        turn = 19
        print "\nTurn", turn
        aps.getLeaf("Br01").split(2, "Br10", 19)
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Gorgon")
        aps.getLeaf("Br03").revealCreatures(['Warbear'])
        aps.getLeaf("Br03").addCreature("Warbear")
        aps.getLeaf("Br04").revealCreatures(['Troll', 'Troll', 'Troll'])
        aps.getLeaf("Br04").addCreature("Guardian")
        aps.getLeaf("Gd10").revealCreatures(['Serpent', 'Angel', 'Guardian', 'Behemoth', 'Behemoth', 'Cyclops'])
        aps.getLeaf("Rd09").revealCreatures(['Titan', 'Angel', 'Angel', 'Warlock', 'Troll'])
        aps.getLeaf("Gd10").removeCreature("Angel")
        aps.getLeaf("Rd09").removeCreature("Angel")
        aps.getLeaf("Gd10").removeCreature("Guardian")
        aps.getLeaf("Gd08").removeCreature("Angel")
        aps.getLeaf("Gd10").addCreature("Angel")
        aps.getLeaf("Rd09").removeCreature("Angel")
        aps.getLeaf("Rd09").removeCreature("Troll")
        aps.getLeaf("Rd09").removeCreature("Warlock")
        aps.getLeaf("Gd10").removeCreature("Behemoth")
        aps.getLeaf("Gd10").removeCreature("Angel")
        aps.getLeaf("Rd09").removeCreature("Titan")
        aps.getLeaf("Gd10").removeCreature("Cyclops")
        aps.getLeaf("Gd10").revealCreatures(['Serpent', 'Behemoth'])
        aps.getLeaf("Gd10").addCreature("Angel")
        aps.getLeaf("Gd10").revealCreatures(['Serpent'])
        aps.getLeaf("Gd10").addCreature("Serpent")
        aps.printLeaves()
        turn = 20
        print "\nTurn", turn
        aps.getLeaf("Bu06").split(2, "Bu07", 20)
        aps.getLeaf("Bu11").split(2, "Bu02", 20)
        aps.getLeaf("Bu01").revealCreatures(['Ranger'])
        aps.getLeaf("Bu01").addCreature("Ranger")
        aps.getLeaf("Bu02").revealCreatures(['Ranger'])
        aps.getLeaf("Bu02").addCreature("Ranger")
        aps.getLeaf("Bu12").revealCreatures(['Troll', 'Troll', 'Troll'])
        aps.getLeaf("Bu12").addCreature("Wyvern")
        aps.getLeaf("Br02").split(2, "Br05", 20)
        aps.getLeaf("Br01").addCreature("Angel")
        aps.getLeaf("Br01").revealCreatures(['Ranger'])
        aps.getLeaf("Br01").addCreature("Ranger")
        aps.getLeaf("Br02").revealCreatures(['Gorgon'])
        aps.getLeaf("Br02").addCreature("Gorgon")
        aps.getLeaf("Br03").revealCreatures(['Ranger'])
        aps.getLeaf("Br03").addCreature("Ranger")
        aps.getLeaf("Br04").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br04").addCreature("Ranger")
        aps.getLeaf("Bu06").revealCreatures(['Ranger'])
        aps.getLeaf("Bu06").addCreature("Ranger")
        aps.printLeaves()
        turn = 21
        print "\nTurn", turn
        aps.getLeaf("Br01").split(2, "Br05", 21)
        aps.getLeaf("Br01").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br01").addCreature("Ranger")
        aps.getLeaf("Gd01").revealCreatures(['Gorgon'])
        aps.getLeaf("Gd01").addCreature("Gorgon")
        aps.printLeaves()
        turn = 22
        print "\nTurn", turn
        aps.getLeaf("Bu10").split(2, "Bu04", 22)
        aps.getLeaf("Bu01").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu01").addCreature("Ranger")
        aps.getLeaf("Bu12").revealCreatures(['Troll', 'Troll', 'Troll'])
        aps.getLeaf("Bu12").addCreature("Guardian")
        aps.getLeaf("Bk10").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Bk10").addCreature("Gorgon")
        aps.getLeaf("Br03").split(2, "Br06", 22)
        aps.getLeaf("Br01").revealCreatures(['Ranger'])
        aps.getLeaf("Br01").addCreature("Ranger")
        aps.getLeaf("Br03").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br03").addCreature("Ranger")
        aps.getLeaf("Br05").revealCreatures(['Ranger'])
        aps.getLeaf("Br05").addCreature("Ranger")
        aps.getLeaf("Br11").revealCreatures(['Unicorn'])
        aps.getLeaf("Br11").addCreature("Unicorn")
        aps.printLeaves()
        turn = 23
        print "\nTurn", turn
        aps.getLeaf("Gd01").split(2, "Gd02", 23)
        aps.getLeaf("Bu12").split(2, "Bu02", 23)
        aps.getLeaf("Bu01").revealCreatures(['Lion'])
        aps.getLeaf("Bu01").addCreature("Lion")
        aps.getLeaf("Bu06").revealCreatures(['Ranger'])
        aps.getLeaf("Bu06").addCreature("Ranger")
        aps.getLeaf("Bu07").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bu07").addCreature("Ranger")
        aps.getLeaf("Bu11").revealCreatures(['Unicorn'])
        aps.getLeaf("Bu11").addCreature("Unicorn")
        aps.getLeaf("Br01").split(2, "Br07", 23)
        aps.getLeaf("Br11").split(2, "Br12", 23)
        aps.getLeaf("Br01").revealCreatures(['Minotaur'])
        aps.getLeaf("Br01").addCreature("Minotaur")
        aps.getLeaf("Br02").revealCreatures(['Behemoth'])
        aps.getLeaf("Br02").addCreature("Behemoth")
        aps.getLeaf("Br03").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Br03").addCreature("Giant")
        aps.getLeaf("Br05").revealCreatures(['Ranger'])
        aps.getLeaf("Br05").addCreature("Ranger")
        aps.getLeaf("Br07").revealCreatures(['Ranger'])
        aps.getLeaf("Br07").addCreature("Ranger")
        aps.getLeaf("Br11").revealCreatures(['Unicorn'])
        aps.getLeaf("Br11").addCreature("Unicorn")
        aps.printLeaves()
        turn = 24
        print "\nTurn", turn
        aps.getLeaf("Bu01").split(2, "Bu08", 24)
        aps.getLeaf("Bu07").revealCreatures(['Ranger'])
        aps.getLeaf("Bu07").addCreature("Lion")
        aps.getLeaf("Bu11").revealCreatures(['Minotaur', 'Minotaur'])
        aps.getLeaf("Bu11").addCreature("Unicorn")
        aps.getLeaf("Bk08").revealCreatures(['Titan'])
        aps.getLeaf("Bk08").addCreature("Warlock")
        aps.getLeaf("Br03").split(2, "Br09", 24)
        aps.getLeaf("Br05").revealCreatures(['Ranger'])
        aps.getLeaf("Br05").addCreature("Lion")
        aps.printLeaves()

    def testPredictSplits7(self):
        print "\ntest 7 begins"
        creatures = []
        creatures.append(CreatureInfo('Angel', True, True))
        creatures.append(CreatureInfo('Gargoyle', True, True))
        creatures.append(CreatureInfo('Centaur', True, True))
        creatures.append(CreatureInfo('Centaur', False, True))
        creatures.append(CreatureInfo('Centaur', True, False))
        n = Node("Gd10", 1, creatures, None)
        n.revealCreatures(['Gargoyle', 'Gargoyle'])
        print n
        assert(n.allCertain())
        print "\ntest 7 ends"


    def testPredictSplits8(self):
        print "\ntest 8 begins"
        ps = PredictSplits("Bk", "Bk10", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bk10").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Gd", "Gd07", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gd07").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Br", "Br02", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Br02").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Rd", "Rd09", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Rd09").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Bu", "Bu03", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bu03").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Gr", "Gr07", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gr07").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.printLeaves()
        turn = 1
        print "\nTurn", turn
        aps.getLeaf("Bk10").split(4, "Bk01", 1)
        aps.getLeaf("Bk01").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk01").addCreature("Cyclops")
        aps.getLeaf("Bk10").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk10").addCreature("Troll")
        aps.getLeaf("Gd07").split(4, "Gd01", 1)
        aps.getLeaf("Gd01").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gd01").addCreature("Lion")
        aps.getLeaf("Gd07").revealCreatures(['Gargoyle'])
        aps.getLeaf("Gd07").addCreature("Gargoyle")
        aps.getLeaf("Br02").split(4, "Br06", 1)
        aps.getLeaf("Br02").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br02").addCreature("Cyclops")
        aps.getLeaf("Br06").revealCreatures(['Ogre'])
        aps.getLeaf("Br06").addCreature("Ogre")
        aps.getLeaf("Rd09").split(4, "Rd07", 1)
        aps.getLeaf("Rd07").revealCreatures(['Ogre'])
        aps.getLeaf("Rd07").addCreature("Ogre")
        aps.getLeaf("Rd09").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Rd09").addCreature("Lion")
        aps.getLeaf("Bu03").split(4, "Bu07", 1)
        aps.getLeaf("Bu07").revealCreatures(['Centaur'])
        aps.getLeaf("Bu07").addCreature("Centaur")
        aps.getLeaf("Gr07").split(4, "Gr01", 1)
        aps.getLeaf("Gr07").revealCreatures(['Titan'])
        aps.getLeaf("Gr01").revealCreatures(['Centaur'])
        aps.getLeaf("Gr01").addCreature("Centaur")
        aps.getLeaf("Gr07").revealCreatures(['Titan'])
        aps.getLeaf("Gr07").addCreature("Warlock")
        aps.getLeaf("Bk10").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk10").addCreature("Troll")
        aps.getLeaf("Gd01").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gd01").addCreature("Lion")
        aps.getLeaf("Gd07").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd07").addCreature("Cyclops")
        aps.getLeaf("Br02").revealCreatures(['Centaur'])
        aps.getLeaf("Br02").addCreature("Centaur")
        aps.getLeaf("Br06").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br06").addCreature("Troll")
        aps.getLeaf("Rd09").revealCreatures(['Titan'])
        aps.getLeaf("Rd09").addCreature("Warlock")
        aps.getLeaf("Bu03").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bu03").addCreature("Troll")
        aps.getLeaf("Gr07").revealCreatures(['Titan'])
        aps.getLeaf("Gr07").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr07").addCreature("Cyclops")
        aps.getLeaf("Bk01").revealCreatures(['Centaur'])
        aps.getLeaf("Bk01").addCreature("Centaur")
        aps.getLeaf("Bk10").revealCreatures(['Centaur'])
        aps.getLeaf("Bk10").addCreature("Centaur")
        aps.getLeaf("Gd01").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Gd01").addCreature("Minotaur")
        aps.getLeaf("Gd07").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd07").addCreature("Cyclops")
        aps.getLeaf("Br02").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br02").addCreature("Cyclops")
        aps.getLeaf("Br06").revealCreatures(['Ogre', 'Ogre', 'Ogre'])
        aps.getLeaf("Br06").addCreature("Minotaur")
        aps.getLeaf("Rd07").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd07").addCreature("Cyclops")
        aps.getLeaf("Rd09").revealCreatures(['Ogre'])
        aps.getLeaf("Rd09").addCreature("Ogre")
        aps.getLeaf("Bu03").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bu03").addCreature("Troll")
        aps.printLeaves()
        turn = 4
        print "\nTurn", turn
        aps.getLeaf("Bk10").split(2, "Bk04", 4)
        aps.getLeaf("Bk01").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk01").addCreature("Cyclops")
        aps.getLeaf("Bk10").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bk10").addCreature("Warbear")
        aps.getLeaf("Gd07").split(2, "Gd02", 4)
        aps.getLeaf("Gd07").revealCreatures(['Cyclops'])
        aps.getLeaf("Gd07").addCreature("Cyclops")
        aps.getLeaf("Br02").split(2, "Br11", 4)
        aps.getLeaf("Br02").revealCreatures(['Centaur'])
        aps.getLeaf("Br02").addCreature("Centaur")
        aps.getLeaf("Rd09").split(2, "Rd01", 4)
        aps.getLeaf("Rd09").revealCreatures(['Lion'])
        aps.getLeaf("Rd09").addCreature("Lion")
        aps.getLeaf("Bu03").revealCreatures(['Gargoyle'])
        aps.getLeaf("Bu03").addCreature("Gargoyle")
        aps.getLeaf("Bu07").revealCreatures(['Gargoyle'])
        aps.getLeaf("Bu07").addCreature("Gargoyle")
        aps.getLeaf("Gr07").revealCreatures(['Ogre'])
        aps.getLeaf("Gr07").addCreature("Ogre")
        aps.getLeaf("Bk04").revealCreatures(['Centaur'])
        aps.getLeaf("Bk04").addCreature("Centaur")
        aps.getLeaf("Gd01").revealCreatures(['Angel', 'Minotaur', 'Lion', 'Lion', 'Gargoyle', 'Centaur', 'Centaur'])
        aps.getLeaf("Rd09").revealCreatures(['Titan', 'Warlock', 'Lion', 'Lion', 'Ogre', 'Ogre'])
        aps.getLeaf("Rd09").removeCreature("Ogre")
        aps.getLeaf("Gd01").removeCreature("Centaur")
        aps.getLeaf("Rd09").removeCreature("Warlock")
        aps.getLeaf("Gd01").removeCreature("Minotaur")
        aps.getLeaf("Rd09").removeCreature("Ogre")
        aps.getLeaf("Gd01").removeCreature("Gargoyle")
        aps.getLeaf("Rd09").removeCreature("Lion")
        aps.getLeaf("Rd09").removeCreature("Lion")
        aps.getLeaf("Gd01").removeCreature("Angel")
        aps.getLeaf("Gd01").removeCreature("Lion")
        aps.getLeaf("Rd09").removeCreature("Titan")
        aps.getLeaf("Gd01").removeCreature("Lion")
        aps.getLeaf("Gd01").revealCreatures(['Centaur'])
        aps.getLeaf("Gd07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd07").addCreature("Gorgon")
        aps.printLeaves()
        turn = 5
        print "\nTurn", turn
        aps.getLeaf("Br06").split(2, "Br12", 5)
        aps.getLeaf("Br02").revealCreatures(['Cyclops'])
        aps.getLeaf("Br02").addCreature("Cyclops")
        aps.getLeaf("Bu03").split(2, "Bu04", 5)
        aps.getLeaf("Bu04").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu04").addCreature("Cyclops")
        aps.getLeaf("Gr07").split(2, "Gr06", 5)
        aps.getLeaf("Gr07").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr07").addCreature("Cyclops")
        aps.getLeaf("Bk01").revealCreatures(['Angel', 'Cyclops', 'Cyclops', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur'])
        aps.getLeaf("Gr01").revealCreatures(['Angel', 'Centaur', 'Centaur', 'Centaur', 'Ogre'])
        aps.getLeaf("Gr01").removeCreature("Centaur")
        aps.getLeaf("Gr01").removeCreature("Ogre")
        aps.getLeaf("Bk01").removeCreature("Centaur")
        aps.getLeaf("Gr01").removeCreature("Centaur")
        aps.getLeaf("Bk01").removeCreature("Angel")
        aps.getLeaf("Bk01").removeCreature("Cyclops")
        aps.getLeaf("Gr01").removeCreature("Centaur")
        aps.getLeaf("Gr01").removeCreature("Angel")
        aps.getLeaf("Bk01").removeCreature("Centaur")
        aps.getLeaf("Bk01").revealCreatures(['Cyclops', 'Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk04").revealCreatures(['Centaur', 'Centaur', 'Centaur'])
        aps.getLeaf("Bk04").addCreature("Warbear")
        aps.getLeaf("Bk10").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk10").addCreature("Troll")
        aps.printLeaves()
        turn = 6
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd03", 6)
        aps.getLeaf("Br02").split(2, "Br08", 6)
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Behemoth")
        aps.getLeaf("Br08").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br08").addCreature("Lion")
        aps.getLeaf("Bu03").revealCreatures(['Ogre'])
        aps.getLeaf("Bu03").addCreature("Ogre")
        aps.getLeaf("Bu07").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu07").addCreature("Cyclops")
        aps.printLeaves()
        turn = 7
        print "\nTurn", turn
        aps.getLeaf("Bk10").split(2, "Bk02", 7)
        aps.getLeaf("Bk01").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk01").addCreature("Cyclops")
        aps.getLeaf("Gd07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd07").addCreature("Gorgon")
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Behemoth")
        aps.getLeaf("Br06").revealCreatures(['Centaur'])
        aps.getLeaf("Br06").addCreature("Centaur")
        aps.getLeaf("Bu07").split(2, "Bu05", 7)
        aps.getLeaf("Bu03").revealCreatures(['Titan'])
        aps.getLeaf("Bu03").addCreature("Warlock")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops'])
        aps.getLeaf("Bu07").addCreature("Cyclops")
        aps.getLeaf("Gr06").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.getLeaf("Gr07").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr07").addCreature("Cyclops")
        aps.getLeaf("Bk02").revealCreatures(['Ogre'])
        aps.getLeaf("Bk02").addCreature("Ogre")
        aps.getLeaf("Bk10").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bk10").addCreature("Warbear")
        aps.getLeaf("Gd03").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gd03").addCreature("Troll")
        aps.printLeaves()
        turn = 8
        print "\nTurn", turn
        aps.getLeaf("Br02").split(2, "Br07", 8)
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Gorgon")
        aps.getLeaf("Br06").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br06").addCreature("Lion")
        aps.getLeaf("Bu03").revealCreatures(['Titan'])
        aps.getLeaf("Bu04").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu04").addCreature("Cyclops")
        aps.getLeaf("Gr07").split(2, "Gr01", 8)
        aps.getLeaf("Gr06").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.getLeaf("Bk10").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bk10").addCreature("Ranger")
        aps.getLeaf("Gd07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd07").addCreature("Gorgon")
        aps.printLeaves()
        turn = 9
        print "\nTurn", turn
        aps.getLeaf("Br06").split(2, "Br03", 9)
        aps.getLeaf("Br06").revealCreatures(['Troll'])
        aps.getLeaf("Br06").addCreature("Troll")
        aps.getLeaf("Br07").revealCreatures(['Cyclops'])
        aps.getLeaf("Br07").addCreature("Cyclops")
        aps.getLeaf("Br08").revealCreatures(['Centaur'])
        aps.getLeaf("Br08").addCreature("Centaur")
        aps.getLeaf("Br11").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br11").addCreature("Cyclops")
        aps.getLeaf("Bu03").split(2, "Bu09", 9)
        aps.getLeaf("Bu03").revealCreatures(['Troll'])
        aps.getLeaf("Bu03").addCreature("Troll")
        aps.getLeaf("Bu09").revealCreatures(['Ogre'])
        aps.getLeaf("Bu09").addCreature("Ogre")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops', 'Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br07").revealCreatures(['Cyclops', 'Cyclops', 'Centaur'])
        aps.getLeaf("Br07").removeCreature("Cyclops")
        aps.getLeaf("Br07").removeCreature("Cyclops")
        aps.getLeaf("Br07").removeCreature("Centaur")
        aps.getLeaf("Gr06").removeCreature("Cyclops")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.getLeaf("Gr07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gr07").addCreature("Gorgon")
        aps.getLeaf("Bk02").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk02").addCreature("Troll")
        aps.printLeaves()
        turn = 10
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd06", 10)
        aps.getLeaf("Gd07").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gd07").addCreature("Behemoth")
        aps.getLeaf("Br03").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br03").addCreature("Lion")
        aps.getLeaf("Br06").revealCreatures(['Minotaur'])
        aps.getLeaf("Br06").addCreature("Minotaur")
        aps.getLeaf("Br11").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br11").addCreature("Cyclops")
        aps.getLeaf("Bu04").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu04").addCreature("Cyclops")
        aps.getLeaf("Gr07").addCreature("Angel")
        aps.printLeaves()
        turn = 11
        print "\nTurn", turn
        aps.getLeaf("Bk10").split(2, "Bk01", 11)
        aps.getLeaf("Bk10").addCreature("Angel")
        aps.getLeaf("Bk10").revealCreatures(['Ranger'])
        aps.getLeaf("Bk10").addCreature("Ranger")
        aps.getLeaf("Gd07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd07").addCreature("Gorgon")
        aps.getLeaf("Br06").split(2, "Br07", 11)
        aps.getLeaf("Bu03").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bu03").addCreature("Ranger")
        aps.getLeaf("Bu04").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bu04").addCreature("Behemoth")
        aps.getLeaf("Gr06").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr06").addCreature("Cyclops")
        aps.printLeaves()
        turn = 12
        print "\nTurn", turn
        aps.getLeaf("Bk10").split(2, "Bk09", 12)
        aps.getLeaf("Bk02").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk02").addCreature("Troll")
        aps.getLeaf("Bk09").revealCreatures(['Ranger'])
        aps.getLeaf("Bk09").addCreature("Ranger")
        aps.getLeaf("Bk10").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Bk10").addCreature("Unicorn")
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Gorgon")
        aps.getLeaf("Br06").revealCreatures(['Lion'])
        aps.getLeaf("Br06").addCreature("Lion")
        aps.getLeaf("Br07").revealCreatures(['Ogre'])
        aps.getLeaf("Br07").addCreature("Ogre")
        aps.getLeaf("Bu03").split(2, "Bu01", 12)
        aps.getLeaf("Bu01").revealCreatures(['Ogre'])
        aps.getLeaf("Bu01").addCreature("Ogre")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops'])
        aps.getLeaf("Bu07").addCreature("Cyclops")
        aps.getLeaf("Bk01").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bk01").addCreature("Warbear")
        aps.printLeaves()
        turn = 13
        print "\nTurn", turn
        aps.getLeaf("Br02").split(2, "Br10", 13)
        aps.getLeaf("Br02").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").addCreature("Gorgon")
        aps.getLeaf("Br06").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Br06").addCreature("Warbear")
        aps.getLeaf("Bu03").revealCreatures(['Ranger'])
        aps.getLeaf("Bu03").addCreature("Ranger")
        aps.getLeaf("Bu04").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Bu04").addCreature("Gorgon")
        aps.getLeaf("Bk02").revealCreatures(['Troll'])
        aps.getLeaf("Bk02").addCreature("Troll")
        aps.printLeaves()
        turn = 14
        print "\nTurn", turn
        aps.getLeaf("Br06").split(2, "Br04", 14)
        aps.getLeaf("Bu07").split(2, "Bu02", 14)
        aps.getLeaf("Bu03").revealCreatures(['Ranger'])
        aps.getLeaf("Bu03").addCreature("Ranger")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Guardian")
        aps.printLeaves()
        turn = 15
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd06", 15)
        aps.getLeaf("Gd07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd07").addCreature("Gorgon")
        aps.getLeaf("Br06").revealCreatures(['Warbear'])
        aps.getLeaf("Br06").addCreature("Warbear")
        aps.getLeaf("Bu03").split(2, "Bu05", 15)
        aps.getLeaf("Bu02").revealCreatures(['Centaur'])
        aps.getLeaf("Bu02").addCreature("Centaur")
        aps.getLeaf("Bu03").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bu03").addCreature("Warbear")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Bu07").addCreature("Gorgon")
        aps.getLeaf("Gr07").split(2, "Gr01", 15)
        aps.getLeaf("Gr07").revealCreatures(['Titan'])
        aps.getLeaf("Gr06").revealCreatures(['Guardian', 'Cyclops', 'Cyclops', 'Cyclops', 'Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br02").revealCreatures(['Titan', 'Behemoth', 'Behemoth', 'Gorgon', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Br02").removeCreature("Behemoth")
        aps.getLeaf("Gr06").removeCreature("Gargoyle")
        aps.getLeaf("Gr07").removeCreature("Angel")
        aps.getLeaf("Gr06").addCreature("Angel")
        aps.getLeaf("Br02").removeCreature("Gorgon")
        aps.getLeaf("Gr06").removeCreature("Gargoyle")
        aps.getLeaf("Br02").removeCreature("Cyclops")
        aps.getLeaf("Gr06").removeCreature("Guardian")
        aps.getLeaf("Br02").removeCreature("Titan")
        aps.getLeaf("Gr06").removeCreature("Angel")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Angel")
        aps.getLeaf("Gr07").revealCreatures(['Titan'])
        aps.getLeaf("Gr07").addCreature("Warlock")
        aps.getLeaf("Bk02").revealCreatures(['Ogre', 'Ogre', 'Ogre'])
        aps.getLeaf("Bk02").addCreature("Minotaur")
        aps.getLeaf("Gd07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd07").addCreature("Gorgon")
        aps.getLeaf("Bu04").revealCreatures(['Behemoth', 'Gorgon', 'Cyclops', 'Cyclops', 'Cyclops', 'Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk10").revealCreatures(['Titan', 'Angel', 'Unicorn', 'Warbear', 'Warbear', 'Troll'])
        aps.getLeaf("Bk10").removeCreature("Warbear")
        aps.getLeaf("Bk10").removeCreature("Warbear")
        aps.getLeaf("Bu04").removeCreature("Gorgon")
        aps.getLeaf("Bu07").removeCreature("Angel")
        aps.getLeaf("Bu04").addCreature("Angel")
        aps.getLeaf("Bk10").removeCreature("Angel")
        aps.getLeaf("Bk10").removeCreature("Unicorn")
        aps.getLeaf("Bu04").removeCreature("Gargoyle")
        aps.getLeaf("Bu04").removeCreature("Cyclops")
        aps.getLeaf("Bu04").removeCreature("Angel")
        aps.getLeaf("Bk10").removeCreature("Titan")
        aps.getLeaf("Bk10").removeCreature("Troll")
        aps.getLeaf("Bu04").removeCreature("Behemoth")
        aps.getLeaf("Bu04").revealCreatures(['Cyclops', 'Cyclops', 'Gargoyle'])
        aps.getLeaf("Bu04").addCreature("Angel")
        aps.getLeaf("Bu02").revealCreatures(['Centaur', 'Centaur', 'Centaur'])
        aps.getLeaf("Bu02").addCreature("Warbear")
        aps.getLeaf("Bu04").revealCreatures(['Cyclops'])
        aps.getLeaf("Bu04").addCreature("Cyclops")
        aps.getLeaf("Bu05").revealCreatures(['Ranger'])
        aps.getLeaf("Bu05").addCreature("Ranger")
        aps.getLeaf("Bu07").revealCreatures(['Centaur'])
        aps.getLeaf("Bu07").addCreature("Centaur")
        aps.getLeaf("Gr01").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr01").addCreature("Cyclops")
        aps.getLeaf("Gr07").revealCreatures(['Gorgon'])
        aps.getLeaf("Gr07").addCreature("Gorgon")
        aps.printLeaves()
        turn = 17
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd11", 17)
        aps.getLeaf("Gd07").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gd07").addCreature("Behemoth")
        aps.getLeaf("Bu01").revealCreatures(['Troll'])
        aps.getLeaf("Bu01").addCreature("Troll")
        aps.getLeaf("Bu02").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bu02").addCreature("Lion")
        aps.getLeaf("Bu03").revealCreatures(['Troll'])
        aps.getLeaf("Bu03").addCreature("Troll")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Bu07").addCreature("Gorgon")
        aps.getLeaf("Gr01").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gr01").addCreature("Gorgon")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Behemoth")
        aps.getLeaf("Gd06").revealCreatures(['Gorgon'])
        aps.getLeaf("Gd06").addCreature("Gorgon")
        aps.printLeaves()
        turn = 18
        print "\nTurn", turn
        aps.getLeaf("Bu03").split(2, "Bu09", 18)
        aps.getLeaf("Bu07").split(2, "Bu12", 18)
        aps.getLeaf("Bu03").revealCreatures(['Ranger'])
        aps.getLeaf("Bu03").addCreature("Ranger")
        aps.getLeaf("Bu04").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bu04").addCreature("Behemoth")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bu07").addCreature("Behemoth")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Gorgon")
        aps.getLeaf("Gr07").revealCreatures(['Gorgon'])
        aps.getLeaf("Gr07").addCreature("Gorgon")
        aps.getLeaf("Gd06").addCreature("Angel")
        aps.getLeaf("Gd11").revealCreatures(['Gorgon'])
        aps.getLeaf("Gd11").addCreature("Gorgon")
        aps.getLeaf("Bu03").revealCreatures(['Warbear'])
        aps.getLeaf("Bu03").addCreature("Warbear")
        aps.getLeaf("Bu05").revealCreatures(['Ranger'])
        aps.getLeaf("Bu05").addCreature("Ranger")
        aps.getLeaf("Gr01").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gr01").addCreature("Gorgon")
        aps.printLeaves()
        turn = 20
        print "\nTurn", turn
        aps.getLeaf("Bu03").split(2, "Bu06", 20)
        aps.getLeaf("Bu01").revealCreatures(['Troll'])
        aps.getLeaf("Bu01").addCreature("Troll")
        aps.getLeaf("Bu02").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bu02").addCreature("Lion")
        aps.getLeaf("Bu03").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Bu03").addCreature("Unicorn")
        aps.getLeaf("Bu04").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bu04").addCreature("Behemoth")
        aps.getLeaf("Gr07").revealCreatures(['Titan', 'Warlock', 'Warlock', 'Gorgon', 'Gorgon', 'Gorgon', 'Cyclops'])
        aps.getLeaf("Bu02").revealCreatures(['Warbear', 'Lion', 'Lion', 'Centaur', 'Centaur', 'Centaur'])
        aps.getLeaf("Bu02").removeCreature("Lion")
        aps.getLeaf("Bu02").removeCreature("Centaur")
        aps.getLeaf("Bu02").removeCreature("Warbear")
        aps.getLeaf("Gr07").removeCreature("Gorgon")
        aps.getLeaf("Gr06").removeCreature("Angel")
        aps.getLeaf("Gr07").addCreature("Angel")
        aps.getLeaf("Bu02").removeCreature("Lion")
        aps.getLeaf("Bu02").removeCreature("Centaur")
        aps.getLeaf("Gr07").removeCreature("Gorgon")
        aps.getLeaf("Bu02").removeCreature("Centaur")
        aps.getLeaf("Gr07").revealCreatures(['Titan', 'Angel', 'Warlock', 'Warlock', 'Gorgon', 'Cyclops'])
        aps.getLeaf("Gr07").addCreature("Angel")
        aps.getLeaf("Gr01").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr01").addCreature("Behemoth")
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Gorgon")
        aps.getLeaf("Gd06").revealCreatures(['Gorgon'])
        aps.getLeaf("Gd06").addCreature("Gorgon")
        aps.getLeaf("Gd07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd07").addCreature("Gorgon")
        aps.printLeaves()
        turn = 21
        print "\nTurn", turn
        aps.getLeaf("Bu04").split(2, "Bu11", 21)
        aps.getLeaf("Bu04").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Bu04").addCreature("Gorgon")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Bu07").addCreature("Gorgon")
        aps.getLeaf("Gr07").split(2, "Gr08", 21)
        aps.getLeaf("Gr01").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr01").addCreature("Gargoyle")
        aps.printLeaves()
        turn = 22
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd10", 22)
        aps.getLeaf("Gd07").revealCreatures(['Behemoth', 'Behemoth'])
        aps.getLeaf("Gd07").addCreature("Serpent")
        aps.getLeaf("Gd11").revealCreatures(['Gorgon'])
        aps.getLeaf("Gd11").addCreature("Gargoyle")
        aps.getLeaf("Bu07").split(2, "Bu10", 22)
        aps.getLeaf("Bu04").revealCreatures(['Cyclops'])
        aps.getLeaf("Bu04").addCreature("Gargoyle")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bu07").addCreature("Behemoth")
        aps.getLeaf("Gr01").split(2, "Gr04", 22)
        aps.getLeaf("Gr06").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr06").addCreature("Behemoth")
        aps.getLeaf("Gr08").revealCreatures(['Cyclops'])
        aps.getLeaf("Gr08").addCreature("Gargoyle")
        aps.getLeaf("Gd11").revealCreatures(['Gargoyle'])
        aps.getLeaf("Gd11").addCreature("Gargoyle")
        aps.printLeaves()
        turn = 23
        print "\nTurn", turn
        aps.getLeaf("Bu04").split(2, "Bu09", 23)
        aps.getLeaf("Bu03").revealCreatures(['Troll'])
        aps.getLeaf("Bu03").addCreature("Troll")
        aps.getLeaf("Bu04").revealCreatures(['Behemoth', 'Behemoth'])
        aps.getLeaf("Bu04").addCreature("Serpent")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops'])
        aps.getLeaf("Bu07").addCreature("Gargoyle")
        aps.getLeaf("Gd07").revealCreatures(['Behemoth', 'Behemoth'])
        aps.getLeaf("Gd07").addCreature("Serpent")
        aps.getLeaf("Bu07").revealCreatures(['Behemoth', 'Behemoth', 'Gorgon', 'Cyclops', 'Cyclops', 'Cyclops', 'Gargoyle'])
        aps.getLeaf("Gr01").revealCreatures(['Behemoth', 'Gorgon', 'Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Gr01").removeCreature("Gorgon")
        aps.getLeaf("Gr01").removeCreature("Cyclops")
        aps.getLeaf("Gr01").removeCreature("Cyclops")
        aps.getLeaf("Bu07").removeCreature("Behemoth")
        aps.getLeaf("Bu07").removeCreature("Behemoth")
        aps.getLeaf("Bu04").removeCreature("Angel")
        aps.getLeaf("Bu07").addCreature("Angel")
        aps.getLeaf("Gr01").removeCreature("Behemoth")
        aps.getLeaf("Bu07").removeCreature("Angel")
        aps.getLeaf("Gr01").removeCreature("Cyclops")
        aps.getLeaf("Bu07").removeCreature("Gorgon")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops', 'Gargoyle'])
        aps.getLeaf("Bu07").addCreature("Angel")
        aps.printLeaves()
        turn = 24
        print "\nTurn", turn
        aps.getLeaf("Gr06").split(2, "Gr01", 24)
        aps.getLeaf("Gr07").revealCreatures(['Titan'])
        aps.getLeaf("Gr07").addCreature("Warlock")
        aps.printLeaves()
        turn = 25
        print "\nTurn", turn
        aps.getLeaf("Bu03").split(2, "Bu12", 25)
        aps.getLeaf("Gd10").addCreature("Ogre")
        aps.getLeaf("Bu04").revealCreatures(['Serpent', 'Behemoth', 'Behemoth', 'Gorgon', 'Cyclops'])
        aps.getLeaf("Gd11").revealCreatures(['Gorgon', 'Gorgon', 'Gorgon', 'Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd11").removeCreature("Gargoyle")
        aps.getLeaf("Bu07").removeCreature("Angel")
        aps.getLeaf("Bu04").addCreature("Angel")
        aps.getLeaf("Gd11").removeCreature("Gorgon")
        aps.getLeaf("Gd11").removeCreature("Gorgon")
        aps.getLeaf("Bu04").removeCreature("Behemoth")
        aps.getLeaf("Gd11").removeCreature("Gorgon")
        aps.getLeaf("Gd11").removeCreature("Gargoyle")
        aps.getLeaf("Bu04").removeCreature("Behemoth")
        aps.getLeaf("Bu04").revealCreatures(['Serpent', 'Angel', 'Gorgon', 'Cyclops'])
        aps.getLeaf("Bu04").addCreature("Angel")
        aps.getLeaf("Bu04").revealCreatures(['Serpent'])
        aps.getLeaf("Bu04").addCreature("Serpent")
        aps.getLeaf("Gr01").revealCreatures(['Gorgon', 'Gorgon'])
        aps.getLeaf("Bu12").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Gr01").removeCreature("Gorgon")
        aps.getLeaf("Bu12").removeCreature("Troll")
        aps.getLeaf("Gr07").removeCreature("Angel")
        aps.getLeaf("Gr01").addCreature("Angel")
        aps.getLeaf("Gr01").removeCreature("Gorgon")
        aps.getLeaf("Bu12").revealCreatures(['Troll'])
        aps.getLeaf("Bu12").addCreature("Troll")
        aps.getLeaf("Bu12").removeCreature("Troll")
        aps.getLeaf("Bu12").removeCreature("Troll")
        aps.getLeaf("Gr01").removeCreature("Angel")
        aps.getLeaf("Gd07").revealCreatures(['Titan', 'Serpent', 'Serpent', 'Behemoth', 'Behemoth', 'Gorgon', 'Cyclops'])
        aps.getLeaf("Gr07").revealCreatures(['Titan', 'Angel', 'Warlock', 'Warlock', 'Warlock'])
        aps.getLeaf("Gr07").removeCreature("Warlock")
        aps.getLeaf("Gr07").removeCreature("Warlock")
        aps.getLeaf("Gr07").removeCreature("Angel")
        aps.getLeaf("Gd07").removeCreature("Behemoth")
        aps.getLeaf("Gr07").removeCreature("Warlock")
        aps.getLeaf("Gd07").removeCreature("Cyclops")
        aps.getLeaf("Gd07").removeCreature("Behemoth")
        aps.getLeaf("Gd07").removeCreature("Gorgon")
        aps.getLeaf("Gr07").removeCreature("Titan")
        aps.getLeaf("Gd07").revealCreatures(['Titan', 'Serpent', 'Serpent'])
        aps.getLeaf("Gd07").addCreature("Angel")
        aps.getLeaf("Gd06").removeCreature("Angel")
        aps.getLeaf("Gd07").addCreature("Angel")
        aps.getLeaf("Gd10").revealCreatures(['Ogre'])
        aps.getLeaf("Gd10").addCreature("Ogre")
        aps.getLeaf("Bu03").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Bu03").addCreature("Unicorn")
        aps.getLeaf("Bu06").revealCreatures(['Ranger'])
        aps.getLeaf("Bu06").addCreature("Ranger")
        aps.getLeaf("Bu09").addCreature("Ogre")
        aps.getLeaf("Bu10").addCreature("Ogre")
        aps.getLeaf("Gd07").addCreature("Angel")
        aps.getLeaf("Gd10").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gd10").addCreature("Troll")
        aps.getLeaf("Bu03").revealCreatures(['Titan'])
        aps.getLeaf("Bu03").revealCreatures(['Titan'])
        aps.getLeaf("Bu03").addCreature("Warlock")
        aps.getLeaf("Bu04").addCreature("Centaur")
        aps.getLeaf("Bu07").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bu07").addCreature("Behemoth")
        aps.printLeaves()
        turn = 31
        print "\nTurn", turn
        aps.getLeaf("Bu03").split(2, "Bu08", 31)
        aps.getLeaf("Bu03").revealCreatures(['Titan'])
        aps.getLeaf("Bu03").addCreature("Archangel")
        aps.getLeaf("Bu08").revealCreatures(['Warbear', 'Warbear'])
        aps.getLeaf("Bu08").addCreature("Unicorn")
        aps.printLeaves()
        turn = 33
        print "\nTurn", turn
        aps.getLeaf("Bu04").split(2, "Bu01", 33)
        aps.getLeaf("Gd07").revealCreatures(['Titan'])
        aps.getLeaf("Gd07").addCreature("Warlock")
        aps.printLeaves()
        turn = 35
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd05", 35)
        aps.getLeaf("Bu08").revealCreatures(['Unicorn'])
        aps.getLeaf("Bu08").addCreature("Unicorn")
        aps.getLeaf("Bu08").revealCreatures(['Unicorn'])
        aps.getLeaf("Bu08").addCreature("Unicorn")
        aps.getLeaf("Gd07").revealCreatures(['Titan'])
        aps.getLeaf("Gd07").addCreature("Warlock")
        aps.getLeaf("Bu03").revealCreatures(['Titan'])
        aps.getLeaf("Bu03").revealCreatures(['Titan', 'Archangel', 'Unicorn', 'Unicorn', 'Warlock', 'Warlock'])
        aps.getLeaf("Gd05").revealCreatures(['Angel', 'Warlock'])
        aps.getLeaf("Gd05").removeCreature("Angel")
        aps.getLeaf("Gd05").removeCreature("Warlock")
        aps.getLeaf("Bu03").revealCreatures(['Titan', 'Archangel', 'Unicorn', 'Unicorn', 'Warlock', 'Warlock'])
        aps.getLeaf("Bu04").removeCreature("Angel")
        aps.getLeaf("Bu03").addCreature("Angel")
        aps.getLeaf("Bu01").revealCreatures(['Centaur'])
        aps.getLeaf("Bu01").addCreature("Centaur")
        aps.getLeaf("Bu04").revealCreatures(['Serpent'])
        aps.getLeaf("Bu04").addCreature("Serpent")
        aps.getLeaf("Gd07").revealCreatures(['Titan'])
        aps.getLeaf("Gd07").revealCreatures(['Serpent'])
        aps.getLeaf("Gd07").addCreature("Serpent")
        aps.printLeaves()
        turn = 38
        print "\nTurn", turn
        aps.getLeaf("Bu03").split(2, "Bu07", 38)
        aps.printLeaves()
        turn = 39
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd06", 39)
        aps.getLeaf("Bu01").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bu01").addCreature("Lion")
        aps.getLeaf("Bu03").revealCreatures(['Unicorn'])
        aps.getLeaf("Bu03").addCreature("Unicorn")
        aps.getLeaf("Gd07").revealCreatures(['Titan'])
        aps.getLeaf("Gd07").addCreature("Archangel")
        aps.getLeaf("Bu03").revealCreatures(['Titan'])
        aps.getLeaf("Bu03").revealCreatures(['Titan', 'Archangel', 'Angel', 'Unicorn', 'Unicorn', 'Unicorn'])
        aps.getLeaf("Gd06").revealCreatures(['Angel', 'Warlock'])
        aps.getLeaf("Gd06").removeCreature("Angel")
        aps.getLeaf("Bu04").removeCreature("Angel")
        aps.getLeaf("Bu03").addCreature("Angel")
        aps.getLeaf("Gd06").removeCreature("Warlock")
        aps.getLeaf("Bu03").revealCreatures(['Titan', 'Archangel', 'Angel', 'Angel', 'Unicorn', 'Unicorn', 'Unicorn'])
        aps.getLeaf("Bu01").revealCreatures(['Lion'])
        aps.getLeaf("Bu01").addCreature("Lion")
        aps.getLeaf("Bu01").revealCreatures(['Lion'])
        aps.getLeaf("Bu01").addCreature("Lion")
        aps.getLeaf("Bu01").revealCreatures(['Lion', 'Lion', 'Lion'])
        aps.getLeaf("Bu01").addCreature("Griffon")
        aps.getLeaf("Gd07").revealCreatures(['Serpent'])
        aps.getLeaf("Gd07").addCreature("Serpent")
        aps.printLeaves()
        turn = 44
        print "\nTurn", turn
        aps.getLeaf("Bu01").split(2, "Bu11", 44)
        aps.printLeaves()
        turn = 45
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd10", 45)
        aps.getLeaf("Bu01").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu01").addCreature("Ranger")
        aps.getLeaf("Bu01").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu01").addCreature("Ranger")
        aps.printLeaves()
        turn = 47
        print "\nTurn", turn
        aps.getLeaf("Bu01").split(2, "Bu10", 47)
        aps.getLeaf("Bu01").revealCreatures(['Ranger'])
        aps.getLeaf("Bu01").addCreature("Ranger")
        aps.getLeaf("Bu10").revealCreatures(['Lion'])
        aps.getLeaf("Bu10").addCreature("Lion")
        aps.getLeaf("Gd10").revealCreatures(['Serpent'])
        aps.getLeaf("Gd10").addCreature("Serpent")
        aps.getLeaf("Bu10").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu10").addCreature("Ranger")
        aps.getLeaf("Bu03").revealCreatures(['Titan'])
        aps.getLeaf("Bu03").revealCreatures(['Titan', 'Archangel', 'Angel', 'Angel', 'Unicorn', 'Unicorn', 'Unicorn'])
        aps.getLeaf("Gd10").revealCreatures(['Serpent', 'Serpent', 'Angel'])
        aps.getLeaf("Bu03").removeCreature("Angel")
        aps.getLeaf("Gd10").removeCreature("Serpent")
        aps.getLeaf("Gd10").removeCreature("Angel")
        aps.getLeaf("Bu03").removeCreature("Archangel")
        aps.getLeaf("Gd10").revealCreatures(['Serpent'])
        aps.getLeaf("Gd10").addCreature("Serpent")
        aps.getLeaf("Bu03").removeCreature("Angel")
        aps.getLeaf("Gd10").removeCreature("Serpent")
        aps.getLeaf("Bu03").removeCreature("Unicorn")
        aps.getLeaf("Gd10").removeCreature("Serpent")
        aps.getLeaf("Bu03").removeCreature("Unicorn")
        aps.getLeaf("Bu03").revealCreatures(['Titan', 'Unicorn'])
        aps.getLeaf("Bu03").addCreature("Angel")
        aps.getLeaf("Bu01").revealCreatures(['Griffon'])
        aps.getLeaf("Bu01").addCreature("Griffon")
        aps.getLeaf("Bu10").revealCreatures(['Lion', 'Lion', 'Lion'])
        aps.getLeaf("Bu10").addCreature("Griffon")
        aps.getLeaf("Bu11").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bu11").addCreature("Lion")
        aps.getLeaf("Gd07").revealCreatures(['Titan'])
        aps.getLeaf("Gd07").addCreature("Warlock")
        aps.printLeaves()
        turn = 50
        print "\nTurn", turn
        aps.getLeaf("Bu01").split(2, "Bu02", 50)
        aps.getLeaf("Bu01").revealCreatures(['Ranger'])
        aps.getLeaf("Bu01").addCreature("Ranger")
        aps.getLeaf("Bu10").revealCreatures(['Lion', 'Lion', 'Lion'])
        aps.getLeaf("Bu10").addCreature("Griffon")
        aps.getLeaf("Bu11").revealCreatures(['Lion'])
        aps.getLeaf("Bu11").addCreature("Lion")
        aps.getLeaf("Bu01").revealCreatures(['Griffon', 'Griffon'])
        aps.getLeaf("Bu01").addCreature("Hydra")
        aps.getLeaf("Bu03").revealCreatures(['Unicorn'])
        aps.getLeaf("Bu03").addCreature("Unicorn")
        aps.getLeaf("Bu10").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu10").addCreature("Ranger")
        aps.getLeaf("Bu11").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bu11").addCreature("Lion")
        aps.getLeaf("Gd07").addCreature("Angel")
        aps.printLeaves()
        turn = 52
        print "\nTurn", turn
        aps.getLeaf("Bu10").split(2, "Bu05", 52)
        aps.getLeaf("Bu02").revealCreatures(['Ranger'])
        aps.getLeaf("Bu02").addCreature("Ranger")
        aps.getLeaf("Bu03").revealCreatures(['Unicorn'])
        aps.getLeaf("Bu03").addCreature("Unicorn")
        aps.getLeaf("Bu04").revealCreatures(['Serpent'])
        aps.getLeaf("Bu04").addCreature("Serpent")
        aps.printLeaves()
        turn = 53
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd03", 53)
        aps.getLeaf("Gd07").revealCreatures(['Titan'])
        aps.getLeaf("Gd03").revealCreatures(['Serpent'])
        aps.getLeaf("Gd03").addCreature("Behemoth")
        aps.getLeaf("Gd07").revealCreatures(['Serpent'])
        aps.getLeaf("Gd07").addCreature("Behemoth")
        aps.getLeaf("Bu01").split(2, "Bu09", 53)
        aps.getLeaf("Bu02").revealCreatures(['Ranger'])
        aps.getLeaf("Bu02").addCreature("Ranger")
        aps.getLeaf("Bu03").revealCreatures(['Unicorn'])
        aps.getLeaf("Bu03").addCreature("Unicorn")
        aps.getLeaf("Bu10").revealCreatures(['Ranger'])
        aps.getLeaf("Bu10").addCreature("Ranger")
        aps.getLeaf("Bu02").revealCreatures(['Lion'])
        aps.getLeaf("Bu02").addCreature("Lion")
        aps.getLeaf("Bu09").revealCreatures(['Ranger'])
        aps.getLeaf("Bu09").addCreature("Ranger")
        aps.getLeaf("Bu10").revealCreatures(['Griffon', 'Griffon'])
        aps.getLeaf("Bu10").addCreature("Hydra")
        aps.printLeaves()
        turn = 55
        print "\nTurn", turn
        aps.getLeaf("Bu10").split(2, "Bu07", 55)
        aps.getLeaf("Bu01").revealCreatures(['Griffon', 'Griffon'])
        aps.getLeaf("Bu01").addCreature("Hydra")
        aps.getLeaf("Bu01").revealCreatures(['Griffon', 'Griffon'])
        aps.getLeaf("Bu01").addCreature("Hydra")
        aps.getLeaf("Bu07").revealCreatures(['Lion'])
        aps.getLeaf("Bu07").addCreature("Lion")
        aps.getLeaf("Bu09").revealCreatures(['Ranger'])
        aps.getLeaf("Bu09").addCreature("Ranger")
        aps.getLeaf("Gd07").revealCreatures(['Titan'])
        aps.getLeaf("Gd07").addCreature("Warlock")
        aps.printLeaves()
        turn = 57
        print "\nTurn", turn
        aps.getLeaf("Bu01").split(2, "Bu12", 57)
        aps.getLeaf("Bu04").revealCreatures(['Serpent'])
        aps.getLeaf("Bu04").addCreature("Behemoth")
        aps.getLeaf("Bu10").revealCreatures(['Ranger'])
        aps.getLeaf("Bu10").addCreature("Ranger")
        aps.printLeaves()
        turn = 58
        print "\nTurn", turn
        aps.getLeaf("Gd07").split(2, "Gd10", 58)
        aps.getLeaf("Bu10").revealCreatures(['Ranger'])
        aps.getLeaf("Bu10").addCreature("Ranger")
        aps.getLeaf("Gd07").revealCreatures(['Titan'])
        aps.getLeaf("Gd07").revealCreatures(['Serpent'])
        aps.getLeaf("Gd07").addCreature("Behemoth")
        aps.printLeaves()
        turn = 59
        print "\nTurn", turn
        aps.getLeaf("Bu10").split(2, "Bu12", 59)
        aps.getLeaf("Bu09").revealCreatures(['Ranger'])
        aps.getLeaf("Bu09").addCreature("Ranger")
        aps.getLeaf("Bu10").revealCreatures(['Ranger'])
        aps.getLeaf("Bu10").addCreature("Ranger")
        aps.getLeaf("Bu03").revealCreatures(['Titan'])
        aps.getLeaf("Bu03").addCreature("Angel")
        aps.getLeaf("Bu01").revealCreatures(['Hydra'])
        aps.getLeaf("Bu01").addCreature("Hydra")
        aps.getLeaf("Bu04").revealCreatures(['Behemoth'])
        aps.getLeaf("Bu04").addCreature("Behemoth")
        aps.getLeaf("Bu07").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Bu07").addCreature("Ranger")
        aps.getLeaf("Bu10").revealCreatures(['Griffon', 'Griffon'])
        aps.getLeaf("Bu10").addCreature("Hydra")
        aps.printLeaves()
        turn = 61
        print "\nTurn", turn
        aps.getLeaf("Bu10").split(2, "Bu02", 61)
        aps.getLeaf("Bu07").revealCreatures(['Lion'])
        aps.getLeaf("Bu07").addCreature("Lion")
        aps.getLeaf("Bu09").revealCreatures(['Ranger'])
        aps.getLeaf("Bu09").addCreature("Ranger")
        aps.getLeaf("Bu01").revealCreatures(['Hydra'])
        aps.getLeaf("Bu01").addCreature("Hydra")
        aps.getLeaf("Bu07").revealCreatures(['Ranger'])
        aps.getLeaf("Bu07").addCreature("Ranger")
        aps.getLeaf("Bu10").revealCreatures(['Ranger'])
        aps.getLeaf("Bu10").addCreature("Ranger")
        aps.printLeaves()


    def testPredictSplits9(self):
        print "\ntest 9 begins"
        ps = PredictSplits("Gr", "Gr01", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gr01").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Br", "Br04", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Br04").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Rd", "Rd12", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Rd12").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Bu", "Bu11", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bu11").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Bk", "Bk06", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bk06").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Gd", "Gd03", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gd03").revealCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.printLeaves()
        turn = 1
        print "\nTurn", turn
        aps.getLeaf("Gr01").split(4, "Gr11", 1)
        aps.getLeaf("Gr11").revealCreatures(['Titan'])
        aps.getLeaf("Gr01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gr01").addCreature("Troll")
        aps.getLeaf("Gr11").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr11").addCreature("Cyclops")
        aps.getLeaf("Br04").split(4, "Br11", 1)
        aps.getLeaf("Br04").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br04").addCreature("Lion")
        aps.getLeaf("Br11").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br11").addCreature("Troll")
        aps.getLeaf("Rd12").split(4, "Rd07", 1)
        aps.getLeaf("Rd12").revealCreatures(['Centaur'])
        aps.getLeaf("Rd12").addCreature("Centaur")
        aps.getLeaf("Bu11").split(4, "Bu10", 1)
        aps.getLeaf("Bu10").revealCreatures(['Ogre'])
        aps.getLeaf("Bu10").addCreature("Ogre")
        aps.getLeaf("Bu11").revealCreatures(['Centaur'])
        aps.getLeaf("Bu11").addCreature("Centaur")
        aps.getLeaf("Bk06").split(4, "Bk11", 1)
        aps.getLeaf("Bk06").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk06").addCreature("Troll")
        aps.getLeaf("Bk11").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bk11").addCreature("Lion")
        aps.getLeaf("Gd03").split(4, "Gd11", 1)
        aps.getLeaf("Gd03").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gd03").addCreature("Lion")
        aps.getLeaf("Gd11").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd11").addCreature("Cyclops")
        aps.getLeaf("Gr01").revealCreatures(['Centaur'])
        aps.getLeaf("Gr01").addCreature("Centaur")
        aps.getLeaf("Gr11").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr11").addCreature("Cyclops")
        aps.getLeaf("Br04").revealCreatures(['Gargoyle'])
        aps.getLeaf("Br04").addCreature("Gargoyle")
        aps.getLeaf("Rd07").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Rd07").addCreature("Troll")
        aps.getLeaf("Bu10").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu10").addCreature("Cyclops")
        aps.getLeaf("Bk06").revealCreatures(['Ogre'])
        aps.getLeaf("Bk06").addCreature("Ogre")
        aps.getLeaf("Bk11").revealCreatures(['Gargoyle'])
        aps.getLeaf("Bk11").addCreature("Gargoyle")
        aps.getLeaf("Gd03").revealCreatures(['Lion'])
        aps.getLeaf("Gd03").addCreature("Lion")
        aps.getLeaf("Gd11").revealCreatures(['Ogre'])
        aps.getLeaf("Gd11").addCreature("Ogre")
        aps.getLeaf("Gr01").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gr01").addCreature("Lion")
        aps.getLeaf("Rd12").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd12").addCreature("Cyclops")
        aps.getLeaf("Rd07").revealCreatures(['Troll'])
        aps.getLeaf("Rd07").addCreature("Troll")
        aps.getLeaf("Bu10").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu10").addCreature("Cyclops")
        aps.getLeaf("Bk06").revealCreatures(['Gargoyle'])
        aps.getLeaf("Bk06").addCreature("Gargoyle")
        aps.getLeaf("Gd11").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd11").addCreature("Cyclops")
        aps.printLeaves()
        turn = 4
        print "\nTurn", turn
        aps.getLeaf("Gr01").split(2, "Gr10", 4)
        aps.getLeaf("Gr01").revealCreatures(['Troll'])
        aps.getLeaf("Gr01").addCreature("Troll")
        aps.getLeaf("Br04").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br04").addCreature("Cyclops")
        aps.getLeaf("Br11").revealCreatures(['Gargoyle'])
        aps.getLeaf("Br11").addCreature("Gargoyle")
        aps.getLeaf("Rd12").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Rd12").addCreature("Lion")
        aps.getLeaf("Rd07").revealCreatures(['Centaur'])
        aps.getLeaf("Rd07").addCreature("Centaur")
        aps.getLeaf("Bu10").split(2, "Bu09", 4)
        aps.getLeaf("Bu10").revealCreatures(['Cyclops'])
        aps.getLeaf("Bu10").addCreature("Cyclops")
        aps.getLeaf("Bk06").split(2, "Bk10", 4)
        aps.getLeaf("Bk06").revealCreatures(['Troll'])
        aps.getLeaf("Bk06").addCreature("Troll")
        aps.getLeaf("Bk11").revealCreatures(['Lion'])
        aps.getLeaf("Bk11").addCreature("Lion")
        aps.getLeaf("Gd03").revealCreatures(['Angel', 'Lion', 'Lion', 'Centaur', 'Centaur', 'Ogre'])
        aps.getLeaf("Bk11").revealCreatures(['Angel', 'Lion', 'Lion', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur'])
        aps.getLeaf("Bk11").removeCreature("Gargoyle")
        aps.getLeaf("Bk11").removeCreature("Gargoyle")
        aps.getLeaf("Bk11").removeCreature("Centaur")
        aps.getLeaf("Bk11").removeCreature("Lion")
        aps.getLeaf("Bk11").removeCreature("Centaur")
        aps.getLeaf("Gd03").removeCreature("Angel")
        aps.getLeaf("Gd03").removeCreature("Lion")
        aps.getLeaf("Bk11").removeCreature("Angel")
        aps.getLeaf("Bk11").removeCreature("Lion")
        aps.getLeaf("Gd03").removeCreature("Centaur")
        aps.getLeaf("Gd03").removeCreature("Ogre")
        aps.getLeaf("Gd03").addCreature("Angel")
        aps.getLeaf("Gd03").revealCreatures(['Angel', 'Lion', 'Centaur'])
        aps.getLeaf("Gd03").revealCreatures(['Lion'])
        aps.getLeaf("Gd03").addCreature("Lion")
        aps.printLeaves()
        turn = 5
        print "\nTurn", turn
        aps.getLeaf("Br04").split(2, "Br10", 5)
        aps.getLeaf("Br11").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br11").addCreature("Cyclops")
        aps.getLeaf("Rd07").split(2, "Rd09", 5)
        aps.getLeaf("Rd12").split(2, "Rd04", 5)
        aps.getLeaf("Bu10").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bu10").addCreature("Behemoth")
        aps.getLeaf("Gd11").split(2, "Gd12", 5)
        aps.getLeaf("Gd03").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Gd03").addCreature("Ranger")
        aps.getLeaf("Gd11").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gd11").addCreature("Troll")
        aps.getLeaf("Gr01").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gr01").addCreature("Troll")
        aps.getLeaf("Br11").revealCreatures(['Titan', 'Cyclops', 'Troll', 'Gargoyle', 'Gargoyle', 'Ogre', 'Ogre'])
        aps.getLeaf("Bu11").revealCreatures(['Angel', 'Centaur', 'Centaur', 'Centaur', 'Ogre'])
        aps.getLeaf("Br11").removeCreature("Gargoyle")
        aps.getLeaf("Br11").removeCreature("Cyclops")
        aps.getLeaf("Bu11").removeCreature("Angel")
        aps.getLeaf("Br04").removeCreature("Angel")
        aps.getLeaf("Br11").addCreature("Angel")
        aps.getLeaf("Bu11").removeCreature("Centaur")
        aps.getLeaf("Bu11").removeCreature("Centaur")
        aps.getLeaf("Br11").removeCreature("Angel")
        aps.getLeaf("Bu11").removeCreature("Ogre")
        aps.getLeaf("Bu11").removeCreature("Centaur")
        aps.getLeaf("Br11").removeCreature("Gargoyle")
        aps.getLeaf("Br11").revealCreatures(['Titan', 'Troll', 'Ogre', 'Ogre'])
        aps.getLeaf("Br10").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Br10").addCreature("Cyclops")
        aps.getLeaf("Rd09").revealCreatures(['Ogre'])
        aps.getLeaf("Rd09").addCreature("Ogre")
        aps.getLeaf("Rd12").revealCreatures(['Lion'])
        aps.getLeaf("Rd12").addCreature("Lion")
        aps.printLeaves()
        turn = 6
        print "\nTurn", turn
        aps.getLeaf("Bu10").split(2, "Bu04", 6)
        aps.getLeaf("Bu09").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bu09").addCreature("Cyclops")
        aps.getLeaf("Bk06").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Bk06").addCreature("Ranger")
        aps.getLeaf("Gd11").revealCreatures(['Cyclops'])
        aps.getLeaf("Gd11").addCreature("Cyclops")
        aps.printLeaves()
        turn = 7
        print "\nTurn", turn
        aps.getLeaf("Gr01").split(2, "Gr02", 7)
        aps.getLeaf("Gr01").revealCreatures(['Lion'])
        aps.getLeaf("Gr01").addCreature("Lion")
        aps.getLeaf("Br04").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br04").addCreature("Lion")
        aps.getLeaf("Rd04").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd04").addCreature("Cyclops")
        aps.getLeaf("Rd07").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Rd07").addCreature("Ranger")
        aps.getLeaf("Bu04").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bu04").addCreature("Troll")
        aps.getLeaf("Bk06").split(2, "Bk03", 7)
        aps.getLeaf("Bk06").revealCreatures(['Titan'])
        aps.getLeaf("Bk06").addCreature("Warlock")
        aps.getLeaf("Gd03").revealCreatures(['Centaur'])
        aps.getLeaf("Gd03").addCreature("Centaur")
        aps.getLeaf("Gr01").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Gr01").addCreature("Ranger")
        aps.getLeaf("Rd04").addCreature("Centaur")
        aps.getLeaf("Rd07").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Rd07").addCreature("Warbear")
        aps.getLeaf("Rd12").revealCreatures(['Cyclops'])
        aps.getLeaf("Rd12").addCreature("Cyclops")
        aps.getLeaf("Bk03").revealCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk03").addCreature("Troll")
        aps.getLeaf("Bk06").revealCreatures(['Ranger'])
        aps.getLeaf("Bk06").addCreature("Ranger")
        aps.printLeaves()
        turn = 8
        print "\nTurn", turn
        aps.getLeaf("Gd11").split(2, "Gd02", 8)
        aps.getLeaf("Gd03").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gd03").addCreature("Lion")
        aps.getLeaf("Gd12").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd12").addCreature("Cyclops")
        aps.printLeaves()
        turn = 9
        print "\nTurn", turn
        aps.getLeaf("Gr01").split(2, "Gr09", 9)
        aps.getLeaf("Gr01").revealCreatures(['Troll', 'Troll', 'Troll'])
        aps.getLeaf("Gr01").addCreature("Guardian")
        aps.getLeaf("Br04").addCreature("Angel")
        aps.getLeaf("Br04").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br04").addCreature("Lion")
        aps.getLeaf("Rd12").split(2, "Rd03", 9)
        aps.getLeaf("Rd03").revealCreatures(['Centaur'])
        aps.getLeaf("Rd03").addCreature("Centaur")
        aps.getLeaf("Rd04").revealCreatures(['Cyclops'])
        aps.getLeaf("Rd04").addCreature("Cyclops")
        aps.getLeaf("Bu10").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bu10").addCreature("Behemoth")
        aps.getLeaf("Bk06").split(2, "Bk09", 9)
        aps.getLeaf("Bk03").revealCreatures(['Troll'])
        aps.getLeaf("Bk03").addCreature("Troll")
        aps.getLeaf("Gd11").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Gd11").addCreature("Gorgon")
        aps.getLeaf("Gr01").revealCreatures(['Ranger'])
        aps.getLeaf("Gr01").addCreature("Ranger")
        aps.getLeaf("Gr11").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr11").addCreature("Cyclops")
        aps.printLeaves()
        turn = 10
        print "\nTurn", turn
        aps.getLeaf("Br04").split(2, "Br02", 10)
        aps.getLeaf("Br02").revealCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br02").addCreature("Lion")
        aps.getLeaf("Br04").revealCreatures(['Cyclops'])
        aps.getLeaf("Br04").addCreature("Cyclops")
        aps.getLeaf("Rd07").split(2, "Rd01", 10)
        aps.getLeaf("Rd07").revealCreatures(['Ranger'])
        aps.getLeaf("Rd07").addCreature("Ranger")
        aps.getLeaf("Rd12").revealCreatures(['Cyclops'])
        aps.getLeaf("Rd12").addCreature("Cyclops")
        aps.getLeaf("Rd04").revealCreatures(['Cyclops'])
        aps.getLeaf("Rd04").addCreature("Cyclops")
        aps.getLeaf("Bu10").revealCreatures(['Titan', 'Behemoth', 'Behemoth', 'Cyclops', 'Cyclops', 'Cyclops'])
        aps.getLeaf("Bk06").revealCreatures(['Titan', 'Warlock', 'Ranger', 'Troll', 'Troll'])
        aps.getLeaf("Bk06").removeCreature("Ranger")
        aps.getLeaf("Bu10").removeCreature("Behemoth")
        aps.getLeaf("Bk06").removeCreature("Warlock")
        aps.getLeaf("Bk06").removeCreature("Troll")
        aps.getLeaf("Bu10").removeCreature("Cyclops")
        aps.getLeaf("Bu10").removeCreature("Cyclops")
        aps.getLeaf("Bk06").removeCreature("Titan")
        aps.getLeaf("Bu10").revealCreatures(['Titan', 'Behemoth', 'Cyclops'])
        aps.getLeaf("Bu10").addCreature("Angel")
        aps.getLeaf("Gd03").split(2, "Gd02", 10)
        aps.getLeaf("Gd03").revealCreatures(['Ranger'])
        aps.getLeaf("Gd03").addCreature("Ranger")
        aps.getLeaf("Gd11").revealCreatures(['Troll'])
        aps.getLeaf("Gd11").addCreature("Troll")
        aps.printLeaves()

        turn = 11
        print "\nTurn", turn
        aps.getLeaf("Gr01").split(2, "Gr04", 11)
        aps.getLeaf("Gr01").merge(aps.getLeaf("Gr04"), turn)
        aps.getLeaf("Gr01").split(5, "Gr04", 11)

        aps.getLeaf("Gr11").split(5, "Gr08", 11)
        aps.getLeaf("Gr08").merge(aps.getLeaf("Gr11"), turn)
        aps.getLeaf("Gr11").split(3, "Gr08", 11)
        aps.getLeaf("Gr11").merge(aps.getLeaf("Gr08"), turn)
        aps.getLeaf("Gr11").split(5, "Gr08", 11)
        aps.getLeaf("Gr11").merge(aps.getLeaf("Gr08"), turn)
        aps.getLeaf("Gr11").split(2, "Gr08", 11)

        aps.getLeaf("Gr04").revealCreatures(['Troll', 'Troll'])
        aps.getLeaf("Gr04").addCreature("Warbear")
        aps.getLeaf("Gr11").revealCreatures(['Centaur'])
        aps.getLeaf("Gr11").addCreature("Centaur")

        aps.getLeaf("Bu10").revealCreatures(['Behemoth'])
        aps.getLeaf("Bu10").addCreature("Behemoth")
        aps.getLeaf("Gd11").split(2, "Gd09", 11)

        aps.getLeaf("Gr08").revealCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gr08").addCreature("Cyclops")
        aps.getLeaf("Gr09").revealCreatures(['Lion'])
        aps.getLeaf("Gr09").addCreature("Lion")

        aps.getLeaf("Br04").revealCreatures(['Lion', 'Lion'])
        aps.getLeaf("Br04").addCreature("Ranger")
        aps.getLeaf("Rd04").revealCreatures(['Cyclops', 'Cyclops'])
        aps.getLeaf("Rd04").addCreature("Gorgon")
        aps.getLeaf("Rd01").revealCreatures(['Centaur'])
        aps.getLeaf("Rd01").addCreature("Centaur")
        aps.printLeaves()

if __name__ == "__main__":
    unittest.main()
