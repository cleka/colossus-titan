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
        print "test 7 ends"


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
        assert(not ps.getLeaf("Gd11").allCertain())
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
        print "\ntest 8 ends"

        """
        Start Gd04: Tit, Ang, Ogr, Ogr, Cen, Cen, Gar, Gar
        Turn 1  Gd04 splits off Tit, Gar, Gar, Cen into Gd12
                Gd12 teleports (Titan)
                Gd04 recruits Cen with Cen
                Gd12 recruits Wlk with Tit
        Turn 2  Gd04 recruits Tro with 2xOgr
                Gd12 recruits Cyc with 2xGar
        Turn 3  Gd04 recruits Lio with 2xCen
        Turn 4  Gd04 splits off Cen, Cen into Gd07
                Gd04 recruits Tro with Tro
        Turn 5  Gd04 recruits Lio with Lio
                Gd12 recruits Cen with Cen
        Turn 6  Gd04 splits off Ogr, Ogr into Gd08
                Gd12 splits off Gar, Gar into Gd03
                Gd08 recruits Ogr with Ogr
                Gd12 recruits Lio with 2xCen
        Turn 7  Gd07 recruits Lio with 2xCen
        Turn 8  Gd12 splits off Cen, Cen into Gd09
                Gd03 recruits Cyc with 2xGar
                Gd07 recruits Lio with Lio
                Gd08 recruits Tro with 2xOgr
        Turn 9  Gd04 recruits Ran with 2xLio
                Gd07 recruits Ran with 2xLio
                Gd08 recruits Tro with Tro
                Gd12 recruits Wlk with Tit
        Turn 10 Gd07 recruits Ran with Ran
                Gd12 recruits Ran with 2xLio
        Turn 11 Gd04 recruits Cyc with Cyc
                Gd07 recruits Ran with 2xLio
                Gd09 recruits Cen with Cen
                Gd08 loses Tro, Tro, Ogr, Ogr, Ogr
        Turn 12 Gd07 splits off Cen, Cen into Gd06
                Gd09 recruits Lio with 2xCen
        Turn 13 Gd12 splits off Lio, Lio into Gd10
                Gd03 recruits Cyc with Cyc
                Gd04 recruits Ran with 2xLio
                Gd12 recruits Gor with 2xCyc
        Turn 14 Gd04 splits off Lio, Lio into Gd02
                Gd07 recruits Min with 2xLio
                Gd09 recruits Wbe with 3xCen
                Gd10 recruits Lio with Lio
                Gd12 recruits Cyc with Cyc
        Turn 15 Gd12 splits off Cyc, Ran into Gd11
                Gd07 revealed as Ran, Ran, Ran, Min, Lio, Lio
                Angel summoned from Gd04 to Gd07
                Gd02 recruits Ran with 2xLio
                Gd11 recruits Ran with Ran
        Turn 17 Gd07 splits off Lio, Lio into Gd08
                Gd12 revealed to Gr as Tit, Wlk, Gor, Cyc, Cyc
                Gd02 recruits Ran with Ran
                Gd04 recruits Tro with Tro
                Gd09 recruits Lio with Lio
                Gd10 recruits Gri with 3xLio
                Gd11 recruits Cyc with Cyc
                Gd07 loses Ang, Ran, Ran, Ran, Min
                Gd04 loses Ran, Ran, Tro, Tro, Tro
        Turn 18 Gd02 recruits Lio with Lio
        Turn 19 Gd08 recruits Ran with 2xLio
                Gd10 recruits Ran with 2xLio
                Gd06 loses Cen, Cen
        Turn 20 Gd03 recruits Gor with 2xCyc
                Gd10 recruits Lio with Lio
        Turn 21 Gd02 recruits Gri with 3xLio
                Gd08 recruits Lio with Lio
                Gd11 recruits Ran with Ran
        Turn 22 Gd09 splits off Cen, Cen into Gd07
                Gd03 recruits Beh with 3xCyc
                Gd08 recruits Wbe with Wbe
                Gd10 recruits Tro with Ran
                Gd11 recruits Lio with Ran
                Gd12 recruits Gor with 2xCyc
                Gd10 revealed to Bk as Gri, Ran, Tro, 4xLio
        Turn 23 Gd02 recruits Tro with Ran
                Gd03 recruits Gor with 2xCyc
                Gd07 recruits Cen with Cen
                Gd08 recruits Lio with Lio
                Gd11 recruits Lio with Lio
                Gd12 recruits Cyc with Cyc
        Turn 24 Gd02 splits off Lio, Lio into Gd04
                Gd10 splits off Lio, Lio into Gd06
                Gd02 recruits Tro with Ran
                Gd03 recruits Beh with 3xCyc
                Gd05 recruits Cyc with 2xGar
                Gd08 recruits Cen with Lio
        """


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
        print "\ntest 9 ends"

        """
        Start Gr11: Tit, Ang, Ogr, Ogr, Cen, Cen, Gar, Gar
        Turn 1  Gr11 splits off Tit, Gar, Gar, Cen into Gr02
                Gr02 teleports (Titan)
                Gr02 recruits Cyc with 2xGar
                Gr11 recruits Cen with Cen
        Turn 2  Gr02 recruits Wlk with Tit
                Gr11 recruits Cen with Cen
        Turn 3  Gr02 recruits Cyc with Cyc
                Gr11 recruits Lio with 2xCen
        Turn 4  Gr02 splits off Gar, Gar into Gr10
                Gr11 splits off Cen, Cen, Cen into Gr03
                Gr03 merges back into Gr11
        Turn 5  Gr11 splits off Cen, Cen, Cen into Gr12
                Gr02 recruits Wlk with Wlk
                Gr10 recruits Cyc with 2xGar
                Gr12 recruits Wbe with 3xCen
                Gr10 is attacked, revealed as Gar, Gar, Cyc, eliminated
        Turn 6  Gr02 recruits Cen with Cen
        """


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

    """
        Start Gr08: Tit, Ang, Ogr, Ogr, Cen, Cen, Gar, Gar
        Turn 1  Gr08 splits off Tit, Gar, Ogr, Ogr into Gr04
                Gr04 recruits Wlk with Tit
                Gr08 recruits Cen with Cen
        Turn 2  Gr04 recruits Gar with Gar
        Turn 3  Gr04 recruits Cyc with 2xGar
        Turn 4  Gr04 splits off Gar, Gar into Gr06
                Gr04 recruits Ogr with Ogr
                Gr08 recruits Lio with 2xCen
        Turn 5  Gr04 recruits Cyc with Cyc
                Gr06 recruits Ogr
                Gr08 recruits Lio with Lio
        Turn 6  Gr04 splits off Ogr, Ogr into Gr07
                Gr08 splits off Cen, Cen into Gr11
                Gr04 recruits Wlk with Tit
                Gr06 recruits Ogr with Ogr
                Gr11 loses Cen, Cen
        Turn 7  Gr04 recruits Cyc with Cyc
                Gr06 recruits Tro with 2xOgr
                Gr07 recruits Ogr with Ogr
        Turn 8  Gr04 splits off Cyc, Ogr into Gr12
                Gr07 recruits Min with 3xOgr
                Gr08 recruits Gar with Gar
                Gr06 loses Tro, Gar, Gar, Ogr, Ogr
                Gr12 loses Cyc, Ogr
        Turn 9  Gr08 recruits Cyc with 2xGar
    """


if __name__ == "__main__":
    unittest.main()
