#!/bin/sh

for FILE in $*; do
      #get the suffix on the file name
      SUFFIX=`expr $FILE : ".*\.\(.*\)"`

      if [ "$SUFFIX" = "java" ] ; then
         echo "Indenting $FILE java style";
         $CVSROOT/CVSROOT/jacobe -cfg=$CVSROOT/CVSROOT/colossus-jacobe.cfg -overwrite -nobackup $FILE
      fi
done

