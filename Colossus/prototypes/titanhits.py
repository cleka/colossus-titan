#!/usr/bin/env python

#titanhits.py   David Ripton 2002, converted from my old titanhits.pl
__version__ = '$Id$'

"""
Calculate the probability of each number of hits in a Titan game attack.

Roll N 6-sided dice.  s, the strike number, is an integer in the range
of 2-6.  Each roll >= s is a hit.

p, the probability of a hit on each die, is (7 - s) / 6

For each value h in the range 0-N, the probability of exactly h hits
is (p)^h (1-p)^(N-h) (N choose h)
where (N choose h) is N! / (h! * (N-h)!)
"""

import sys

def fact(arg):
    """Calculate the factorial of a number."""
    answer = 1
    for i in xrange(2, arg + 1):
        answer *= i
    return answer

def choose(a,b):
    """Calculate the value of a choose b"""
    return fact(a) / (fact(b) * fact(a - b))


def main(dice, target):
    try:
        dice = int(dice)
        target = int(target)
    except ValueError:
        print "You need to enter number of dice and target number as integers."
        return
    print "dice is %d and target number is %d" % (dice, target)

    p = (7.0 - target) / 6
    antip = 1 - p
    print "p is %f and antip is %f" % (p, antip)

    cumulative_prob = 0

    print "        equal     or fewer   or greater"

    for h in range(0, dice+1):
        prob = (p ** h) * (antip ** (dice - h)) * choose(dice, h)
        cumulative_prob = cumulative_prob + prob
        print "%2d  %10.5f  %10.5f  %10.5f" % (h, prob, cumulative_prob,
            1 - cumulative_prob + prob)


if __name__ == '__main__':
    if (len(sys.argv) == 3):
        dice = sys.argv[1]
        target = sys.argv[2]
    else:
        dice = raw_input("Enter number of dice: ")
        target = raw_input("Enter target number: ")
    main(dice, target)
