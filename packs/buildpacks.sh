#!/bin/sh
i=1
while [ $i -le 10 ]
do
    echo "Building pack $i"
    export PACK_NUMBER=$i
    ant release
    cp bin/packs-release.apk store/pack$i.apk
    i=`expr $i + 1`
done

