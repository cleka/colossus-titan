#!/usr/bin/env python2

import sys
import unittest


def wrap_line(line, max_width=79, hanging=2):
    """Wrap a single line of Python code into a possibly-multiline string.

       First line keeps its indentation.  Subsequent lines get the first
       line's indentation, plus hanging spaces added.

       All but the last line get a backslash appended.

       XXX Only breaks at whitespace.
       XXX Fails on strings with embedded whitespace.  
       XXX Wraps last line if it barely fits.
    """
    if len(line) <= max_width:
        return line
    assert '\n' not in line

    leng = len(line)
    ls = line.lstrip()
    initial_indent = leng - len(ls)

    words = line.split()

    lines = []
    word = 0
    end_words = False
    cont = ' \\'
    contlen = len(cont)
    right = max_width - contlen
    while not end_words:
        if lines:
            left = initial_indent + hanging
        else:
            left = initial_indent
        cur_line = ''
        end_words = False
        if len(words[word]) > right - left: # Handle very long words
            cur_line = words[word]
            word += 1
            if word >= len(words):
                end_words = True
        else:                               # Compose line of words
            while len(cur_line) + len(words[word]) <= right - left:
                cur_line += words[word] + ' '
                word += 1
                if word >= len(words):
                    end_words = True
                    break
        cur_line = cur_line.rstrip()
        lines.append(left * ' ' + cur_line + cont)
    # Last line lacks continuation backslash
    lines[-1] = lines[-1][:-contlen]
    return '\n'.join([cur_line for cur_line in lines])


class WrapTestCase(unittest.TestCase):
    def test_wrap_line(self):
        line = ''
        assert wrap_line(line) == line
        line = "        print '\ntest 9 begins'"
        assert wrap_line(line) == line
        line = "        aps.getLeaf('Bk10').revealCreatures(['Titan', 'Archangel', 'Angel', 'Angel'])"
        out =  """        aps.getLeaf('Bk10').revealCreatures(['Titan', 'Archangel', 'Angel', \\
          'Angel'])"""
        assert wrap_line(line) == out

if __name__ == '__main__':
    unittest.main()
