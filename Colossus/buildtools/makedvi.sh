#!/bin/sh

# $Header$
# Written by Romain Dolbeau
# makedvi.sh, build a DVI file from a LaTeX source file
# should handle correctly file with and without BIBTEX references.

LATEX=latex
BIBTEX=bibtex

BASEFILE=${1:?"Need an argument, the name of the LaTeX input file, without the .tex extension"}
TEXFILE=${BASEFILE}.tex
BIBFILE=${BASEFILE}.bib

if [ \! -f $TEXFILE ]; then
    echo "LaTeX input file "$TEXFILE" doesn't exist";
    exit 1
fi

$LATEX $BASEFILE

if [ -f $BIBFILE ]; then
    $BIBTEX $BASEFILE
fi;

$LATEX $BASEFILE
$LATEX $BASEFILE

exit 0
