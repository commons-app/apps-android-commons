#!/bin/sh
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore /home/nico/d/d/wya/nr-commons.keystore commons/target/commons-1.0-SNAPSHOT.apk nrkeystorealias
zipalign -f -v 4 commons/target/commons-1.0-SNAPSHOT.apk commons/target/commons-1.0-SNAPSHOT_signed.apk
