#!/usr/bin/env python2

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
                Rd01 attacks and is revealed: Tit, Wlk, Wlk, Cyc, Tro, Gar, Gar
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
        ps.getLeaf("Rd10").revealCreatures(["Titan", "Ranger", "Troll", 
            "Troll", "Gargoyle", "Ogre", "Ogre"])
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
        ps.getLeaf("Rd08").removeCreatures(["Lion", "Lion", "Centaur", 
                "Centaur"])
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
        print "test 7 ends"


if __name__ == "__main__":
    unittest.main()
