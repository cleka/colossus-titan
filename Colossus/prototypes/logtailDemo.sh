#!/bin/bash

#
# Testmain / demo script for using logFollowing.SH
#
# This script will write blocks of lines to a file (lines.log),
# and uses the functions from logFollowing.SH to copy the just
# added part to a separate file (and show to screen).
#

. ../logFollowing.SH

export LOGFILE=lines.log
rm -f $LOGFILE

#echo oldline1 >> $LOGFILE
#echo oldline2 >> $LOGFILE
#echo oldline3 >> $LOGFILE

rememberSize $LOGFILE

echo "file started" >> $LOGFILE

i=0
while [ ! -e stop.flag ]
do
  start=`expr $size + 1`
  echo "New lines will be added starting from line $start"

  j=0
  while [ $j -le $i ]
  do
    echo "$i / $j" >> $LOGFILE
    j=`expr $j + 1`
  done
  
  PARTFILE=lines.part-$i.log
  showNew $LOGFILE $PARTFILE
  echo "There were $partsize lines added. The new part is:"
  echo "========================"
  cat $PARTFILE
  echo "------------------------"
  echo ""
  
  i=`expr $i + 1`

done

