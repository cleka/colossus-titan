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
    """Return 1 if list big is a superset of list little."""
    for el in little:
        if big.count(el) < little.count(el):
            return 0
    return 1

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
       Return 1 if found, 0 if not."""
    for ci in li:
        if ci.name == creatureName:
            li.remove(ci)
            return 1
    return 0

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
    for ci in li:
        if not ci.certain:
            li.remove(ci)
            li.reverse()
            return
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
        """Return true if all creatures are certain."""
        for ci in self.creatures:
            if not ci.certain:
                return 0
        return 1

    def setAllCertain(self):
        """Set all creatures to certain."""
        for ci in self.creatures:
            ci.certain = 1

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


    def revealSomeCreatures(self, li):
        """li is a list of creature names"""
        print "revealSomeCreatures() for", self, li

        li2 = []
        for name in li:
            li2.append(CreatureInfo(name, 1, 1))

        # Use a copy rather than the original so we can remove
        # creatures as we check for multiples.
        dupe = copy.deepcopy(li2)

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
            err = ("Certainty error in Node.revealSomeCreatures() count=%d "
                  "height=%d") % (count, self.getHeight())
            raise RuntimeError, err

        # Then mark passed creatures as certain and then
        # communicate this to the parent, to adjust other legions.

        if len(self.getCertain()) == self.getHeight():
            # No need -- we already know everything.
            return

        dupe = copy.deepcopy(li2)
        count = 0
        for ci in dupe:
            ci.certain = 1
            ci.atSplit = 1   # If not atSplit, would be certain.
            if numCreature(self.creatures, ci.name) < numCreature(dupe,
                    ci.name):
                self.creatures.append(ci)
                count += 1

        # Ensure that the creatures in li are now marked as certain
        dupe = copy.deepcopy(li2)
        certain = self.getCertain()
        for ci in certain:
            if ci in dupe:
                dupe.remove(ci)
        for ci in dupe:
            for ci2 in self.creatures:
                if not ci2.certain and ci2.name == ci.name:
                    ci2.certain = 1
                    break

        # Need to remove count uncertain creatures.
        for unused in range(count):
            removeLastUncertainCreature(self.creatures)
        print "self is", self
        self.parent.tellChildContents(self)

    def revealAllCreatures(self, li):
        """li is a list of creature names"""
        li2 = []
        for name in li:
            li2.append(CreatureInfo(name, 1, 1))

        # Confirm that all creatures that were certain are there.
        certain = self.getCertain()
        for ci in certain:
            if numCreature(li2, ci.name) < numCreature(certain, ci.name):
                raise (RuntimeError,
                        "Certainty error in Node.revealAllCreatures()")
        self.revealSomeCreatures(li)

    def childCreaturesMatch(self):
        """Return true if creatures in children are consistent with self."""
        if self.child1 == None:
            return 1
        all = (self.child1.getAtSplitCreatures() + self.child1.removed +
                self.child2.getAtSplitCreatures() + self.child2.removed)
        for ci in all:
            if numCreature(all, ci.name) != numCreature(
                    self.creatures, ci.name):
                return 0
        return 1


    def isLegalInitialSplitoff(self):
        if self.getHeight() != 4:
            return 0
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
            raise RuntimeError, "Known creatures not in parent legion"

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


    def chooseCreaturesToSplitOut(self, childSize, knownKeep, knownSplit):
        """Decide how to split this legion, and return a list of Creatures to
        remove.  Return null on error."""
        print "chooseCreaturesToSplitOut() for", self, childSize, \
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
            raise RuntimeError, "Known creatures not in parent legion"

        clones = copy.deepcopy(creatures)

        # Move the weaker creatures to the end of the legion.
        clones.sort(creatureComp)

        # Move known splitoffs to the end of the legion.
        for ci in knownSplit:
            clones.remove(ci)
            clones.append(ci)

        # Move known non-splitoffs to the beginning
        temp = []  # Needed because there is no removeLast
        for ci in knownKeep:
            clones.remove(ci)
            temp.append(ci)
        for ci in temp:
            clones.insert(0, ci)

        # If initial split, move angel or titan to the end
        if self.getHeight() == 8:
            if ("Titan" in getCreatureNames(knownSplit) or
                    "Angel" in getCreatureNames(knownKeep)):
                # Maintain flags.
                lord = getCreatureInfo(clones, "Titan")
            else:
                lord = getCreatureInfo(clones, "Angel")
            clones.remove(lord)
            clones.append(lord)

        creaturesToRemove = []
        for unused in range(childSize):
            last = clones.pop()
            last.atSplit = 1
            creaturesToRemove.append(last)
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

        splitoffCreatures = self.chooseCreaturesToSplitOut(childSize,
            knownKeep, knownSplit)
        print "splitoffCreatures is", splitoffCreatures

        splitoffNames = getCreatureNames(splitoffCreatures)

        if self.allCertain():
            creatureNames = getCreatureNames(self.creatures)
            posSplitNames = []
            posKeepNames = []
            pos = self.findAllPossibleSplits(childSize, knownKeep, knownSplit)
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

        print "knownKeepNames", knownKeepNames
        print "knownSplitNames", knownSplitNames

        # lists of CreatureInfo
        strongList = []
        weakList = []
        for ci in self.creatures:
            name = ci.name
            newinfo = CreatureInfo(name, 0, 1)
            if name in splitoffNames:
                weakList.append(newinfo)
                splitoffNames.remove(name)
                # If in knownSplit, set certain
                if name in knownSplitNames:
                    knownSplitNames.remove(name)
                    newinfo.certain = 1
            else:
                strongList.append(newinfo)
                # If in knownKeep, set certain
                if name in knownKeepNames:
                    knownKeepNames.remove(name)
                    newinfo.certain = 1

        afterSplit1 = []
        afterSplit2 = []
        removed1 = []
        removed2 = []
        if self.child1 != None:
            afterSplit1 = self.child1.getAfterSplitCreatures()
            afterSplit2 = self.child2.getAfterSplitCreatures()
            removed1 = self.child1.removed
            removed2 = self.child2.removed

        # Assume that the bigger stack got the better creatures.
        if 2 * childSize > self.getHeight():
            marker1 = otherMarkerId
            marker2 = self.markerId
        else:
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
        self.revealSomeCreatures(getCreatureNames(childCertainAtSplit))

        # Resplitting "too often" fixes some certainty issues.
        self.split(self.childSize2, self.getOtherChildMarkerId())


    def addCreature(self, creatureName):
        print "addCreature()", self, ':', creatureName
        if (self.getHeight() >= 7 and self.child1 == None):
            raise RuntimeError, "Tried adding to 7-high legion"
        ci = CreatureInfo(creatureName, 1, 0)
        self.creatures.append(ci)


    def removeCreature(self, creatureName):
        print "removeCreature()", self, ':', creatureName
        if (self.getHeight() <= 0):
            raise RuntimeError, "Tried removing from 0-high legion"
        self.revealSomeCreatures([creatureName])
        ci = getCreatureInfo(self.creatures, creatureName)

        # Only need to track the removed creature for future parent split
        # predictions if it was here at the time of the split.
        if ci.atSplit:
            self.removed.append(ci)
        removeCreatureByName(self.creatures, creatureName)

    def removeCreatures(self, creatureNames):
        self.revealSomeCreatures(creatureNames)
        for name in creatureNames:
            self.removeCreature(name)



class PredictSplits:
    def __init__(self, playerName, rootId, creatureNames):
        self.playerName = playerName
        # All creatures in root legion must be known

        infoList = []
        for name in creatureNames:
            info = CreatureInfo(name, 1, 1)
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
        ps.getLeaf("Rd01").revealSomeCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Rd01").addCreature("Troll")
        ps.getLeaf("Rd02").revealSomeCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd02").addCreature("Lion")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        ps.printLeaves()

        turn = 2
        print "Turn", turn
        ps.getLeaf("Rd01").revealSomeCreatures(["Gargoyle"])
        ps.getLeaf("Rd01").addCreature("Gargoyle")
        ps.getLeaf("Rd02").revealSomeCreatures(["Lion"])
        ps.getLeaf("Rd02").addCreature("Lion")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        ps.printLeaves()

        turn = 3
        print "Turn", turn
        ps.getLeaf("Rd01").revealSomeCreatures(["Titan"])
        ps.getLeaf("Rd01").addCreature("Warlock")
        ps.getLeaf("Rd02").addCreature("Gargoyle")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd02").allCertain())
        ps.printLeaves()

        turn = 4
        print "Turn", turn
        ps.getLeaf("Rd01").split(2, "Rd03", turn)
        ps.getLeaf("Rd02").split(2, "Rd04", turn)
        ps.getLeaf("Rd01").revealSomeCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd01").addCreature("Cyclops")
        ps.getLeaf("Rd02").revealSomeCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd02").addCreature("Cyclops")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(not ps.getLeaf("Rd03").allCertain())
        assert(not ps.getLeaf("Rd04").allCertain())
        ps.printLeaves()

        turn = 5
        print "Turn", turn
        ps.getLeaf("Rd01").revealSomeCreatures(["Warlock"])
        ps.getLeaf("Rd01").addCreature("Warlock")
        ps.getLeaf("Rd02").addCreature("Ogre")
        ps.getLeaf("Rd03").revealSomeCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Rd03").addCreature("Troll")
        ps.getLeaf("Rd04").revealSomeCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd04").addCreature("Lion")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        ps.printLeaves()

        turn = 6
        print "Turn", turn
        ps.getLeaf("Rd02").split(2, "Rd05", turn)
        ps.getLeaf("Rd01").revealAllCreatures(["Titan", "Warlock", "Warlock",
            "Cyclops", "Troll", "Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd01").removeCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd02").removeCreature("Angel")
        ps.getLeaf("Rd01").addCreature("Angel")
        ps.getLeaf("Rd02").revealSomeCreatures(["Lion", "Lion"])
        ps.getLeaf("Rd02").addCreature("Minotaur")
        ps.getLeaf("Rd04").revealSomeCreatures(["Lion"])
        ps.getLeaf("Rd04").addCreature("Lion")
        ps.getLeaf("Rd02").revealAllCreatures(["Cyclops", "Minotaur", "Lion",
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
        ps.getLeaf("Rd03").revealSomeCreatures(["Troll"])
        ps.getLeaf("Rd03").addCreature("Troll")
        ps.getLeaf("Rd04").revealSomeCreatures(["Lion"])
        ps.getLeaf("Rd04").addCreature("Lion")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        ps.printLeaves()

        turn = 8
        print "Turn", turn
        ps.getLeaf("Rd01").split(2, "Rd02", turn)
        ps.getLeaf("Rd01").revealSomeCreatures(["Cyclops"])
        ps.getLeaf("Rd01").addCreature("Cyclops")
        ps.getLeaf("Rd05").revealSomeCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd05").addCreature("Cyclops")
        assert(not ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd02").allCertain())
        assert(ps.getLeaf("Rd03").allCertain())
        assert(ps.getLeaf("Rd04").allCertain())
        assert(ps.getLeaf("Rd05").allCertain())
        ps.printLeaves()

        turn = 9
        print "Turn", turn
        ps.getLeaf("Rd01").revealSomeCreatures(["Troll"])
        ps.getLeaf("Rd01").addCreature("Troll")
        ps.getLeaf("Rd03").revealSomeCreatures(["Troll"])
        ps.getLeaf("Rd03").addCreature("Troll")
        ps.getLeaf("Rd04").revealSomeCreatures(["Lion", "Lion", "Lion"])
        ps.getLeaf("Rd04").addCreature("Griffon")
        ps.getLeaf("Rd05").revealSomeCreatures(["Cyclops"])
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
        ps.getLeaf("Rd04").revealAllCreatures(["Griffon", "Lion", "Lion", "Lion",
            "Centaur", "Centaur"])
        ps.getLeaf("Rd01").revealSomeCreatures(["Cyclops"])
        ps.getLeaf("Rd01").addCreature("Cyclops")
        ps.getLeaf("Rd03").revealSomeCreatures(["Troll", "Troll"])
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
        ps.getLeaf("Rd03").revealSomeCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd03").addCreature("Warbear")
        ps.getLeaf("Rd05").revealSomeCreatures(["Cyclops"])
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
        ps.getLeaf("Rd01").revealAllCreatures(["Titan", "Warlock", "Warlock",
            "Cyclops", "Cyclops", "Cyclops"])
        ps.getLeaf("Rd05").revealSomeCreatures(["Cyclops", "Cyclops", "Cyclops"])
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
        ps.getLeaf("Rd04").revealAllCreatures(["Griffon", "Lion", "Lion", "Lion",
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
        ps.getLeaf("Rd10").revealSomeCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Rd10").addCreature("Troll")
        ps.getLeaf("Rd11").revealSomeCreatures(["Gargoyle"])
        ps.getLeaf("Rd11").addCreature("Gargoyle")
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 2
        print "Turn", turn
        ps.getLeaf("Rd10").revealSomeCreatures(["Troll"])
        ps.getLeaf("Rd10").addCreature("Troll")
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 3
        print "Turn", turn
        ps.getLeaf("Rd10").revealSomeCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd10").addCreature("Ranger")
        ps.getLeaf("Rd11").revealSomeCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd11").addCreature("Cyclops")
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(not ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 4
        print "Turn", turn
        ps.getLeaf("Rd10").revealAllCreatures(["Titan", "Ranger", "Troll", "Troll",
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
        ps.getLeaf("Rd10").revealSomeCreatures(["Troll"])
        ps.getLeaf("Rd10").addCreature("Troll")
        ps.getLeaf("Rd01").revealSomeCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Rd01").addCreature("Troll")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 6
        print "Turn", turn
        ps.getLeaf("Rd01").revealAllCreatures(["Troll", "Ogre", "Ogre"])
        ps.getLeaf("Rd01").revealSomeCreatures(["Troll"])
        ps.getLeaf("Rd01").addCreature("Troll")
        ps.getLeaf("Rd10").revealSomeCreatures(["Troll", "Troll", "Troll"])
        ps.getLeaf("Rd10").addCreature("Wyvern")
        ps.getLeaf("Rd11").revealSomeCreatures(["Cyclops"])
        ps.getLeaf("Rd11").addCreature("Cyclops")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 7
        print "Turn", turn
        ps.getLeaf("Rd10").split(2, "Rd06", turn)
        ps.getLeaf("Rd11").revealSomeCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd11").addCreature("Lion")
        assert(ps.getLeaf("Rd01").allCertain())
        assert(not ps.getLeaf("Rd06").allCertain())
        assert(not ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 8
        print "Turn", turn
        ps.getLeaf("Rd11").split(2, "Rd07", turn)
        ps.getLeaf("Rd01").revealAllCreatures(["Troll", "Troll", "Ogre", "Ogre"])
        ps.getLeaf("Rd10").removeCreature("Angel")
        ps.getLeaf("Rd01").addCreature("Angel")
        ps.getLeaf("Rd01").removeCreatures(["Troll", "Troll", "Ogre", "Ogre"])
        ps.getLeaf("Rd01").addCreature("Angel")
        ps.getLeaf("Rd10").revealSomeCreatures(["Wyvern"])
        ps.getLeaf("Rd10").addCreature("Wyvern")
        ps.getLeaf("Rd11").revealSomeCreatures(["Lion"])
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
        ps.getLeaf("Rd11").revealSomeCreatures(["Cyclops"])
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
        ps.getLeaf("Rd01").revealAllCreatures(["Angel", "Angel"])
        ps.getLeaf("Rd06").revealSomeCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd06").addCreature("Warbear")
        ps.getLeaf("Rd07").revealSomeCreatures(["Centaur"])
        ps.getLeaf("Rd07").addCreature("Centaur")
        ps.getLeaf("Rd08").revealSomeCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd08").addCreature("Lion")
        ps.getLeaf("Rd10").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd10").addCreature("Ranger")
        ps.getLeaf("Rd11").revealSomeCreatures(["Cyclops", "Cyclops", "Cyclops"])
        ps.getLeaf("Rd11").addCreature("Behemoth")
        ps.getLeaf("Rd01").revealAllCreatures(["Angel", "Angel"])
        ps.getLeaf("Rd01").removeCreatures(["Angel", "Angel"])
        assert(ps.getLeaf("Rd06").allCertain())
        assert(ps.getLeaf("Rd07").allCertain())
        assert(ps.getLeaf("Rd08").allCertain())
        assert(ps.getLeaf("Rd10").allCertain())
        assert(ps.getLeaf("Rd11").allCertain())
        ps.printLeaves()

        turn = 11
        print "Turn", turn
        ps.getLeaf("Rd06").revealSomeCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd06").addCreature("Ranger")
        ps.getLeaf("Rd07").revealSomeCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Rd07").addCreature("Lion")
        ps.getLeaf("Rd08").revealSomeCreatures(["Lion"])
        ps.getLeaf("Rd08").addCreature("Lion")
        ps.getLeaf("Rd10").revealSomeCreatures(["Titan"])
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
        ps.getLeaf("Rd05").revealSomeCreatures(["Troll"])
        ps.getLeaf("Rd05").addCreature("Troll")
        ps.getLeaf("Rd06").revealSomeCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd06").addCreature("Warbear")
        ps.getLeaf("Rd07").revealSomeCreatures(["Lion"])
        ps.getLeaf("Rd07").addCreature("Lion")
        ps.getLeaf("Rd11").revealSomeCreatures(["Lion", "Lion"])
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
        ps.getLeaf("Rd05").revealSomeCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd05").addCreature("Warbear")
        ps.getLeaf("Rd07").revealSomeCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Rd07").addCreature("Cyclops")
        ps.getLeaf("Rd11").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd11").addCreature("Ranger")
        ps.getLeaf("Rd08").revealAllCreatures(["Lion", "Lion", "Centaur",
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
        ps.getLeaf("Rd06").revealAllCreatures(["Warbear", "Warbear", "Ranger",
                "Troll", "Troll"])
        ps.getLeaf("Rd04").revealSomeCreatures(["Cyclops"])
        ps.getLeaf("Rd04").addCreature("Cyclops")
        ps.getLeaf("Rd06").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd06").addCreature("Ranger")
        ps.getLeaf("Rd10").revealSomeCreatures(["Wyvern", "Wyvern"])
        ps.getLeaf("Rd10").addCreature("Hydra")
        ps.getLeaf("Rd11").revealSomeCreatures(["Ranger"])
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
        ps.getLeaf("Rd05").revealSomeCreatures(["Troll"])
        ps.getLeaf("Rd05").addCreature("Troll")
        ps.getLeaf("Rd06").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd06").addCreature("Ranger")
        ps.getLeaf("Rd11").revealSomeCreatures(["Cyclops", "Cyclops"])
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
        ps.getLeaf("Rd06").revealAllCreatures(["Warbear", "Warbear", "Ranger",
                "Ranger", "Ranger", "Troll", "Troll"])
        ps.getLeaf("Rd01").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd01").addCreature("Ranger")
        ps.getLeaf("Rd04").revealSomeCreatures(["Cyclops"])
        ps.getLeaf("Rd04").addCreature("Cyclops")
        ps.getLeaf("Rd05").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd05").addCreature("Ranger")
        ps.getLeaf("Rd07").revealSomeCreatures(["Lion", "Lion"])
        ps.getLeaf("Rd07").addCreature("Ranger")
        ps.getLeaf("Rd10").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd10").addCreature("Ranger")
        ps.getLeaf("Rd11").revealSomeCreatures(["Ranger"])
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
        ps.getLeaf("Rd08").revealAllCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd08").removeCreatures(["Troll", "Troll"])
        ps.getLeaf("Rd05").revealSomeCreatures(["Warbear"])
        ps.getLeaf("Rd05").addCreature("Warbear")
        ps.getLeaf("Rd11").revealSomeCreatures(["Behemoth"])
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
        ps.getLeaf("Rd01").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd01").addCreature("Ranger")
        ps.getLeaf("Rd11").revealSomeCreatures(["Gorgon"])
        ps.getLeaf("Rd11").addCreature("Gorgon")
        ps.getLeaf("Rd12").revealAllCreatures(["Ranger", "Ranger"])
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
        ps.getLeaf("Rd07").revealAllCreatures(["Cyclops", "Ranger", "Lion",
                "Lion", "Centaur", "Centaur"])
        ps.getLeaf("Rd07").removeCreatures(["Lion", "Centaur", "Centaur"])
        ps.getLeaf("Rd01").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd01").addCreature("Ranger")
        ps.getLeaf("Rd03").revealSomeCreatures(["Cyclops"])
        ps.getLeaf("Rd03").addCreature("Cyclops")
        ps.getLeaf("Rd04").revealSomeCreatures(["Cyclops", "Cyclops"])
        ps.getLeaf("Rd04").addCreature("Gorgon")
        ps.getLeaf("Rd07").revealSomeCreatures(["Ranger"])
        ps.getLeaf("Rd07").addCreature("Ranger")
        ps.getLeaf("Rd08").revealSomeCreatures(["Ranger"])
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
        ps.getLeaf("Rd04").revealAllCreatures(["Gorgon", "Cyclops", "Cyclops",
                "Cyclops", "Lion"])
        ps.getLeaf("Rd10").revealAllCreatures(["Titan", "Hydra", "Wyvern",
                "Wyvern", "Warlock"])
        ps.getLeaf("Rd10").addCreature("Angel")
        ps.getLeaf("Rd05").revealAllCreatures(["Warbear", "Warbear", "Ranger",
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
        ps.getLeaf("Gr07").revealSomeCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Gr07").addCreature("Cyclops")
        ps.getLeaf("Gr11").revealSomeCreatures(["Centaur"])
        ps.getLeaf("Gr11").addCreature("Centaur")
        assert(not ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        ps.printLeaves()

        turn = 2
        print "Turn", turn
        ps.getLeaf("Gr07").revealSomeCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Gr07").addCreature("Cyclops")
        ps.getLeaf("Gr11").revealSomeCreatures(["Ogre"])
        ps.getLeaf("Gr11").addCreature("Ogre")
        assert(not ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        ps.printLeaves()

        turn = 3
        print "Turn", turn
        ps.getLeaf("Gr11").revealSomeCreatures(["Centaur", "Centaur", "Centaur"])
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
        ps = PredictSplits("Rd", "Rd12", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Rd12").revealAllCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Bk", "Bk10", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Bk10").revealAllCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Br", "Br05", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Br05").revealAllCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps = PredictSplits("Gd", "Gd08", ['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        aps.add_ps(ps)
        aps.getLeaf("Gd08").revealAllCreatures(['Titan', 'Angel', 'Gargoyle', 'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        turn = 1
        print "\nTurn", turn
        aps.getLeaf("Rd12").split(4, "Rd01", 1)
        aps.getLeaf("Rd12").revealSomeCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Rd12").addCreature("Lion")
        aps.getLeaf("Rd01").revealSomeCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Rd01").addCreature("Troll")
        aps.getLeaf("Bk10").split(4, "Bk03", 1)
        aps.getLeaf("Bk03").revealSomeCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Bk03").addCreature("Lion")
        aps.getLeaf("Bk10").revealSomeCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Bk10").addCreature("Cyclops")
        aps.getLeaf("Br05").split(4, "Br01", 1)
        aps.getLeaf("Br01").revealSomeCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br01").addCreature("Troll")
        aps.getLeaf("Br05").revealSomeCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br05").addCreature("Lion")
        aps.getLeaf("Gd08").split(4, "Gd01", 1)
        aps.getLeaf("Gd01").revealSomeCreatures(['Gargoyle'])
        aps.getLeaf("Gd01").addCreature("Gargoyle")
        aps.getLeaf("Gd08").revealSomeCreatures(['Ogre'])
        aps.getLeaf("Gd08").addCreature("Ogre")
        aps.getLeaf("Rd12").revealSomeCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Rd12").addCreature("Lion")
        aps.getLeaf("Rd01").revealSomeCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Rd01").addCreature("Troll")
        aps.getLeaf("Bk10").revealSomeCreatures(['Ogre'])
        aps.getLeaf("Bk10").addCreature("Ogre")
        aps.getLeaf("Br01").revealSomeCreatures(['Titan'])
        aps.getLeaf("Br01").addCreature("Warlock")
        aps.getLeaf("Br05").revealSomeCreatures(['Lion'])
        aps.getLeaf("Br05").addCreature("Lion")
        aps.getLeaf("Gd01").revealSomeCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd01").addCreature("Cyclops")
        aps.getLeaf("Gd08").revealSomeCreatures(['Gargoyle'])
        aps.getLeaf("Gd08").addCreature("Gargoyle")
        aps.getLeaf("Rd12").revealSomeCreatures(['Gargoyle'])
        aps.getLeaf("Rd12").addCreature("Gargoyle")
        aps.getLeaf("Bk03").revealSomeCreatures(['Ogre'])
        aps.getLeaf("Bk03").addCreature("Ogre")
        aps.getLeaf("Bk10").revealSomeCreatures(['Titan'])
        aps.getLeaf("Bk10").addCreature("Warlock")
        aps.getLeaf("Br01").revealSomeCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Br01").addCreature("Troll")
        aps.getLeaf("Br05").revealSomeCreatures(['Centaur'])
        aps.getLeaf("Br05").addCreature("Centaur")
        aps.getLeaf("Gd01").revealSomeCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd01").addCreature("Cyclops")
        aps.getLeaf("Gd08").revealSomeCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Gd08").addCreature("Troll")
        turn = 4
        print "\nTurn", turn
        aps.getLeaf("Rd12").split(2, "Rd11", 4)
        aps.getLeaf("Rd12").revealSomeCreatures(['Titan'])
        aps.getLeaf("Rd12").addCreature("Warlock")
        aps.getLeaf("Rd11").revealSomeCreatures(['Centaur'])
        aps.getLeaf("Rd11").addCreature("Centaur")
        aps.getLeaf("Rd01").revealSomeCreatures(['Gargoyle'])
        aps.getLeaf("Rd01").addCreature("Gargoyle")
        aps.getLeaf("Bk10").split(2, "Bk04", 4)
        aps.getLeaf("Bk03").revealSomeCreatures(['Ogre', 'Ogre'])
        aps.getLeaf("Bk03").addCreature("Troll")
        aps.getLeaf("Bk10").revealSomeCreatures(['Cyclops'])
        aps.getLeaf("Bk10").addCreature("Cyclops")
        aps.getLeaf("Br01").split(2, "Br02", 4)
        aps.getLeaf("Br01").revealSomeCreatures(['Titan'])
        aps.getLeaf("Br01").addCreature("Warlock")
        aps.getLeaf("Gd01").split(2, "Gd04", 4)
        aps.getLeaf("Gd08").split(2, "Gd02", 4)
        turn = 5
        print "\nTurn", turn
        aps.getLeaf("Rd01").split(2, "Rd07", 5)
        aps.getLeaf("Rd01").revealSomeCreatures(['Troll'])
        aps.getLeaf("Rd01").addCreature("Troll")
        aps.getLeaf("Rd12").revealSomeCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd12").addCreature("Cyclops")
        aps.getLeaf("Bk03").split(2, "Bk12", 5)
        aps.getLeaf("Bk03").revealSomeCreatures(['Troll'])
        aps.getLeaf("Bk03").addCreature("Troll")
        aps.getLeaf("Bk10").revealSomeCreatures(['Ogre'])
        aps.getLeaf("Bk10").addCreature("Ogre")
        aps.getLeaf("Br05").split(2, "Br10", 5)
        aps.getLeaf("Br01").revealSomeCreatures(['Titan'])
        aps.getLeaf("Br01").revealSomeCreatures(['Titan'])
        aps.getLeaf("Br01").addCreature("Warlock")
        aps.getLeaf("Br05").revealSomeCreatures(['Centaur'])
        aps.getLeaf("Br05").addCreature("Centaur")
        aps.getLeaf("Gd01").revealSomeCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Gd01").addCreature("Lion")
        aps.getLeaf("Gd02").revealSomeCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd02").addCreature("Cyclops")
        turn = 6
        print "\nTurn", turn
        aps.getLeaf("Rd12").split(2, "Rd02", 6)
        aps.getLeaf("Rd11").revealAllCreatures(['Centaur', 'Centaur', 'Centaur'])
        aps.getLeaf("Gd04").revealAllCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Gd04").removeCreature("Gargoyle")
        aps.getLeaf("Rd11").removeCreature("Centaur")
        aps.getLeaf("Gd04").removeCreature("Gargoyle")
        aps.getLeaf("Rd11").removeCreature("Centaur")
        aps.getLeaf("Rd11").revealAllCreatures(['Centaur'])
        aps.getLeaf("Rd11").revealSomeCreatures(['Centaur'])
        aps.getLeaf("Rd11").addCreature("Centaur")
        aps.getLeaf("Rd02").revealSomeCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd02").addCreature("Cyclops")
        aps.getLeaf("Rd12").revealSomeCreatures(['Lion', 'Lion'])
        aps.getLeaf("Rd12").addCreature("Ranger")
        aps.getLeaf("Rd01").revealSomeCreatures(['Gargoyle', 'Gargoyle'])
        aps.getLeaf("Rd01").addCreature("Cyclops")
        aps.getLeaf("Bk10").split(2, "Bk09", 6)
        aps.getLeaf("Bk03").revealSomeCreatures(['Lion'])
        aps.getLeaf("Bk03").addCreature("Lion")
        aps.getLeaf("Bk10").revealSomeCreatures(['Ogre'])
        aps.getLeaf("Bk10").addCreature("Ogre")
        aps.getLeaf("Br01").split(2, "Br08", 6)
        aps.getLeaf("Br01").revealSomeCreatures(['Troll'])
        aps.getLeaf("Br01").addCreature("Troll")
        aps.getLeaf("Br02").revealSomeCreatures(['Ogre'])
        aps.getLeaf("Br02").addCreature("Ogre")
        aps.getLeaf("Br05").revealSomeCreatures(['Centaur', 'Centaur'])
        aps.getLeaf("Br05").addCreature("Lion")
        aps.printLeaves()


if __name__ == "__main__":
    unittest.main()
