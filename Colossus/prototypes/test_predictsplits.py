#!/usr/bin/env python2
# Needs Python 2.3+ for sets

__version__ = "$Id$"

import unittest
from predictsplits import PredictSplits, CreatureInfo, Node


class PredictSplitsTestCase(unittest.TestCase):

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


    def testPredictSplits4(self):
        print "\ntest 4 begins"
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
        print "test 4 ends"


    def testPredictSplits5(self):
        print "\ntest 5 begins"
        ps = PredictSplits("Gd", "Gd04", ['Titan', 'Angel', 'Gargoyle',
          'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps.getLeaf("Gd04").revealCreatures(['Titan', 'Angel', 'Gargoyle',
          'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps.printLeaves()
        assert(ps.getLeaf("Gd04").allCertain())

        turn = 1
        print "\nTurn", turn
        ps.getLeaf("Gd04").split(4, "Gd12", turn)
        ps.getLeaf("Gd12").revealCreatures(['Titan'])
        ps.getLeaf("Gd04").revealCreatures(['Centaur'])
        ps.getLeaf("Gd04").addCreature("Centaur")
        ps.getLeaf("Gd12").revealCreatures(['Titan'])
        ps.getLeaf("Gd12").addCreature("Warlock")
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 2
        ps.getLeaf("Gd04").revealCreatures(['Ogre', 'Ogre'])
        ps.getLeaf("Gd04").addCreature("Troll")
        ps.getLeaf("Gd12").revealCreatures(['Gargoyle', 'Gargoyle'])
        ps.getLeaf("Gd12").addCreature("Cyclops")
        assert(ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 3
        ps.getLeaf("Gd04").revealCreatures(['Centaur', 'Centaur'])
        ps.getLeaf("Gd04").addCreature("Lion")
        assert(ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 4
        print "\nTurn", turn
        ps.getLeaf("Gd04").split(2, "Gd07", turn)
        ps.getLeaf("Gd04").revealCreatures(['Troll'])
        ps.getLeaf("Gd04").addCreature("Troll")
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 5
        print "\nTurn", turn
        ps.getLeaf("Gd04").revealCreatures(['Lion'])
        ps.getLeaf("Gd04").addCreature("Lion")
        ps.getLeaf("Gd12").revealCreatures(['Centaur'])
        ps.getLeaf("Gd12").addCreature("Centaur")
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 6
        print "\nTurn", turn
        ps.getLeaf("Gd04").split(2, "Gd08", turn)
        ps.getLeaf("Gd12").split(2, "Gd03", turn)
        ps.getLeaf("Gd08").revealCreatures(['Ogre'])
        ps.getLeaf("Gd08").addCreature("Ogre")
        ps.getLeaf("Gd12").revealCreatures(['Centaur', 'Centaur'])
        ps.getLeaf("Gd12").addCreature("Lion")
        ps.getLeaf("Gd07").revealCreatures(['Centaur', 'Centaur'])
        ps.getLeaf("Gd07").addCreature("Lion")
        ps.getLeaf("Gd12").revealCreatures(['Lion'])
        ps.getLeaf("Gd12").addCreature("Lion")
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd08").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 7
        print "\nTurn", turn
        ps.getLeaf("Gd12").split(2, "Gd09", turn)
        ps.getLeaf("Gd03").revealCreatures(['Gargoyle', 'Gargoyle'])
        ps.getLeaf("Gd03").addCreature("Cyclops")
        ps.getLeaf("Gd07").revealCreatures(['Lion'])
        ps.getLeaf("Gd07").addCreature("Lion")
        ps.getLeaf("Gd08").revealCreatures(['Ogre', 'Ogre'])
        ps.getLeaf("Gd08").addCreature("Troll")
        assert(ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd08").allCertain())
        assert(not ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 8
        print "\nTurn", turn
        ps.getLeaf("Gd04").revealCreatures(['Lion', 'Lion'])
        ps.getLeaf("Gd04").addCreature("Ranger")
        ps.getLeaf("Gd07").revealCreatures(['Lion', 'Lion'])
        ps.getLeaf("Gd07").addCreature("Ranger")
        ps.getLeaf("Gd08").revealCreatures(['Troll'])
        ps.getLeaf("Gd08").addCreature("Troll")
        ps.getLeaf("Gd12").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd12").addCreature("Cyclops")
        assert(ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd08").allCertain())
        assert(not ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 10
        print "\nTurn", turn
        ps.getLeaf("Gd07").revealCreatures(['Ranger'])
        ps.getLeaf("Gd07").addCreature("Ranger")
        ps.getLeaf("Gd12").revealCreatures(['Lion', 'Lion'])
        ps.getLeaf("Gd12").addCreature("Ranger")
        assert(ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd08").allCertain())
        assert(not ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 11
        print "\nTurn", turn
        ps.getLeaf("Gd03").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd03").addCreature("Cyclops")
        ps.getLeaf("Gd07").revealCreatures(['Lion', 'Lion'])
        ps.getLeaf("Gd07").addCreature("Ranger")
        ps.getLeaf("Gd09").revealCreatures(['Centaur'])
        ps.getLeaf("Gd09").addCreature("Centaur")
        ps.getLeaf("Gd08").removeCreature("Troll")
        ps.getLeaf("Gd08").removeCreature("Troll")
        ps.getLeaf("Gd08").removeCreature("Ogre")
        ps.getLeaf("Gd08").removeCreature("Ogre")
        ps.getLeaf("Gd08").removeCreature("Ogre")
        assert(ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 12
        print "\nTurn", turn
        ps.getLeaf("Gd07").split(2, "Gd06", turn)
        ps.getLeaf("Gd09").revealCreatures(['Centaur', 'Centaur'])
        ps.getLeaf("Gd09").addCreature("Lion")
        assert(ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 13
        print "\nTurn", turn
        ps.getLeaf("Gd12").split(2, "Gd10", turn)
        ps.getLeaf("Gd03").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd03").addCreature("Cyclops")
        ps.getLeaf("Gd04").revealCreatures(['Lion', 'Lion'])
        ps.getLeaf("Gd04").addCreature("Ranger")
        ps.getLeaf("Gd12").revealCreatures(['Cyclops', 'Cyclops'])
        ps.getLeaf("Gd12").addCreature("Gorgon")
        assert(ps.getLeaf("Gd03").allCertain())
        assert(ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd10").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 14
        print "\nTurn", turn
        ps.getLeaf("Gd04").split(2, "Gd02", turn)
        ps.getLeaf("Gd07").revealCreatures(['Lion', 'Lion'])
        ps.getLeaf("Gd07").addCreature("Minotaur")
        ps.getLeaf("Gd09").revealCreatures(['Centaur', 'Centaur', 'Centaur'])
        ps.getLeaf("Gd09").addCreature("Warbear")
        ps.getLeaf("Gd10").revealCreatures(['Lion'])
        ps.getLeaf("Gd10").addCreature("Lion")
        ps.getLeaf("Gd12").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd12").addCreature("Cyclops")
        assert(ps.getLeaf("Gd03").allCertain())
        assert(ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd10").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 15
        print "\nTurn", turn
        ps.getLeaf("Gd12").split(2, "Gd11", turn)
        assert(not ps.getLeaf("Gd02").allCertain())
        assert(ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd10").allCertain())
        assert(not ps.getLeaf("Gd11").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 16
        print "\nTurn", turn
        ps.getLeaf("Gd07").revealCreatures(['Ranger', 'Ranger', 'Ranger',
          'Minotaur', 'Lion', 'Lion'])
        ps.getLeaf("Gd04").removeCreature("Angel")
        ps.getLeaf("Gd07").addCreature("Angel")
        ps.getLeaf("Gd02").revealCreatures(['Lion', 'Lion'])
        ps.getLeaf("Gd02").addCreature("Ranger")
        ps.getLeaf("Gd11").revealCreatures(['Ranger'])
        ps.getLeaf("Gd11").addCreature("Ranger")
        assert(ps.getLeaf("Gd02").allCertain())
        assert(ps.getLeaf("Gd03").allCertain())
        assert(ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd07").allCertain())
        assert(ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())
        assert(not ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 17
        print "\nTurn", turn
        ps.getLeaf("Gd07").split(2, "Gd08", turn)
        ps.getLeaf("Gd02").revealCreatures(['Ranger'])
        ps.getLeaf("Gd02").addCreature("Ranger")
        ps.getLeaf("Gd04").revealCreatures(['Troll'])
        ps.getLeaf("Gd04").addCreature("Troll")
        ps.getLeaf("Gd09").revealCreatures(['Lion'])
        ps.getLeaf("Gd09").addCreature("Lion")
        ps.getLeaf("Gd10").revealCreatures(['Lion', 'Lion', 'Lion'])
        ps.getLeaf("Gd11").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd11").addCreature("Cyclops")
        ps.getLeaf("Gd07").revealCreatures(['Angel', 'Ranger', 'Ranger',
          'Ranger', 'Minotaur'])
        ps.getLeaf("Gd07").removeCreature("Minotaur")
        ps.getLeaf("Gd07").removeCreature("Angel")
        ps.getLeaf("Gd07").removeCreature("Ranger")
        ps.getLeaf("Gd07").removeCreature("Ranger")
        ps.getLeaf("Gd07").removeCreature("Ranger")
        ps.getLeaf("Gd04").revealCreatures(['Ranger', 'Ranger', 'Troll',
          'Troll', 'Troll'])
        ps.getLeaf("Gd04").removeCreature("Ranger")
        ps.getLeaf("Gd04").removeCreature("Ranger")
        ps.getLeaf("Gd04").removeCreature("Troll")
        ps.getLeaf("Gd04").removeCreature("Troll")
        ps.getLeaf("Gd04").removeCreature("Troll")
        ps.getLeaf("Gd02").revealCreatures(['Lion'])
        ps.getLeaf("Gd02").addCreature("Lion")
        ps.getLeaf("Gd08").revealCreatures(['Lion', 'Lion'])
        ps.getLeaf("Gd08").addCreature("Ranger")
        ps.getLeaf("Gd10").revealCreatures(['Lion', 'Lion'])
        ps.getLeaf("Gd10").addCreature("Ranger")
        ps.getLeaf("Gd06").removeCreature("Centaur")
        ps.getLeaf("Gd06").removeCreature("Centaur")
        ps.getLeaf("Gd03").revealCreatures(['Cyclops', 'Cyclops'])
        ps.getLeaf("Gd03").addCreature("Gorgon")
        ps.getLeaf("Gd10").revealCreatures(['Lion'])
        ps.getLeaf("Gd10").addCreature("Lion")
        assert(ps.getLeaf("Gd02").allCertain())
        assert(ps.getLeaf("Gd03").allCertain())
        assert(ps.getLeaf("Gd08").allCertain())
        assert(ps.getLeaf("Gd09").allCertain())
        assert(ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())
        assert(ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 20
        print "\nTurn", turn
        ps.getLeaf("Gd02").revealCreatures(['Lion', 'Lion', 'Lion'])
        ps.getLeaf("Gd08").revealCreatures(['Lion'])
        ps.getLeaf("Gd08").addCreature("Lion")
        ps.getLeaf("Gd09").revealCreatures(['Lion'])
        ps.getLeaf("Gd09").addCreature("Lion")
        ps.getLeaf("Gd11").revealCreatures(['Ranger'])
        ps.getLeaf("Gd11").addCreature("Troll")
        assert(ps.getLeaf("Gd02").allCertain())
        assert(ps.getLeaf("Gd03").allCertain())
        assert(ps.getLeaf("Gd08").allCertain())
        assert(ps.getLeaf("Gd09").allCertain())
        assert(ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())
        assert(ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 21
        print "\nTurn", turn
        ps.printLeaves()

        turn = 22
        print "\nTurn", turn
        ps.getLeaf("Gd09").split(2, "Gd07", turn)
        ps.getLeaf("Gd03").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        ps.getLeaf("Gd03").addCreature("Behemoth")
        ps.getLeaf("Gd08").revealCreatures(['Ranger'])
        ps.getLeaf("Gd08").addCreature("Troll")
        ps.getLeaf("Gd09").revealCreatures(['Warbear'])
        ps.getLeaf("Gd09").addCreature("Warbear")
        ps.getLeaf("Gd10").revealCreatures(['Ranger'])
        ps.getLeaf("Gd10").addCreature("Troll")
        ps.getLeaf("Gd11").revealCreatures(['Ranger'])
        ps.getLeaf("Gd11").addCreature("Lion")
        ps.getLeaf("Gd12").revealCreatures(['Cyclops', 'Cyclops'])
        ps.getLeaf("Gd12").addCreature("Gorgon")
        assert(ps.getLeaf("Gd02").allCertain())
        assert(ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(ps.getLeaf("Gd08").allCertain())
        assert(not ps.getLeaf("Gd09").allCertain())
        assert(ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())
        assert(ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 23
        print "\nTurn", turn
        ps.getLeaf("Gd03").split(2, "Gd05", turn)
        ps.getLeaf("Gd02").revealCreatures(['Ranger'])
        ps.getLeaf("Gd02").addCreature("Troll")
        ps.getLeaf("Gd03").revealCreatures(['Cyclops', 'Cyclops'])
        ps.getLeaf("Gd03").addCreature("Gorgon")
        ps.getLeaf("Gd07").revealCreatures(['Centaur'])
        ps.getLeaf("Gd07").addCreature("Centaur")
        ps.getLeaf("Gd08").revealCreatures(['Lion'])
        ps.getLeaf("Gd08").addCreature("Lion")
        ps.getLeaf("Gd11").revealCreatures(['Lion'])
        ps.getLeaf("Gd11").addCreature("Lion")
        ps.getLeaf("Gd12").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd12").addCreature("Cyclops")
        assert(ps.getLeaf("Gd02").allCertain())
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd05").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(ps.getLeaf("Gd08").allCertain())
        assert(not ps.getLeaf("Gd09").allCertain())
        assert(ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())
        assert(ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()

        turn = 24
        print "\nTurn", turn
        ps.getLeaf("Gd02").split(2, "Gd04", turn)
        ps.getLeaf("Gd10").split(2, "Gd06", turn)
        ps.getLeaf("Gd02").revealCreatures(['Ranger'])
        ps.getLeaf("Gd02").addCreature("Troll")
        ps.getLeaf("Gd03").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        ps.getLeaf("Gd03").addCreature("Behemoth")
        ps.getLeaf("Gd05").revealCreatures(['Gargoyle', 'Gargoyle'])
        ps.getLeaf("Gd05").addCreature("Cyclops")
        ps.getLeaf("Gd08").revealCreatures(['Lion'])
        ps.getLeaf("Gd08").addCreature("Centaur")
        assert(not ps.getLeaf("Gd02").allCertain())
        assert(ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd05").allCertain())
        assert(not ps.getLeaf("Gd06").allCertain())
        assert(not ps.getLeaf("Gd07").allCertain())
        assert(ps.getLeaf("Gd08").allCertain())
        assert(not ps.getLeaf("Gd09").allCertain())
        assert(not ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())
        assert(ps.getLeaf("Gd12").allCertain())
        ps.printLeaves()
        print "\ntest 5 ends"


    def testPredictSplits6(self):
        print "\ntest 6 begins"
        ps = PredictSplits("Gr", "Gr11", ['Titan', 'Angel', 'Gargoyle',
          'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps.getLeaf("Gr11").revealCreatures(['Titan', 'Angel', 'Gargoyle',
          'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps.printLeaves()

        turn = 1
        print "\nTurn", turn
        ps.getLeaf("Gr11").split(4, "Gr02", turn)
        ps.getLeaf("Gr02").revealCreatures(['Titan'])
        ps.getLeaf("Gr02").revealCreatures(['Gargoyle', 'Gargoyle'])
        ps.getLeaf("Gr02").addCreature("Cyclops")
        ps.getLeaf("Gr11").revealCreatures(['Centaur'])
        ps.getLeaf("Gr11").addCreature("Centaur")
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        ps.printLeaves()

        turn = 2
        print "\nTurn", turn
        ps.getLeaf("Gr02").revealCreatures(['Titan'])
        ps.getLeaf("Gr02").addCreature("Warlock")
        ps.getLeaf("Gr11").revealCreatures(['Centaur'])
        ps.getLeaf("Gr11").addCreature("Centaur")
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        ps.printLeaves()

        turn = 3
        print "\nTurn", turn
        ps.getLeaf("Gr02").revealCreatures(['Cyclops'])
        ps.getLeaf("Gr02").addCreature("Cyclops")
        ps.getLeaf("Gr11").revealCreatures(['Centaur', 'Centaur'])
        ps.getLeaf("Gr11").addCreature("Lion")
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        ps.printLeaves()

        turn = 4
        print "\nTurn", turn
        ps.getLeaf("Gr02").split(2, "Gr10", turn)
        ps.getLeaf("Gr11").split(3, "Gr03", turn)
        ps.getLeaf("Gr11").merge(ps.getLeaf("Gr03"), turn)
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(not ps.getLeaf("Gr10").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        ps.printLeaves()

        turn = 5
        print "\nTurn", turn
        ps.getLeaf("Gr11").split(3, "Gr12", turn)
        ps.getLeaf("Gr02").revealCreatures(['Warlock'])
        ps.getLeaf("Gr02").addCreature("Warlock")
        ps.getLeaf("Gr10").revealCreatures(['Gargoyle', 'Gargoyle'])
        ps.getLeaf("Gr10").addCreature("Cyclops")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(ps.getLeaf("Gr10").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        assert(not ps.getLeaf("Gr12").allCertain())
        ps.getLeaf("Gr12").revealCreatures(['Centaur', 'Centaur', 'Centaur'])
        ps.getLeaf("Gr12").addCreature("Warbear")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(ps.getLeaf("Gr10").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        assert(ps.getLeaf("Gr12").allCertain())

        ps.getLeaf("Gr10").revealCreatures(['Cyclops', 'Gargoyle', 'Gargoyle'])
        ps.getLeaf("Gr10").removeCreature("Cyclops")
        ps.getLeaf("Gr10").revealCreatures(['Gargoyle', 'Gargoyle'])
        ps.getLeaf("Gr10").addCreature("Cyclops")
        ps.getLeaf("Gr10").removeCreature("Cyclops")
        ps.getLeaf("Gr10").removeCreature("Gargoyle")
        ps.getLeaf("Gr10").removeCreature("Gargoyle")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(not ps.getLeaf("Gr11").allCertain())
        assert(ps.getLeaf("Gr12").allCertain())

        turn = 6
        print "\nTurn", turn
        ps.getLeaf("Gr02").revealCreatures(['Centaur'])
        ps.getLeaf("Gr02").addCreature("Centaur")
        ps.printLeaves()
        assert(ps.getLeaf("Gr02").allCertain())
        assert(ps.getLeaf("Gr11").allCertain())
        assert(ps.getLeaf("Gr12").allCertain())
        print "\ntest 6 ends"


    def testPredictSplits7(self):
        print "\ntest 7 begins"
        ps = PredictSplits("Gr", "Gr08", ['Titan', 'Angel', 'Gargoyle',
          'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])
        ps.printLeaves()

        turn = 1
        print "Turn", turn
        ps.getLeaf("Gr08").split(4, "Gr04", turn)
        ps.getLeaf("Gr04").revealCreatures(["Titan"])
        ps.getLeaf("Gr04").revealCreatures(["Titan"])
        ps.getLeaf("Gr04").addCreature("Warlock")
        ps.getLeaf("Gr08").revealCreatures(["Centaur"])
        ps.getLeaf("Gr08").addCreature("Centaur")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 2
        print "Turn", turn
        ps.getLeaf("Gr04").revealCreatures(["Gargoyle"])
        ps.getLeaf("Gr04").addCreature("Gargoyle")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 3
        print "Turn", turn
        ps.getLeaf("Gr04").revealCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Gr04").addCreature("Cyclops")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 4
        print "Turn", turn
        ps.getLeaf("Gr04").split(2, "Gr06", turn)
        ps.getLeaf("Gr04").revealCreatures(["Ogre"])
        ps.getLeaf("Gr04").addCreature("Ogre")
        ps.getLeaf("Gr08").revealCreatures(["Centaur", "Centaur"])
        ps.getLeaf("Gr08").addCreature("Lion")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr06").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 5
        print "Turn", turn
        ps.getLeaf("Gr04").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr04").addCreature("Cyclops")
        ps.getLeaf("Gr06").addCreature("Ogre")
        ps.getLeaf("Gr08").revealCreatures(["Lion"])
        ps.getLeaf("Gr08").addCreature("Lion")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr06").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 6
        print "Turn", turn
        ps.getLeaf("Gr04").split(2, "Gr07", turn)
        ps.getLeaf("Gr08").split(2, "Gr11", turn)
        ps.getLeaf("Gr04").revealCreatures(["Titan"])
        ps.getLeaf("Gr04").addCreature("Warlock")
        ps.getLeaf("Gr06").revealCreatures(["Ogre"])
        ps.getLeaf("Gr06").addCreature("Ogre")
        ps.getLeaf("Gr11").removeCreatures(["Centaur", "Centaur"])
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr06").allCertain())
        assert(not ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 7
        print "Turn", turn
        ps.getLeaf("Gr04").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr04").addCreature("Cyclops")
        ps.getLeaf("Gr06").revealCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Gr06").addCreature("Troll")
        ps.getLeaf("Gr07").revealCreatures(["Ogre"])
        ps.getLeaf("Gr07").addCreature("Ogre")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr06").allCertain())
        assert(not ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 8
        print "Turn", turn
        ps.getLeaf("Gr04").split(2, "Gr12", turn)
        ps.printLeaves()

        ps.getLeaf("Gr07").revealCreatures(["Ogre", "Ogre", "Ogre"])
        ps.getLeaf("Gr07").addCreature("Minotaur")
        ps.getLeaf("Gr08").revealCreatures(["Gargoyle"])
        ps.getLeaf("Gr08").addCreature("Gargoyle")
        ps.getLeaf("Gr06").removeCreatures(["Troll", "Gargoyle", "Gargoyle",
          "Ogre", "Ogre"])
        ps.getLeaf("Gr12").removeCreatures(["Cyclops", "Ogre"])
        ps.printLeaves()
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(ps.getLeaf("Gr08").allCertain())

        turn = 9
        print "Turn", turn
        ps.getLeaf("Gr08").revealCreatures(["Gargoyle", "Gargoyle"])
        ps.getLeaf("Gr08").addCreature("Cyclops")
        ps.printLeaves()
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(ps.getLeaf("Gr08").allCertain())

        turn = 11
        print "Turn", turn
        ps.getLeaf("Gr08").split(2, "Gr01", turn)
        ps.getLeaf("Gr04").revealCreatures(["Titan"])
        ps.getLeaf("Gr04").addCreature("Warlock")
        ps.getLeaf("Gr08").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr08").addCreature("Cyclops")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr01").allCertain())
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 12
        print "Turn", turn
        ps.getLeaf("Gr04").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr04").addCreature("Cyclops")
        ps.getLeaf("Gr08").revealCreatures(["Lion"])
        ps.getLeaf("Gr08").addCreature("Lion")
        ps.printLeaves()
        ps.getLeaf("Gr01").removeCreatures(["Gargoyle", "Gargoyle"])
        ps.printLeaves()
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(ps.getLeaf("Gr08").allCertain())

        turn = 13
        print "Turn", turn
        ps.getLeaf("Gr08").split(2, "Gr02", turn)
        ps.getLeaf("Gr07").revealCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Gr07").addCreature("Troll")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 14
        print "Turn", turn
        ps.getLeaf("Gr04").split(2, "Gr06", turn)
        ps.getLeaf("Gr04").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr04").addCreature("Cyclops")
        ps.getLeaf("Gr06").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr06").addCreature("Cyclops")
        ps.getLeaf("Gr08").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr08").addCreature("Cyclops")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr06").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 15
        print "Turn", turn
        ps.getLeaf("Gr06").removeCreatures(["Cyclops", "Cyclops", "Cyclops"])
        ps.getLeaf("Gr02").revealCreatures(["Centaur"])
        ps.getLeaf("Gr02").addCreature("Centaur")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 16
        print "Turn", turn
        ps.getLeaf("Gr04").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr04").addCreature("Cyclops")
        ps.getLeaf("Gr07").revealCreatures(["Ogre", "Ogre", "Ogre"])
        ps.getLeaf("Gr07").addCreature("Minotaur")
        ps.getLeaf("Gr08").revealCreatures(["Lion"])
        ps.getLeaf("Gr08").addCreature("Lion")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr02").allCertain())
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())

        turn = 17
        print "Turn", turn
        ps.getLeaf("Gr02").removeCreatures(["Lion", "Centaur", "Centaur"])
        ps.printLeaves()
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(ps.getLeaf("Gr08").allCertain())
        ps.getLeaf("Gr08").split(2, "Gr12", turn)
        ps.getLeaf("Gr08").revealCreatures(["Cyclops", "Cyclops"])
        ps.getLeaf("Gr08").addCreature("Gorgon")
        ps.printLeaves()
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())
        assert(not ps.getLeaf("Gr12").allCertain())

        turn = 18
        print "Turn", turn
        ps.getLeaf("Gr12").removeCreatures(["Lion", "Lion"])
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(ps.getLeaf("Gr08").allCertain())
        ps.getLeaf("Gr04").split(2, "Gr12", turn)
        ps.getLeaf("Gr07").split(2, "Gr09", turn)
        ps.getLeaf("Gr08").revealCreatures(["Cyclops", "Cyclops"])
        ps.getLeaf("Gr08").addCreature("Gorgon")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr07").allCertain())
        assert(ps.getLeaf("Gr08").allCertain())
        assert(not ps.getLeaf("Gr09").allCertain())
        assert(not ps.getLeaf("Gr12").allCertain())

        turn = 19
        print "Turn", turn
        ps.getLeaf("Gr09").removeCreatures(["Ogre", "Ogre"])
        ps.getLeaf("Gr08").split(2, "Gr06", turn)
        ps.getLeaf("Gr07").revealCreatures(["Minotaur", "Minotaur"])
        ps.getLeaf("Gr07").addCreature("Unicorn")
        ps.getLeaf("Gr08").revealCreatures(["Cyclops", "Cyclops"])
        ps.getLeaf("Gr08").addCreature("Gorgon")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(not ps.getLeaf("Gr06").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(not ps.getLeaf("Gr08").allCertain())
        assert(not ps.getLeaf("Gr12").allCertain())

        turn = 20
        print "Turn", turn
        ps.getLeaf("Gr06").removeCreatures(["Gorgon", "Lion"])
        ps.getLeaf("Gr08").revealCreatures(["Cyclops", "Cyclops", "Cyclops"])
        ps.getLeaf("Gr08").addCreature("Behemoth")
        ps.getLeaf("Gr12").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr12").addCreature("Cyclops")
        ps.printLeaves()
        assert(not ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(ps.getLeaf("Gr08").allCertain())
        assert(not ps.getLeaf("Gr12").allCertain())

        turn = 21
        print "Turn", turn
        ps.getLeaf("Gr04").revealCreatures(["Cyclops"])
        ps.getLeaf("Gr04").addCreature("Gargoyle")
        ps.getLeaf("Gr12").revealCreatures(["Cyclops", "Cyclops", "Cyclops"])
        ps.getLeaf("Gr12").addCreature("Behemoth")
        ps.printLeaves()
        assert(ps.getLeaf("Gr04").allCertain())
        assert(ps.getLeaf("Gr07").allCertain())
        assert(ps.getLeaf("Gr08").allCertain())
        assert(ps.getLeaf("Gr12").allCertain())
        print "\ntest 7 ends"


    def testPredictSplits8(self):
        print "\ntest 8 begins"
        ps = PredictSplits("Gd", "Gd03", ['Titan', 'Angel', 'Gargoyle', 
          'Gargoyle', 'Centaur', 'Centaur', 'Ogre', 'Ogre'])

        turn = 1
        print "\nTurn", turn
        ps.getLeaf("Gd03").split(4, "Gd04", turn)
        ps.getLeaf("Gd03").revealCreatures(['Ogre'])
        ps.getLeaf("Gd03").addCreature("Ogre")
        ps.getLeaf("Gd04").revealCreatures(['Gargoyle', 'Gargoyle'])
        ps.getLeaf("Gd04").addCreature("Cyclops")
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        ps.printLeaves()

        turn = 2
        print "\nTurn", turn
        ps.getLeaf("Gd03").revealCreatures(['Centaur'])
        ps.getLeaf("Gd03").addCreature("Centaur")
        ps.getLeaf("Gd04").revealCreatures(['Ogre'])
        ps.getLeaf("Gd04").addCreature("Ogre")
        ps.printLeaves()
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(len(ps.getLeaf("Gd03").getCertainCreatures()) == 5)
        assert(len(ps.getLeaf("Gd04").getCertainCreatures()) == 5)

        turn = 3
        print "\nTurn", turn
        ps.getLeaf("Gd04").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd04").addCreature("Cyclops")
        ps.printLeaves()
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())

        turn = 4
        print "\nTurn", turn
        ps.getLeaf("Gd04").split(2, "Gd11", turn)
        ps.getLeaf("Gd04").revealCreatures(['Ogre', 'Ogre'])
        ps.getLeaf("Gd04").addCreature("Troll")
        ps.printLeaves()
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd11").allCertain())

        turn = 5
        print "\nTurn", turn
        ps.getLeaf("Gd04").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd04").addCreature("Cyclops")
        ps.getLeaf("Gd11").revealCreatures(['Gargoyle', 'Gargoyle'])
        ps.getLeaf("Gd11").addCreature("Cyclops")
        ps.printLeaves()
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())
        assert(len(ps.getLeaf("Gd03").getCertainCreatures()) == 5)
        assert(len(ps.getLeaf("Gd04").getCertainCreatures()) == 6)

        turn = 6
        print "\nTurn", turn
        ps.getLeaf("Gd04").split(2, "Gd10", turn)
        ps.printLeaves()
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())

        turn = 7
        print "\nTurn", turn
        ps.getLeaf("Gd04").revealCreatures(['Cyclops', 'Cyclops', 'Cyclops'])
        ps.getLeaf("Gd04").addCreature("Behemoth")
        ps.printLeaves()
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())

        turn = 8
        print "\nTurn", turn
        ps.getLeaf("Gd03").revealCreatures(['Ogre', 'Ogre'])
        ps.getLeaf("Gd03").addCreature("Troll")
        ps.printLeaves()
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())

        turn = 9
        print "\nTurn", turn
        ps.getLeaf("Gd03").split(2, "Gd01", turn)
        ps.printLeaves()
        ps.getLeaf("Gd04").revealCreatures(['Troll'])
        ps.getLeaf("Gd04").addCreature("Troll")
        ps.getLeaf("Gd03").revealCreatures(['Troll'])
        ps.getLeaf("Gd03").addCreature("Troll")
        ps.getLeaf("Gd11").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd11").addCreature("Cyclops")
        ps.printLeaves()
        assert(not ps.getLeaf("Gd01").allCertain())
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())
        assert(len(ps.getLeaf("Gd04").getCertainCreatures()) == 6)

        ps.getLeaf("Gd01").removeCreature("Centaur")
        ps.getLeaf("Gd01").removeCreature("Centaur")
        ps.printLeaves()
        assert(len(ps.getLeaf("Gd03").getCertainCreatures()) == 5)

        turn = 10
        print "\nTurn", turn
        ps.getLeaf("Gd04").split(2, "Gd08", turn)
        ps.getLeaf("Gd11").revealCreatures(['Cyclops'])
        ps.getLeaf("Gd11").addCreature("Cyclops")
        ps.printLeaves()
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd08").allCertain())
        assert(ps.getLeaf("Gd10").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())

        ps.getLeaf("Gd10").removeCreature("Ogre")
        ps.getLeaf("Gd10").removeCreature("Ogre")
        ps.printLeaves()
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd08").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())

        turn = 11
        print "\nTurn", turn
        ps.getLeaf("Gd03").revealCreatures(['Troll'])
        ps.getLeaf("Gd03").addCreature("Troll")
        ps.printLeaves()
        print "\ntest 8 ends"
        assert(not ps.getLeaf("Gd03").allCertain())
        assert(not ps.getLeaf("Gd04").allCertain())
        assert(not ps.getLeaf("Gd08").allCertain())
        assert(ps.getLeaf("Gd11").allCertain())


if __name__ == "__main__":
    unittest.main()
