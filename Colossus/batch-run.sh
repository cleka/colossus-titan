#! /bin/bash

#
# Used to start Colossus many times in a loop (typically loop-batch-run.sh),
# loading a given saved game and continuing automatically, with "autoquit"
# option.
#
# It can be controlled whether board is shown.
# It can be controlled which file to load.
#
# After completion, moves all log files to a created temp
# directory under /var/tmp/colossus/<timestamp> .
#

TMPNAM=`date '+%Y%m%d-%H%M%S'`
TMPDIR=/var/tmp/colossus/$TMPNAM

[ -e ~/.colossus/jre.SH ] && source ~/.colossus/jre.SH

if [ -z "$FORCEBOARD" ]
then
  FORCEBOARD=true
fi

if [ -e forceboard ]
then
  FORCEBOARD=`cat forceboard`
fi

# have them here in a never-happens if block just for easy copy-pasting...
if [ 1 -eq 0 ]
then  
    LOAD_FILE=000-Will-muster-Chimera.xml
    LOAD_FILE=00-3-players.xml
    LOAD_FILE=00-After.recruit-Hydra.xml
    LOAD_FILE=00-Clemens-500.xml
    LOAD_FILE=00-Rational-500.xml
    LOAD_FILE=00-Rational-x3.xml
    LOAD_FILE=111-ClemensAI+2SimpleAI.xml
    LOAD_FILE=11-recruit-lion.xml
    LOAD_FILE=illegal-recruit.xml
    LOAD_FILE=keep-snap1684779979674_21-clemens-Muster.xml
    LOAD_FILE=keep-snap1684781850102_11-clemens-Muster.xml
    LOAD_FILE=keep-snap1684782292749_10-clemens-Muster.xml
fi


if [ -z "$LOAD_FILE" ]
then
    LOAD_FILE=111-ClemensAI+2SimpleAI.xml
    LOAD_FILE=000-Will-muster-Chimera.xml
fi

if [ -e loadfile ]
then
    LOAD_FILE=`cat loadfile`
fi

mkdir $TMPDIR

echo "Running Colossus with LOAD_FILE=$LOAD_FILE"

java $COLOSSUS_JRE_ARGS -Dnet.sf.colossus.forceViewBoard=$FORCEBOARD \
     -Djava.util.logging.config.file=logging.properties \
     -Xmx${MEM_SIZE} -jar Colossus.jar \
     -i 3 -L 1 -Z 2               \
     --load $LOAD_FILE            \
     -g                           \
     -q                           \
     $1 $2 $3 $4 $5 $6 $7 $8 $9

mv /tmp/Colossus*log $TMPDIR

exit 0
