#! /bin/sh

[ -e ~/.colossus/jre.SH ] && source ~/.colossus/jre.SH

BATTLE_XML=./Battle-stuff/battle.xml

FORCE_BOARD=false
END_AFTER_BATTLE=true
LOG_PROPERTIES=logging.properties

# -P: probability based battle hits
PROB_HITS="-P"

#
# You can override any of the above by putting it to a file and then
# use it e.g. by:
#  ./run-battle.sh ./battle1.cf
#

if [ $# -gt 0 ]
then
    if [ -e $1 ]
    then
	echo "Sourcing battle settings '$1'..."
	source $1
	shift
    fi
fi

echo "Starting Colossus for $BATTLE_XML"

java -Dnet.sf.colossus.forceViewBoard=$FORCE_BOARD           \
     -Dnet.sf.colossus.endAfterFirstBattle=$END_AFTER_BATTLE \
     -Djava.util.logging.config.file=$LOG_PROPERTIES         \
     -Xmx256M -jar Colossus.jar                              \
     --load $BATTLE_XML $PROB_HITS

