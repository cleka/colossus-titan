#!/usr/bin/env python2

__version__ = "$Id$"

import sys
import copy
from sets import Set


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
    assert superset(big, little)
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


def getCreatureInfo(li, creatureName):
    """Return the first CreatureInfo that matches the passed name."""
    for ci in li:
        if ci.name == creatureName:
            return ci
    return None

def getCreatureNames(li):
    return [ci.name for ci in li]

def removeLastUncertainCreature(li):
    li.reverse()
    for ii, ci in enumerate(li):
        if not ci.certain:
            del li[ii]    # li.remove(ci) can remove the wrong one
            li.reverse()
            return
    li.reverse()
    raise RuntimeError, "No uncertain creatures"


def minCount(lili, name):
    """lili is a list of lists.  Return the minimum number of times
       name appears in any of the lists contained in lili."""
    min_ = sys.maxint
    for li in lili:
        min_ = min(li.count(name), min_)
    # If we never saw it, reduce to zero
    if min_ == sys.maxint:
        min_ = 0
    return min_


class Perms:
    """Based on Gagan Saksena's code in online Python cookbook
       Not reentrant or thread-safe."""
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
        self.removed = []               # list of CreatureInfo, only if atSplit
        self.parent = parent
        self._clearChildren()

    def _clearChildren(self):
        # All are at the time this node was split.
        self.childSize2 = 0   # size of the smaller splitoff
        self.child1 = None    # child1 is the presumed "better" legion
        self.child2 = None
        self.turnSplit = -1

    def _fullname(self):
        return self.markerId + '(' + str(self.turnCreated) + ")"

    def __str__(self):
        """Show a string with both current and removed creatures."""
        self.creatures.sort(creatureComp)
        s = self._fullname() + ":"
        for ci in self.creatures:
            s += " " + str(ci)
        for ci in self.removed:
            s += " " + str(ci) + "-"
        return s

    def __repr__(self):
        return str(self)

    def getCertainCreatures(self):
        """Return list of CreatureInfo where certain is true."""
        return [ci for ci in self.creatures if ci.certain]

    def allCertain(self):
        """Return True if all creatures are certain."""
        for ci in self.creatures:
            if not ci.certain:
                return False
        return True

    def hasSplit(self):
        """Return True if this legion has split."""
        if self.child1 is None and self.child2 is None:
            return False
        # A legion can have no children, or two, but not one.
        assert self.child1 is not None and self.child2 is not None
        return True

    def getChildren(self):
        if self.hasSplit():
            return [self.child1, self.child2]
        else:
            return []

    def allDescendentsCertain(self):
        """Return True if all of this node's children, grandchildren, etc.
           have no uncertain creatures."""
        for child in self.getChildren():
            if not child.allCertain() or not child.allDescendentsCertain():
                return False
        return True

    def getAtSplitOrRemovedCreatures(self):
        """Return list of CreatureInfo where atSplit is true, plus removed
           creatures."""
        alive = [ci for ci in self.creatures if ci.atSplit]
        dead = self.removed[:]
        return alive + dead

    def getAfterSplitCreatures(self):
        """Return list of CreatureInfo where atSplit is false."""
        return [ci for ci in self.creatures if not ci.atSplit]

    def getCertainAtSplitOrRemovedCreatures(self):
        """Return list of CreatureInfo where both certain and atSplit are
           true, plus removed creatures."""
        alive = [ci for ci in self.creatures if ci.certain and ci.atSplit]
        dead = self.removed[:]
        return alive + dead

    def getOtherChildMarkerId(self):
        for child in self.getChildren():
            if child.markerId != self.markerId:
                return child.markerId
        return None

    def getHeight(self):
        return len(self.creatures)

    def revealCreatures(self, cnl):
        """Reveal all creatures in the creature name list as certain.
        
           Return True iff new information was sent to this legion's parent.
        """
        if ((not cnl) or
          (superset(getCreatureNames(self.getCertainCreatures()), cnl)
           and (self.allDescendentsCertain()))):
            return False

        cil = [CreatureInfo(name, True, True) for name in cnl]

        # Use a copy rather than the original so we can remove
        # creatures as we check for multiples.
        dupe = copy.deepcopy(cil)

        # Confirm that all creatures that were certain still fit
        # along with the revealed creatures.
        count = len(dupe)
        for ci in self.getCertainCreatures():
            if ci in dupe:
                dupe.remove(ci)
            else:
                count += 1

        if count > self.getHeight():
            err = ("Certainty error in revealCreatures count=%d height=%d") % (
              count, self.getHeight())
            raise RuntimeError, err

        # Then mark passed creatures as certain and then
        # communicate this to the parent, to adjust other legions.

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
        for ci in self.getCertainCreatures():
            if ci in dupe:
                dupe.remove(ci)
        for ci in dupe:
            for ci2 in self.creatures:
                if not ci2.certain and ci2.name == ci.name:
                    ci2.certain = True
                    break

        # Need to remove count uncertain creatures.
        for unused in range(count):
            removeLastUncertainCreature(self.creatures)
        if self.parent is None:
            return False
        else:
            self.parent._updateChildContents()
            return True


    def _updateChildContents(self):
        """Tell this parent legion the updated contents of its children."""
        names = []
        for child in self.getChildren():
            names.extend(getCreatureNames(
              child.getCertainAtSplitOrRemovedCreatures()))
        told_parent = self.revealCreatures(names)
        if not told_parent:
            self.split(self.childSize2, self.getOtherChildMarkerId())


    def _isLegalInitialSplitoff(self):
        if self.getHeight() != 4:
            return False
        names = getCreatureNames(self.creatures)
        return (names.count("Titan") + names.count("Angel") == 1)

    def _findAllPossibleSplits(self, childSize, knownKeep, knownSplit):
        """Return a list of lists of all legal combinations of splitoff names.

           Raises if the combination of knownKeep and knownSplit contains
           uncertain creatures.
        """
        # Sanity checks
        if len(knownSplit) > childSize:
            raise RuntimeError, "More known splitoffs than splitoffs"
        assert self.getHeight() <= 8
        if self.getHeight() == 8:
            assert childSize == 4
            assert "Titan" in getCreatureNames(self.creatures)
            assert "Angel" in getCreatureNames(self.creatures)

        knownCombo = knownSplit + knownKeep
        certain = getCreatureNames(self.getCertainCreatures())
        assert superset(certain, knownCombo), \
          "knownCombo contains uncertain creatures"

        unknowns = getCreatureNames(self.creatures)
        for ci in knownCombo:
            unknowns.remove(ci)

        numUnknownsToSplit = childSize - len(knownSplit)

        perms = Perms()
        unknownCombos = perms.findCombinations(unknowns, numUnknownsToSplit)
        possibleSplitsSet = Set()
        for combo in unknownCombos:
            pos = tuple(knownSplit + combo)
            if self.getHeight() != 8:
                possibleSplitsSet.add(pos)
            else:
                cil = [CreatureInfo(name, False, True) for name in pos]
                posnode = Node(self.markerId, -1, cil, self)
                if posnode._isLegalInitialSplitoff():
                    possibleSplitsSet.add(pos)
        possibleSplits = [list(pos) for pos in possibleSplitsSet]
        return possibleSplits


    def _chooseCreaturesToSplitOut(self, possibleSplits):
        """Decide how to split this legion, and return a list of creature
           names to remove.  Return empty list on error.
        """
        maximize = (2 * len(possibleSplits[0]) > self.getHeight())

        bestKillValue = None
        creaturesToRemove = []
        for li in possibleSplits:
            totalKillValue = 0
            for name in li:
                totalKillValue += killValue[name]
            if ((bestKillValue is None) or
                    (maximize and totalKillValue > bestKillValue) or
                    (not maximize and totalKillValue < bestKillValue)):
                bestKillValue = totalKillValue
                creaturesToRemove = li
        return creaturesToRemove

    def _splitChildren(self):
        for child in self.getChildren():
            if child.hasSplit():
                child.split(child.childSize2, child.getOtherChildMarkerId())


    def split(self, childSize, otherMarkerId, turn=-1):
        assert self.getHeight() <= 8

        if turn == -1:
            turn = self.turnSplit   # Re-predicting earlier split
        else:
            self.turnSplit = turn   # New split

        if self.hasSplit():
            knownKeep = getCreatureNames(
              self.child1.getCertainAtSplitOrRemovedCreatures())
            knownSplit = getCreatureNames(
              self.child2.getCertainAtSplitOrRemovedCreatures())
        else:
            knownKeep = []
            knownSplit = []
        knownCombo = knownKeep + knownSplit
        certain = getCreatureNames(self.getCertainCreatures())
        if not superset(certain, knownCombo):
            # We need to abort this split and trust that it will be redone
            # after the certainty information percolates up to the parent.
            return

        possibleSplits = self._findAllPossibleSplits(childSize, knownKeep,
          knownSplit)
        splitoffNames = self._chooseCreaturesToSplitOut(possibleSplits)

        posSplitNames = []
        posKeepNames = []
        for names in possibleSplits:
            if superset(certain, names):
                posKeepNames.append(subtractLists(certain, names))
                posSplitNames.append(names)

        knownKeep = []
        knownSplit = []
        for name in certain:
            if not name in knownKeep:
                minKeep = minCount(posKeepNames, name)
                for unused in range(minKeep):
                    knownKeep.append(name)
            if not name in knownSplit:
                minSplit = minCount(posSplitNames, name)
                for unused in range(minSplit):
                    knownSplit.append(name)

        def _inherit_parent_certainty(certain, known, other):
            """If one of the child legions is fully known, assign the 
               creatures in the other child legion the same certainty they 
               have in the parent.
            """
            all = certain[:]
            assert superset(all, known)
            for name in known:
                all.remove(name)
            assert superset(all, other)
            for name in all:
                if (all.count(name) > other.count(name)):
                    other.append(name)

        if len(knownSplit) == childSize:
            _inherit_parent_certainty(certain, knownSplit, knownKeep)
        elif len(knownKeep) == self.getHeight() - childSize:
            _inherit_parent_certainty(certain, knownKeep, knownSplit)

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
                if name in knownSplit:
                    knownSplit.remove(name)
                    newinfo.certain = True
            else:
                strongList.append(newinfo)
                # If in knownKeepNames, set certain
                if name in knownKeep:
                    knownKeep.remove(name)
                    newinfo.certain = True

        if self.hasSplit():
            strongList += self.child1.getAfterSplitCreatures()
            for ci in self.child1.removed:
                strongList.remove(ci)
            weakList += self.child2.getAfterSplitCreatures()
            for ci in self.child2.removed:
                weakList.remove(ci)
            self.child1.creatures = strongList
            self.child2.creatures = weakList
        else:
            self.child1 = Node(self.markerId, turn, strongList, self)
            self.child2 = Node(otherMarkerId, turn, weakList, self)
            self.childSize2 = self.child2.getHeight()

        self._splitChildren()


    def merge(self, other, turn):
        """Recombine this legion and other, because it was not possible to
           move either one. The two legions must share the same parent.  If
           either legion has the parent's markerId, then that legion will
           remain. Otherwise this legion will remain."""
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


    def addCreature(self, creatureName):
        if (self.getHeight() >= 7 and not self.hasSplit()):
            raise RuntimeError, "Tried adding to 7-high legion"
        ci = CreatureInfo(creatureName, True, False)
        self.creatures.append(ci)


    def removeCreature(self, creatureName):
        if (self.getHeight() <= 0):
            raise RuntimeError, "Tried removing from 0-high legion"
        self.revealCreatures([creatureName])
        ci = getCreatureInfo(self.creatures, creatureName)

        # Only need to track the removed creature for future parent split
        # predictions if it was here at the time of the split.
        if ci.atSplit:
            self.removed.append(ci)
        self.creatures.remove(ci)

    def removeCreatures(self, creatureNames):
        self.revealCreatures(creatureNames)
        for name in creatureNames:
            self.removeCreature(name)


class PredictSplits:
    def __init__(self, playerName, rootId, creatureNames):
        self.playerName = playerName
        # All creatures in root legion must be known

        infoList = [CreatureInfo(name, True, True) for name in creatureNames]
        self.root = Node(rootId, 0, infoList, None)

    def getNodes(self, node):
        """Return all nodes in tree."""
        nodes = [node]
        for child in node.getChildren():
            nodes += self.getNodes(child)
        return nodes

    def getLeaves(self, node):
        """Return all non-empty childless nodes in tree."""
        leaves = []
        if not node.hasSplit():
            if node.getHeight() > 0:
                leaves.append(node)
        else:
            for child in node.getChildren():
                leaves += self.getLeaves(child)

        # If duplicate markerIds, prune the older node.
        prune_these = Set()
        for leaf1 in leaves:
            for leaf2 in leaves:
                if leaf1 != leaf2 and leaf1.markerId == leaf2.markerId:
                    if leaf1.turnCreated == leaf2.turnCreated:
                        raise (RuntimeError,
                                "Two leaf nodes with same markerId and turn")
                    elif leaf1.turnCreated < leaf2.turnCreated:
                        prune_these.add(leaf1)
                    else:
                        prune_these.add(leaf2)
        for leaf in prune_these:
            leaves.remove(leaf)

        return leaves

    def printLeaves(self):
        """Print all childless nodes in tree, in string order. """
        leaves = self.getLeaves(self.root)
        leaves.sort(lambda a,b: cmp(str(a), str(b)))
        print
        for leaf in leaves:
            print leaf
        print

    def printNodes(self):
        """Print all nodes in tree, in string order. """
        nodes = self.getNodes(self.root)
        nodes.sort(lambda a,b: cmp((a.turnCreated, a.markerId),
          (b.turnCreated, b.markerId)))
        print
        for node in nodes:
            print node
        print

    def dumpAsDot(self, filename=None):
        """Dump all nodes in the tree in Graphviz dot format"""
        if filename is None:
            f = sys.stdout
        else:
            f = open(filename, "w")
        li = []
        li.append("digraph G {")
        for node in self.getNodes(self.root):
            for child in node.getChildren():
                li.append('"%s" -> "%s";' % (str(node), str(child)))
        li.append("}")
        s = "\n".join(li)
        print >>f, s

    def getLeaf(self, markerId):
        """Return the leaf node with matching markerId"""
        leaves = self.getLeaves(self.root)
        for leaf in leaves:
            if leaf.markerId == markerId:
                return leaf
        return None
