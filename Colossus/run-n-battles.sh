#! /bin/sh

N=10
[ $# -gt 0 ] && N=$1

touch Battle-stuff/basenr.txt
basenr=`cat Battle-stuff/basenr.txt`
[ -z "$basenr" ] && basenr=0

i=1
while [ $i -le $N ]
do
    nr=`expr $basenr + $i`
    echo running battle nr $nr
    ./run-battle.sh
    mkdir -p out/$nr
    mv /tmp/Colossus*.log ./out/$nr/.
    i=`expr $i + 1`
done

echo $nr > Battle-stuff/basenr.txt
