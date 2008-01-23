#!/bin/bash

# Run 6-AI games in a loop forever, looking for hangs.
# XXX The log will eventually fill the disk. 
#
# @version: $Id:stresstest.sh 3088 2008-01-20 01:18:45Z peterbecker $
# @author: Clemens Katzer
#
# 11.04.2007 (Clemens Katzer): 
# New file "stresstest.sh" based on original stresstest 
# script, but much more functionality:
# - counter which game nr. ongoing
# - stops if stop.flag found
# - if Colossus exits with non-zero exit code, or backup.flag
#   found, make backup copy of relevant log + autosave files
# - call Colossus with property ForceViewBoard set, so that one
#   sees (even if all are AIs) the MasterBoard and StatusScreen.
#
# Perhaps this replace the original stresstest script completely,
# but I was not sure, perhaps someone would rather like to stay
# with the old one.
#
# By default the tests are run with a "nice -19", which means with
# the least priority. If you use the "-A" option, this will be changed
# to a "nice -0", which is a pretty high priority (usually the highest
# avaiable to a normal user)
#
# Usage:
#
# ./stresstest.sh [-C] [-W] [-V <variants>] [ -N <count>] [ -R <count> ] [-A] [ -- <Colossus-Options> ]
#
# That means, all options not meant to the script but rather that they
# shall be given to the Colossus commandline,  must come after an "--".
#
#   -C or --cleansnaps      = delete all snap files before start of each game
#   -W or --forcewindow     = force to get a masterboard windows (usually
#                             for AI-only games there is no board visible)
#   -V or --variants        = run stresstest only for this/these variants
#   -N or --count           = stop latest after N counts
#   -R or --remote          = nr. of remote clients
#   -A or --aggressive      = grab more resources from the OS
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

if [ ! -e Colossus.jar ]
then
  echo ""
  echo "Can't run stresstest: file Colossus.jar does not exist!"
  echo ""
  exit 1
fi


FORCEBOARD=""
# for stresstest, should always be set, even if 1, so that client
# does not ask whether to dispose.
INTERNALROUNDS="-Dnet.sf.colossus.stressTestRounds=1"
done=no
variants=""
niceness=19

humans=-u0
AIs=6
MyName=MyName
if [ "X$USERNAME" != "X" ]
then
  MyName=$USERNAME
fi

# Colossus log files (Colossus[012].log) will be there:
if [ "X$TEMP" = "X" ]
then
  echo "\$TEMP not set, guessing it to be /tmp"
  TEMP="/tmp"
fi

# offer a choice where we mess around
if [ "X$TEST_WORKDIR" = "X" ]
then
  TEST_WORKDIR=`pwd`"/tmp/stresstest"
  echo "\$TEST_WORKDIR not set, using $TEST_WORKDIR"
fi

# Copied log and autosave files will be put under here
# (in separate dirs names "logs.exitonerror.<gamenr>) :
mkdir -p $TEST_WORKDIR


while [ $# -gt 0 -a "$done" = "no" ]
do
  case $1 in
    --cleansn*|-C)
        echo "Setting delete_snaps to yes"
        delete_snaps=yes
        ;;
    --forcewind*|-W)
        echo "Setting forceViewWindow to yes"
        FORCEBOARD=-Dnet.sf.colossus.forceViewBoard=yes
        ;;
    --rounds*|-I)
        shift
        rounds=$1
        echo "Setting howManyRounds to $rounds"
        INTERNALROUNDS=-Dnet.sf.colossus.stressTestRounds=$rounds
        ;;
    --variants|-V)
        shift
        variants=$1
        echo "Setting list of variants to '$variants'"
        ;;
    --count|-N)
        shift
        maxcount=$1
        echo "Setting maxcount to '$maxcount'"
        ;;
    --remote|-R)
        shift
        nrc=$1
        # That thing with the remote clients currently works only well if at 
       # least one human player is there, too (to get a masterboard). 
        # So, let's add one:
        remoteclients="-n$1 -S -m$MyName -A"
        humans=-u1
        echo "Setting remoteclients to '$remoteclients'"
        # 5 instead of 6, because of the one human:
        AIs=`expr 5 - $nrc`
        echo "Setting AIs then to '$AIs'"
        ;;
    --aggressive|-A)
        niceness=0
        ;;
    --)
        done=yes
        ;;
    *)
        echo "Option '$1' is not meaningful to stresstest script itself - aborted."
        echo "Put options meant to starting Colossus.jar itself after a -- separator"
        exit 1
  esac
  shift
done


# ----------------------------------------------------------------------
# If on windows (e.g. Cygwin), create also Win-style (CR-LF) newlines into 
# the log file (Java will print CR-LFs anyway, and if this script does not,
# we end up with mixed style, and e.g. Emacs shows then those ugly ^M chats...)

if expr "$OS" : "\(WIN\|Win\|win\)" >/dev/null
then
  CR="\r"
else
  CR=""
fi

# ----------------------------------------------------------------------
# Remember from where we started. Have to go back there always after
# copying files.

currdir=`pwd`
rm -f stop.flag log

# ----------------------------------------------------------------------
# Under cygwin, if some other terminal still reads the file (e.g. tail -f),
# rm removes the file (or directory entry?), but if stresstest then wants
# to append (re-create??) file with same name, that fails.
# Try this already here, so that it does not bother us inside the loop 
# all the time...
if ! echo -e "Starting stresstest$CR" > $TEST_WORKDIR/log
then
  echo -e "\nAppending to log failed. Perhaps some other terminal still has the file open?"
  echo -e "Exiting.\n"
  exit 1
fi

# ----------------------------------------------------------------------
# continue the game nr, so that copy-logs-to-dir does not use same
# numbers. If one cleans up dirs, can also remove the file i.
if [ -f $TEST_WORKDIR/i ]
then
  i=`cat $TEST_WORKDIR/i`
else
  i=0
fi

iMax=""
if [ "X$maxcount" != "X" ]
then
  iMax=`expr $i + $maxcount`
fi

# ----------------------------------------------------------------------
# Now loop forever, or until a flag file "stop.flag" is created
# or, if -N was given, i reaches iMax (i at start + N-count).
#
# Continue with game number "i" from a file i.
# If colossus game exited with non-zero exit code (or someone created
# a file "backup.flag" while game still running), copy all relevant
# files to ./logs.exitonerror.$i/  directory.

# removed variants:
#   Infinite    - RationalAI move error
#   Balrog      - not in popup selection in player menu, causes
#                 errors with "InputStream for file BalrogCre.xml"
#   Random      - "Recruit-per-terrain loading failed : 
#                      java.io.FileNotFoundException: RandomTer.xml"


variantnr=0
if [ "X$variants" = "X" ]
then
  variants="Abyssal3 Abyssal6 Abyssal9 Badlands Beelzebub Default ExtTitan Outlands \
            Pantheon TG-Wild TitanPlus Undead Unified"
fi

export variants
export variantscount=`echo $variants | wc -w`

rememberSize log

while true
do 
    cd "$HOME/.colossus/saves"
    if [ "X$delete_snaps" = "Xyes" ]
    then
      rm -f snap*.xml
    fi
    cd $currdir

    variantnr=`expr $variantnr + 1`
    if [ $variantnr -gt "$variantscount" ]
    then
      variantnr=1
    fi
    variant=`echo $variants | cut -d " " -f $variantnr`
    # variant=Unified

    timestamp=`date +'%d.%m.%Y %T'`

    i=`expr $i + 1`
    echo -e $i > $TEST_WORKDIR/i
    echo -e "\n$timestamp: Starting game #$i as variant $variant"
    echo -e "$CR\n$timestamp: Starting game #$i as variant $variant$CR" >> $TEST_WORKDIR/log
    
    CMD="java -ea -Djava.util.logging.config.file=logging.properties $FORCEBOARD $INTERNALROUNDS -Xmx128M -jar Colossus.jar $humans -i$AIs $remoteclients -qgS -d 1 --variant $variant $1 $2 $3 $4 $5 $6 $7 $8 $9"
    echo $CMD
    echo -e "$CMD$CR" >> $TEST_WORKDIR/log 2>&1
    nice -$niceness $CMD >> $TEST_WORKDIR/log 2>&1;

    ec=$?

    # copy the part which was now added to a separate file:
    showNew $TEST_WORKDIR/log $TEST_WORKDIR/part.$i.log

    mkbackup=n
    if [ $ec -ne 0 ]
    then
      echo -e "Exit code is non-zero ($ec) - \c"
      mkbackup=y
    elif grep -e "SEVERE" -e WARNING -e Exception $TEST_WORKDIR/part.$i.log
    then
      echo -e "Found SEVERE, WARNING or Exception in $TEST_WORKDIR/part.$i.log - \c"
      mkbackup=y
    elif [ -e backup.flag ]
    then
      echo -e "File backup.flag found - \c"
      mkbackup=y
    fi
    if [ "$mkbackup" = "y" ]
    then
      echo "making backup of logs and autosave files..."
      rm -f backup.flag
      dir=$TEST_WORKDIR/logs.exitonerror.$i
      mkdir -p $dir
      cp -p $TEST_WORKDIR/log $dir
      cp -p $TEST_WORKDIR/part.$i.log $dir
      cd "$HOME/.Colossus/saves"
      files=`ls -rt -1 snap*.xml 2>/dev/null | tail -20`
      if [ "X$files" != "X" ]
      then 
        cp -p $files $dir
      else
        echo "Note: no snap files to copy - autosave not enabled?"
      fi
      cd "$TEMP"
      cp -p Colossus[012].log $dir
      cd $currdir
    fi
    rm -f $TEST_WORKDIR/part.$i.log
    if [ "X$iMax" != "X" ]
    then
      if [ $i -ge $iMax ]
      then
        echo -e    "\nReady: i=$i, $maxcount rounds run - exiting..."
        echo -e "$CR\nReady: i=$i, $maxcount rounds run - exiting...$CR" >> log

        # if relevant, tell the clients to stop, too:
        if [ "X$remoteclients" != "X" ]
        then
          touch stop.flag
        fi
	exit
      fi
    fi
    if [ -e stop.flag ]
    then
      echo -e    "\nFlagfile stop.flag found - exiting..."
      echo -e "$CR\nFlagfile stop.flag found - exiting...$CR" >> log
      exit
    fi
done
