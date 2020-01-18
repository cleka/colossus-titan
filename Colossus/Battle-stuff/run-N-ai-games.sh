#! /bin/sh

#
# Run run-ai-game.sh a given number of times; if not given,
# defaults to 100.
#

N=100

[ $# -gt 0 ] && N=$1

[ -z "$COL_BASE_DIR" ] && COL_BASE_DIR=/var/colossus

export COL_BASE_DIR

COL_TMP_DIR=$COL_BASE_DIR/tmp
COL_BIN_DIR=$COL_BASE_DIR/bin

flag_file=$COL_TMP_DIR/stop.flag

i=1
while [ $i -le $N ]
do
    $COL_BIN_DIR/run-ai-game.sh
    if [ -e $flag_file ]
    then
	echo "Found $flag_file, exiting."
	rm -f $flag_file
	exit
    fi
    i=`expr $i + 1`
done

exit 0
