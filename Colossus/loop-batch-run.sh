#! /bin/bash

COUNT=10

if [ $# -gt 0 ]
then
    COUNT=$1
fi

export LOAD_FILE="00-Clemens-500.xml"

echo "Loop started, doing $COUNT games."

rm -f stop.flag

stop_found=0

i=1
while [ $i -le $COUNT -a $stop_found -eq 0 ]
do
    echo ""
    echo "Game #$i"
    ./batch-run.sh
    i=`expr $i + 1`

    if [ -e stop.flag ]
    then
	stop_found=1
    fi
    
done

echo "Batch loop ends"
