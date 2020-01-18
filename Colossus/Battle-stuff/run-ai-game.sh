#! /bin/bash

#
# This runs one instance of Colossus with 6 AIs playing against each other.
# It's meant to be executed by run-N-ai-games.sh.
#
# See end of this script for more explanations.
#

VIEW_BOARD=false

[ -z "$COL_BASE_DIR" ] && COL_BASE_DIR=/var/colossus

export COL_BASE_DIR

COL_BIN_DIR=$COL_BASE_DIR/bin
COL_LOG_DIR=$COL_BASE_DIR/log
COL_TMP_DIR=$COL_BASE_DIR/tmp
GAMES_BASE_DIR=$COL_BASE_DIR/games

log_file=$COL_LOG_DIR/run-ai-game.log

if [ ! -w $COL_BIN_DIR -o ! -w $COL_LOG_DIR -o ! -w $GAMES_BASE_DIR ]
then
    echo "PROBLEM: bin-, log- and games dir must exist and writable!"
    exit 1
fi

[ -e $COL_BIN_DIR/jre.SH ] && source $COL_BIN_DIR/jre.SH

next_game_nr_file=$COL_TMP_DIR/next-game-nr.txt

[ ! -e $next_game_nr_file ] && echo 1 > $next_game_nr_file

this_game_nr=`cat $next_game_nr_file`
expr $this_game_nr + 1 > $next_game_nr_file

game_nr=`printf "%05d" $this_game_nr`

modulo=`expr $this_game_nr % 100`
start=`expr $this_game_nr - $modulo`
endnr=`expr $start + 99`
group_dir=`printf "%05d-%05d" $start $endnr`

game_dir=$GAMES_BASE_DIR/$group_dir/$game_nr

mkdir -p $game_dir

cat $COL_BIN_DIR/logging.properties | sed -e 's/%t\/Colossus%g.log/Colossus%g.log/' > $game_dir/logging.properties

JAR=$COL_BIN_DIR/Colossus.jar

export COLOSSUS_JRE_ARGS="$COLOSSUS_JRE_ARGS 
-Dnet.sf.colossus.forceViewBoard=$VIEW_BOARD 
-Duser.home=$game_dir 
-Djava.util.logging.config.file=$game_dir/logging.properties 
-Xmx2048M "

game_log=$game_dir/Colossus0.log
flag_file=$game_dir/flagfile.txt
cd $game_dir

echo "`date '+%Y-%m-%d %T'` Starting game $game_nr ($game_log)" >> $log_file

sec_epoch_start=`date '+%s'`

java $COLOSSUS_JRE_ARGS -jar $JAR -i 6 -Z 1 -M 1 -r 1 -d 10 -t 60 -S -g -q >> $log_file --flagfile $flag_file 2>&1

sec_epoch_ready=`date '+%s'`
duration=`expr $sec_epoch_ready - $sec_epoch_start`

echo "`date '+%Y-%m-%d %T'` Finished game $game_nr"       >> $log_file
echo "                    Game lasted $duration seconds." >> $log_file
echo                                                      >> $log_file

exit 0

# ======================================================================

These two scripts are meant to be run in a directory structure like this:

[clemens@fuji1 colossus]$ tree -L 2 /var/colossus
/var/colossus
├── bin
│   ├── Colossus.jar
│   ├── jre.SH
│   ├── libs
│   ├── logging.properties
│   ├── nohup.out
│   ├── run-ai-game.sh
│   └── run-N-ai-games.sh
├── games
│   └── 00000-00099
│       ├── 00000
│       ├── 00001
│       └── 00002
├── log
│   └── run-ai-game.log
└── tmp
    └── next-game-nr.txt


cd /var/colossus/bin/

nohup ./run-N-ai-games.sh 200 &

Disk space estimate:

So far, 1615 games run, this uses ~46 GB of disk space (most of that is
the autosave games).
One could run cleanup-games.sh (from Colossus public game server) to delete
intermediate save games (only keep first 5 and last 5, or something like that).

What for? Just to generate game data, perhaps to feed it to a self-learning
AI. This games here would not train it to play well, but it could learn the
movements which are allowed and which not. Perhaps...

