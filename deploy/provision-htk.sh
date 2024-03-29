#!/usr/bin/env bash

BASE=/Users/david/la/deploy
ROOT=$BASE/install
PATH=$BASE/bin:$PATH
HTK=HTK-3.4.1
HTS_VERSION=2.3beta
HTS=HTS-${HTS_VERSION}
H4H=${HTS}_for_${HTK}
H4H_URL=http://hts.sp.nitech.ac.jp/archives/${HTS_VERSION}/${H4H}.tar.bz2
#http://hts.sp.nitech.ac.jp/archives/2.3beta/HTS-2.3beta_for_HTK-3.4.1.tar.bz2
AR=ar-for-hts-0.8_for_${HTS}
AR_URL=http://mi.eng.cam.ac.uk/research/emime/ar-for-hts/archive/${AR}.tar.gz
################# HTK
echo "$HTK $HTS"
cd $BASE
mkdir $ROOT
cd $ROOT

rm -rf htk
tar -zxf ../${HTK}.tar.gz
tar -zxf ../HDecode-3.4.1.tar.gz

rm -rf htk-patches
mkdir -p htk-patches
cd htk-patches
wget ${H4H_URL}
tar -jxvf ${H4H}.tar.bz2
#wget ${AR_URL}
#tar -zxf ${AR}.tar.gz

cd ../htk
cp ../htk-patches/${H4H}.patch .
patch -p1 -d . < ${H4H}.patch
#cp ../htk-patches/${AR}/${AR}.patch .
#patch -p1 -d . < ${AR}.patch

# dk adjust for os x
sed 's/malloc.h/stdlib.h/' <HTKLib/strarr.c >strarr.c
cp strarr.c HTKLib/strarr.c
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




#SPTK
cd $ROOT
tar -zxvf ../SPTK-3.4.1.tar.gz
cd SPTK-3.4.1
./configure
make
sudo make install



# speech tools
cd $ROOT
wget http://festvox.org/packed/festival/2.4/speech_tools-2.4-release.tar.gz
tar xvfz speech_tools-2.4-release.tar.gz
cd speech_tools
./configure
make

