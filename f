#!/bin/bash

# edit this path here to point to your jdk
java=/usr/lib/jvm/jdk1.8.0_20/bin/java


pushd $(dirname `which "$0"`) >/dev/null; fieldhome="$PWD"; popd >/dev/null
out=$fieldhome/out/production/

LD_LIBRARY_PATH=$out/fieldwork2/linux64 $java -DappDir=$fieldhome -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Xmx4g -Xms4g -Dglass.disableThreadChecks=true -javaagent:$fieldhome/out/artifacts/fieldagent_jar/fieldagent.jar -cp $java/../../lib/tools.jar:$fieldhome/out/artifacts/fieldagent_jar/fieldagent.jar:$out/fieldwork2/*:$out/fieldwork2/:$out/fielded/:$out/fielded/*:$out/fieldbox/:$out/fieldbox/*:$out/fieldnashorn/*:$out/fieldnashorn/:$out/fieldcef/*:$out/fieldcef/ -Djava.library.path=$out/fieldbox/:$out/fieldwork2/linux64/ fieldagent.Trampoline ${*}
