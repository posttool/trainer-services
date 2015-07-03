#!/usr/bin/env bash

BASE=/Users/posttool/Documents/github/la/deploy
ROOT=$BASE/install
PATH=$BASE/bin:$PATH

################# HTK
cd $ROOT
cp /Users/posttool/Documents/github/adapt2/deploy/HTK-3.4.1.tar.gz .
cp /Users/posttool/Documents/github/adapt2/deploy/HDecode-3.4.1.tar.gz .
mkdir -p HTS-patch
cd HTS-patch
wget http://hts.sp.nitech.ac.jp/archives/2.2/HTS-2.2_for_HTK-3.4.1.tar.bz2
tar -jxvf HTS-2.2_for_HTK-3.4.1.tar.bz2
cd ..
tar -zxf HTK-3.4.1.tar.gz
tar -zxf HDecode-3.4.1.tar.gz
cd htk
cp $ROOT/HTS-patch/HTS-2.2_for_HTK-3.4.1.patch .
patch -p1 -d . < HTS-2.2_for_HTK-3.4.1.patch
# replace HTKLib/strarr.c <malloc.h> with <stdlib.h>
./configure -build=i686-apple-macos LDFLAGS=-L/opt/X11/lib CFLAGS='-I/opt/X11/include -I/usr/include/malloc -DARCH=\"darwin\"' --prefix=$ROOT MAXSTRLEN=2048
make
make install
make hdecode
make install-hdecode
#
#
#
cd $ROOT
wget http://downloads.sourceforge.net/hts-engine/hts_engine_API-1.09.tar.gz
tar -zxf hts_engine_API-1.09.tar.gz
cd hts_engine_API-1.09
./configure --prefix=$ROOT
make
make install









# speech tools
cd $ROOT
wget http://festvox.org/packed/festival/2.4/speech_tools-2.4-release.tar.gz
tar xvfz speech_tools-2.4-release.tar.gz
cd speech_tools
./configure
make

