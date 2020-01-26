#! /bin/sh

N=10
[ $# -gt 0 ] && N=$1 && shift

touch Battle-stuff/basenr.txt
basenr=`cat Battle-stuff/basenr.txt`
[ -z "$basenr" ] && basenr=0

i=1
while [ $i -le $N -a ! -e /tmp/stop.n.flag ]
do
    nr=`expr $basenr + $i`
    mkdir -p out/$nr
    ./run-battle.sh $*
    ec=$?
    if [ $ec -eq 0 ]
    then
	mv /tmp/Colossus*.log ./out/$nr/.
    else
	echo "run-battle ended with exit code '$ec'?" > ./out/$nr/error.txt
    fi
    i=`expr $i + 1`
done

rm -f /tmp/stop.n.flag

echo $nr > Battle-stuff/basenr.txt
