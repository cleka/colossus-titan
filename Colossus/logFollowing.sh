#! /bin/echo Dont_execte-source_me_instead!

# How to use:
# -----------
# This file is not meant to be executed directly.
# You need to source this file via ". logFollowing.sh" to get the 
# functions defined in your script.
#
# What it does:
# -------------
#
# Before the first writing, call rememberSize to remember the number of lines
# in the file (given to function as argument).
# After writing, call showNew to tail the added lines from that file
# (arg1) to a separate file (arg2).
#

function rememberSize ()
{
  FILE=$1
  if [ -e $FILE ]
  then
      size=`wc -l $1 | cut -d " " -f 1`
  else
      # Let's assume it's created afterwards from scratch
      size=0
  fi
}

function showNew()
{
  FILE=$1
  PARTFILE=$2
  startline=`expr $size + 1`
  tail -n +$startline $FILE > $PARTFILE
  partsize=`wc -l $PARTFILE | cut -d " " -f 1`

  size=`expr $size + $partsize`
}

