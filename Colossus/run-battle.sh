
# BATTLE_XML=./Battle-stuff/battle.xml
BATTLE_XML=./Battle-stuff/battle-7v7.xml


source /lib/jvm-private/jdk1.7.0_09.SH


java -Dnet.sf.colossus.forceViewBoard=true \
     -Dnet.sf.colossus.endAfterFirstBattle=true \
     -Djava.util.logging.config.file=logging.properties -Xmx256M -jar Colossus.jar $1 $2 $3 $4 $5 $6 $7 $8 $9 --load $BATTLE_XML


