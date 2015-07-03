#!/usr/bin/env bash

cd /Users/posttool/Documents/github/la/deploy/install

#open fst
wget http://openfst.org/twiki/pub/FST/FstDownload/openfst-1.4.1.tar.gz
tar -xvzf openfst-1.4.1.tar.gz
cd openfst-1.4.1
./configure --enable-compact-fsts --enable-const-fsts --enable-far --enable-lookahead-fsts --enable-pdt --enable-ngram-fsts
sudo make install
cd ..
#open ngram
wget http://openfst.cs.nyu.edu/twiki/pub/GRM/NGramDownload/opengrm-ngram-1.2.1.tar.gz
tar -xvzf opengrm-ngram-1.2.1.tar.gz
cd opengrm-ngram-1.2.1
export CPPFLAGS='-I/usr/local/include'
./configure
sudo make install
cd ..
# phonetsaurus
#git clone https://github.com/AdolfVonKleist/Phonetisaurus.git
#cd Phonetisaurus/src
# export CPPFLAGS='-I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.10.sdk/usr/include/c++/4.2.1 -I/usr/local/include'
#make -j 4
#sudo make install
#cd ..
#sudo python setup.py install
#export LD_LIBRARY_PATH=/usr/local/lib
#cd script/
#wget https://www.dropbox.com/s/vlmlfq52rpbkniv/cmu-eg.me-kn.8g.arpa.gz?dl=0 -O test.arpa.gz
#gunzip test.arpa.gz
#phonetisaurus-arpa2wfst-omega --lm=test.arpa --ofile=test.fst
# see https://github.com/AdolfVonKleist/Phonetisaurus

git clone https://github.com/AdolfVonKleist/RnnLMG2P.git
cd RnnLMG2P/src
make
make install
