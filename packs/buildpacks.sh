#!/bin/sh
mkdir -p store/apks
i=1
while [ $i -le 10 ]
do
    echo "Building pack $i"
    export PACK_NUMBER=$i
    ../gradlew -PpackNumber=$i build
    cp build/outputs/apk/packs-release.apk store/apks/pack$i.apk
    i=`expr $i + 1`
done

