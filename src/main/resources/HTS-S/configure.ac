# Process this file with autoconf to produce a configure script.
# Process this file with autoconf to produce a configure script.
# Process this file with autoconf to produce a configure script.
AC_INIT(HTS-demo_CMU-ARCTIC-SLT_STRAIGHT,2.2,hts-users@sp.nitech.ac.jp)
AC_COPYRIGHT(Copyright 2001-2011 Nagoya Institute of Technology)
AC_COPYRIGHT(Copyright 2001-2008 Tokyo Institute of Technology)



# Checks for programs.
# awk
AC_PROG_AWK



# perl
AC_PATH_PROG([PERL], [perl], [/usr/bin], [$PATH:/usr/bin])
if test ! -f "$PERL"; then
	AC_MSG_ERROR(Can't find perl)
fi



# bc
AC_PATH_PROG([BC], [bc], [/usr/bin], [$PATH:/usr/bin])
if test ! -f "$BC"; then
	AC_MSG_ERROR(Can't find bc)
fi

# wc
AC_PATH_PROG([WC], [wc], [/usr/bin], [$PATH:/usr/bin])
if test ! -f "$WC"; then
	AC_MSG_ERROR(Can't find wc)
fi

# tee
AC_PATH_PROG([TEE], [tee], [/usr/bin], [$PATH:/usr/bin])
if test ! -f "$TEE"; then
	AC_MSG_ERROR(Can't find tee)
fi



# festival
# festival search path
AC_ARG_WITH(fest-search-path,
		AS_HELP_STRING([--with-fest-search-path],[specify the additional festival search path]),
		[fest_search_path=$withval])



# SPTK commands
# SPTK search path
AC_ARG_WITH(sptk-search-path,
		AS_HELP_STRING([--with-sptk-search-path],[specify the additional SPTK binary search path]),
		[sptk_search_path=$withval])

# x2x
AC_PATH_PROG([X2X], [x2x], [:], [${sptk_search_path}:$PATH])
if test ! -f "$X2X"; then
	AC_MSG_ERROR(Can't find x2x of SPTK)
fi

# mgcep
if test -z "$GAMMA"; then
	AC_PATH_PROG([MGCEP], [mcep], [:], [${sptk_search_path}:$PATH])
	if test ! -f "$MGCEP"; then
		AC_MSG_ERROR(Can't find mcep of SPTK)
	fi
elif test $GAMMA -eq 0; then
	AC_PATH_PROG([MGCEP], [mcep], [:], [${sptk_search_path}:$PATH])
	if test ! -f "$MGCEP"; then
		AC_MSG_ERROR(Can't find mcep of SPTK)
	fi
else
	AC_PATH_PROG([MGCEP], [mgcep], [:], [${sptk_search_path}:$PATH])
	if test ! -f "$MGCEP"; then
		AC_MSG_ERROR(Can't find mgcep of SPTK)
	fi
fi

# lpc2lsp
AC_PATH_PROG([LPC2LSP], [lpc2lsp], [:], [${sptk_search_path}:$PATH])
if test ! -f "$LPC2LSP"; then
	AC_MSG_ERROR(Can't find lpc2lsp of SPTK)
fi

# nan
AC_PATH_PROG([NAN], [nan], [:], [${sptk_search_path}:$PATH])
if test ! -f "$NAN"; then
	AC_MSG_ERROR(Can't find nan of SPTK)
fi

# bcut
AC_PATH_PROG([BCUT], [bcut], [:], [${sptk_search_path}:$PATH])
if test ! -f "$BCUT"; then
	AC_MSG_ERROR(Can't find bcut of SPTK)
fi

# vstat
AC_PATH_PROG([VSTAT], [vstat], [:], [${sptk_search_path}:$PATH])
if test ! -f "$VSTAT"; then
	AC_MSG_ERROR(Can't find vstat of SPTK)
fi

# sopr
AC_PATH_PROG([SOPR], [sopr], [:], [${sptk_search_path}:$PATH])
if test ! -f "$SOPR"; then
	AC_MSG_ERROR(Can't find sopr of SPTK)
fi

# vopr
AC_PATH_PROG([VOPR], [vopr], [:], [${sptk_search_path}:$PATH])
if test ! -f "$VOPR"; then
	AC_MSG_ERROR(Can't find vopr of SPTK)
fi

# freqt
AC_PATH_PROG([FREQT], [freqt], [:], [${sptk_search_path}:$PATH])
if test ! -f "$FREQT"; then
	AC_MSG_ERROR(Can't find freqt of SPTK)
fi

# c2acr
AC_PATH_PROG([C2ACR], [c2acr], [:], [${sptk_search_path}:$PATH])
if test ! -f "$C2ACR"; then
	AC_MSG_ERROR(Can't find c2acr of SPTK)
fi

# mc2b
AC_PATH_PROG([MC2B], [mc2b], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MC2B"; then
	AC_MSG_ERROR(Can't find mc2b of SPTK)
fi

# b2mc
AC_PATH_PROG([B2MC], [b2mc], [:], [${sptk_search_path}:$PATH])
if test ! -f "$B2MC"; then
	AC_MSG_ERROR(Can't find b2mc of SPTK)
fi

# merge
AC_PATH_PROG([MERGE], [merge], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MERGE"; then
	AC_MSG_ERROR(Can't find merge of SPTK)
fi

# average
AC_PATH_PROG([AVERAGE], [average], [:], [${sptk_search_path}:$PATH])
if test ! -f "$AVERAGE"; then
	AC_MSG_ERROR(Can't find average of SPTK)
fi

# bcp
AC_PATH_PROG([BCP], [bcp], [:], [${sptk_search_path}:$PATH])
if test ! -f "$BCP"; then
	AC_MSG_ERROR(Can't find bcp of SPTK)
fi

# lsp2lpc
AC_PATH_PROG([LSP2LPC], [lsp2lpc], [:], [${sptk_search_path}:$PATH])
if test ! -f "$LSP2LPC"; then
	AC_MSG_ERROR(Can't find lsp2lpc of SPTK)
fi

# lspcheck
AC_PATH_PROG([LSPCHECK], [lspcheck], [:], [${sptk_search_path}:$PATH])
if test ! -f "$LSPCHECK"; then
	AC_MSG_ERROR(Can't find lspcheck of SPTK)
fi

# mgc2mgc
AC_PATH_PROG([MGC2MGC], [mgc2mgc], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MGC2MGC"; then
	AC_MSG_ERROR(Can't find mgc2mgc of SPTK)
fi

# mgc2sp
AC_PATH_PROG([MGC2SP], [mgc2sp], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MGC2SP"; then
	AC_MSG_ERROR(Can't find mgc2sp of SPTK)
fi

# interpolate
AC_PATH_PROG([INTERPOLATE], [interpolate], [:], [${sptk_search_path}:$PATH])
if test ! -f "$INTERPOLATE"; then
	AC_MSG_ERROR(Can't find interpolate of SPTK)
fi

# dfs
AC_PATH_PROG([DFS], [dfs], [:], [${sptk_search_path}:$PATH])
if test ! -f "$DFS"; then
	AC_MSG_ERROR(Can't find dfs of SPTK)
fi

# minmax
AC_PATH_PROG([MINMAX], [minmax], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MINMAX"; then
	AC_MSG_ERROR(Can't find minmax of SPTK)
fi



# sox (Sound eXchange)
# sox search path
AC_ARG_WITH(sox-search-path,
		AS_HELP_STRING([--with-sox-search-path],[specify the additional sox (Sound eXchange) search path]),
		[sox_search_path=$withval])

# sox
AC_PATH_PROG([SOX], [sox], [:], [${sox_search_path}:$PATH])
if test ! -f "$SOX"; then
	AC_MSG_ERROR(Can't find sox)
fi

# sox option
$SOX -h > sox.option 2>&1
soxversion=`head -1 sox.option | sed -e s/v//g | awk '{printf "%d\n",$NF}'`
if test $soxversion -gt 13; then
	SOXOPTION=2
else
	SOXOPTION=w
fi
rm -f sox.option
AC_SUBST(SOXOPTION)



# MATLAB
# MATLAB search path
AC_ARG_WITH(matlab-search-path,
		AS_HELP_STRING([--with-matlab-search-path],[specify the additional MATLAB binary search path]),
		[matlab_search_path=$withval])

AC_PATH_PROG([MATLAB], [matlab], [:], [${matlab_search_path}:$PATH])
if test ! -f "$MATLAB"; then
	AC_MSG_ERROR(Can't find MATLAB)
fi
MATLAB="$MATLAB -nodisplay -nosplash -nojvm"



# STRAIGHT
# STRAIGHTpath
AC_ARG_WITH(straight-path,
                AS_HELP_STRING([--with-straight-path],[specify the STRAIGHT MATLAB code path]),
                [STRAIGHT=$withval])
if test ! -f "$STRAIGHT/exstraightsource.p" -a ! -f "$STRAIGHT/exstraightsource.m"; then
	AC_MSG_ERROR(Can't find exstraightsource in $STRAIGHT)
fi
if test ! -f "$STRAIGHT/exstraightspec.p"   -a ! -f "$STRAIGHT/exstraightspec.m";   then
	AC_MSG_ERROR(Can't find exstraightspec in $STRAIGHT)
fi
if test ! -f "$STRAIGHT/exstraightsynth.p"  -a ! -f "$STRAIGHT/exstraightsynth.m";  then
	AC_MSG_ERROR(Can't find exstraightsynth in $STRAIGHT)
fi
AC_SUBST(STRAIGHT)



# HTS commands
# HTS search path
AC_ARG_WITH(hts-search-path,
		AS_HELP_STRING([--with-hts-search-path],[specify the additional HTS binary search path]),
		[hts_search_path=$withval])

# HMGenS
AC_PATH_PROG([HMGENS], [HMGenS], [:], [${hts_search_path}:$PATH])
if test ! -f "$HMGENS"; then
	AC_MSG_ERROR(Can't find HMGenS of HTS)
fi

# HCompV
AC_PATH_PROG([HCOMPV], [HCompV], [:], [${hts_search_path}:$PATH])
if test ! -f "$HCOMPV"; then
	AC_MSG_ERROR(Can't find HCompV of HTS)
fi

# HList
AC_PATH_PROG([HLIST],  [HList],  [:], [${hts_search_path}:$PATH])
if test ! -f "$HLIST"; then
	AC_MSG_ERROR(Can't find HList of HTS)
fi

# HInit
AC_PATH_PROG([HINIT],  [HInit],  [:], [${hts_search_path}:$PATH])
if test ! -f "$HINIT"; then
	AC_MSG_ERROR(Can't find HInit of HTS)
fi

# HRest
AC_PATH_PROG([HREST],  [HRest],  [:], [${hts_search_path}:$PATH])
if test ! -f "$HREST"; then
	AC_MSG_ERROR(Can't find HRest of HTS)
fi

# HERest
AC_PATH_PROG([HEREST], [HERest], [:], [${hts_search_path}:$PATH])
if test ! -f "$HEREST"; then
	AC_MSG_ERROR(Can't find HERest of HTS)
fi

# HHEd
AC_PATH_PROG([HHED],   [HHEd],   [:], [${hts_search_path}:$PATH])
if test ! -f "$HHED"; then
	AC_MSG_ERROR(Can't find HHEd of HTS)
fi

# HSMMAlign
AC_PATH_PROG([HSMMALIGN],   [HSMMAlign],   [:], [${hts_search_path}:$PATH])
if test ! -f "$HSMMALIGN"; then
	AC_MSG_ERROR(Can't find HSMMAlign of HTS)
fi



# hts_engine API
# hts_engine search path
AC_ARG_WITH(hts-engine-search-path,
		AS_HELP_STRING([--with-hts-engine-search-path],[specify the additional hts_engine binary search path]),
		[hts_engine_search_path=$withval])

# hts_engine
AC_PATH_PROG([ENGINE], [hts_engine], [:], [${hts_engine_search_path}:$PATH])
if test ! -f "$ENGINE"; then
	AC_MSG_ERROR(Can't find hts_engine)
fi



# speaker and dataset names
AC_ARG_VAR([SPEAKER],[speaker name (default=slt)])
AC_ARG_VAR([DATASET],[dataset (default=cmu_us_arctic)])

if test -z "$SPEAKER"; then
	SPEAKER=slt
fi

if test -z "$DATASET"; then
	DATASET=cmu_us_arctic
fi

AC_SUBST(SPEAKER)
AC_SUBST(DATASET)



# version number and question set name
AC_ARG_VAR([VER],[version number of this setting (default=1)])
AC_ARG_VAR([QNUM],[question set number (default='001')])

if test -z "$VER"; then
	VER=1
fi

if test -z "$QNUM"; then
	QNUM='001'
fi

AC_SUBST(VER)
AC_SUBST(QNUM)



# current directory
AC_SUBST(PWD)



# setting of mel-generalized cepstral analysis, F0 extraction, and postfiltering related settings
# parse command-line arguments
AC_ARG_VAR([FRAMESHIFT],[Frame shift in point (default=240)])
AC_ARG_VAR([FREQWARP],[Frequency warping factor (default=0.55)])
AC_ARG_VAR([GAMMA],[Pole/Zero weight factor (0: mel-cepstral analysis  1: LPC analysis  2,3,...,N: mel-generalized cepstral (MGC) analysis) (default=0)])
AC_ARG_VAR([MGCORDER],[Order of MGC analysis (default=49)])
AC_ARG_VAR([LNGAIN],[Use logarithmic gain instead of linear gain (default=1)])
AC_ARG_VAR([LOWERF0],[Lower limit for F0 extraction in Hz (default=110)])
AC_ARG_VAR([UPPERF0],[Upper limit for F0 extraction in Hz (default=280)])
AC_ARG_VAR([PSTFILTER],[Postfiltering factor (default=1.4)])
AC_ARG_VAR([IMPLEN],[Length of impulse response (default=4096)])
AC_ARG_VAR([SAMPFREQ],[Sampling frequency in Hz (default=48000)])

# default values
if test -z "$FRAMESHIFT"; then
	FRAMESHIFT=240
fi

if test -z "$SAMPFREQ"; then
	SAMPFREQ=48000
fi

if test -z "$FREQWARP"; then
	case $SAMPFREQ in
		8000)  FREQWARP=0.31 ;;
		10000) FREQWARP=0.35 ;;
		12000) FREQWARP=0.37 ;;
		16000) FREQWARP=0.42 ;;
		22050) FREQWARP=0.45 ;;
		32000) FREQWARP=0.45 ;;
		44100) FREQWARP=0.53 ;;
		48000) FREQWARP=0.55 ;;
		*)     FREQWARP=0.00 ;;
	esac
fi

if test -z "$GAMMA"; then
	GAMMA=0
fi

if test -z "$MGCORDER"; then
	MGCORDER=49
fi

if test -z "$LNGAIN"; then
	LNGAIN=1
fi

if test -z "$LOWERF0"; then
	LOWERF0=110
fi 

if test -z "$UPPERF0"; then
	UPPERF0=280
fi 

if test -z "$PSTFILTER"; then
	PSTFILTER=1.4
fi

if test -z "$IMPLEN"; then
	IMPLEN=4096
fi

MGCVSIZE=`expr $MGCORDER + 1`

if test $GAMMA -lt 0; then
	AC_MSG_ERROR(Cannot use MGC coefficients if GAMMA<0!)
fi



# substitute variables
AC_SUBST(FRAMESHIFT)
AC_SUBST(FREQWARP)
AC_SUBST(GAMMA)
AC_SUBST(MGCORDER)
AC_SUBST(LOWERF0)
AC_SUBST(UPPERF0)
AC_SUBST(PSTFILTER)
AC_SUBST(IMPLEN)
AC_SUBST(SAMPFREQ)
AC_SUBST(MGCVSIZE)



# number of windows
# parse command-line arguments
AC_ARG_VAR([NMGCWIN],[number of delta windows for MGC coefficients (default=3)])
AC_ARG_VAR([NLF0WIN],[number of delta windows for log F0 values (default=3)])
AC_ARG_VAR([NBAPWIN],[number of delta windows for band aperiodicity values (default=3)])

# default values
if test -z "$NMGCWIN"; then
	NMGCWIN=3
fi

if test -z "$NLF0WIN"; then
	NLF0WIN=3
fi

if test -z "$NBAPWIN"; then
	NBAPWIN=3
fi

F0STREN=`expr $NLF0WIN + 1`
BAPSTREN=`expr $F0STREN + 1`

# substitute variables
AC_SUBST(NMGCWIN)
AC_SUBST(NLF0WIN)
AC_SUBST(NBAPWIN)
AC_SUBST(F0STREN)
AC_SUBST(BAPSTREN)



# model training settings
# parse command-line arguments
AC_ARG_VAR([NSTATE],[number of HMM states (default=5)])
AC_ARG_VAR([NITER],[number of iterations of embedded training (default=5)])
AC_ARG_VAR([WFLOOR],[mixture weight flooring scale (default=5000)])
AC_ARG_VAR([DAEM],[DAEM algorithm for parameter estimation (0:off or 1:on, default=0)])
AC_ARG_VAR([DAEMNITER],[number of iterations of DAEM-based embedded training (default=10)])
AC_ARG_VAR([DAEMALPHA],[schedule of updating temperature parameter for DAEM (default=1.0)])
AC_ARG_VAR([MAXDEV],[maximum duration scaling factor (default=10)])
AC_ARG_VAR([MINDUR],[minimum duration to be evaluated (default=5)])
AC_ARG_VAR([NMGCTRANSBLK],[number of blocks for MGC transforms (default=3)])
AC_ARG_VAR([NLF0TRANSBLK],[number of blocks for log F0 transforms (default=1)])
AC_ARG_VAR([NBAPTRANSBLK],[number of blocks for band aperiodicity transforms (default=3)])
AC_ARG_VAR([MGCBANDWIDTH],[band width for MGC transforms (default=50 for cepstral form, derault=2 for LSP form)])
AC_ARG_VAR([LF0BANDWIDTH],[band width for log F0 transforms (default=1)])
AC_ARG_VAR([BAPBANDWIDTH],[band width for band aperiodicity transforms (default=26)])

# default values
if test -z "$NSTATE"; then
	NSTATE=5
fi

if test -z "$NITER"; then
	NITER=5
fi

if test -z "$WFLOOR"; then
	WFLOOR=5000
fi

if test -z "$DAEM"; then
	DAEM=0
fi

if test -z "$DAEMNITER"; then
	DAEMNITER=10
fi

if test -z "$DAEMALPHA"; then
	DAEMALPHA=1.0
fi

if test -z "$MAXDEV"; then
	MAXDEV=10
fi

if test -z "$MINDUR"; then
	MINDUR=5
fi

if test -z "$NMGCTRANSBLK"; then
	NMGCTRANSBLK=3
fi

if test -z "$NLF0TRANSBLK"; then
	NLF0TRANSBLK=1
fi

if test -z "$NBAPTRANSBLK"; then
	NBAPTRANSBLK=3
fi

if test -z "$MGCBANDWIDTH"; then
	if test $GAMMA -eq 0; then
		MGCBANDWIDTH=$MGCVSIZE
	else
		MGCBANDWIDTH=2
	fi
fi

if test -z "$LF0BANDWIDTH"; then
	LF0BANDWIDTH=1
fi

if test -z "$BAPBANDWIDTH"; then
	BAPBANDWIDTH=26
fi

# substitute variables
AC_SUBST(NSTATE)
AC_SUBST(NITER)
AC_SUBST(WFLOOR)
AC_SUBST(DAEM)
AC_SUBST(DAEMNITER)
AC_SUBST(DAEMALPHA)
AC_SUBST(MAXDEV)
AC_SUBST(MINDUR)
AC_SUBST(NMGCTRANSBLK)
AC_SUBST(NLF0TRANSBLK)
AC_SUBST(NBAPTRANSBLK)
AC_SUBST(MGCBANDWIDTH)
AC_SUBST(LF0BANDWIDTH)
AC_SUBST(BAPBANDWIDTH)


# parameter generation algorithm settings
# parse command-line arguments
AC_ARG_VAR([MAXEMITER],[max EM iteration (default=20)])
AC_ARG_VAR([EMEPSILON],[convergence factor for EM iteration (default=0.0001)])
AC_ARG_VAR([USEGV],[turn on GV-based parameter generation algorithm (0:off or 1:on, default=1)])
AC_ARG_VAR([MAXGVITER],[maximum number of iterations of GV-based parameter generation algorithm (default=50)])
AC_ARG_VAR([GVEPSILON],[convergence factor for GV iteration (default=0.0001)])
AC_ARG_VAR([MINEUCNORM],[minimum Euclid norm for GV iteration (default=0.01)])
AC_ARG_VAR([STEPINIT],[initial step size (default=1.0)])
AC_ARG_VAR([STEPINC],[step size acceleration factor (default=1.2)])
AC_ARG_VAR([STEPDEC],[step size deceleration factor (default=0.5)])
AC_ARG_VAR([HMMWEIGHT],[weight for HMM output prob. (default=1.0)])
AC_ARG_VAR([GVWEIGHT],[weight for GV output prob. (default=1.0)])
AC_ARG_VAR([OPTKIND],[optimization method (STEEPEST, NEWTON, or LBFGS) (default=NEWTON)])
AC_ARG_VAR([NOSILGV],[turn on GV without silent and pause phoneme (0:off or 1:on, default=1)])
AC_ARG_VAR([CDGV],[turn on context-dependent GV (0:off or 1:on, default=1)])

# default values
if test -z "$MAXEMITER"; then
	MAXEMITER=20
fi

if test -z "$EMEPSILON"; then
	EMEPSILON=0.0001
fi

if test -z "$USEGV"; then
	USEGV=1
fi

if test -z "$MAXGVITER"; then
	MAXGVITER=50
fi

if test -z "$GVEPSILON"; then
	GVEPSILON=0.0001
fi

if test -z "$MINEUCNORM"; then
	MINEUCNORM=0.01
fi

if test -z "$STEPINIT"; then
	STEPINIT=1.0
fi

if test -z "$STEPINC"; then
	STEPINC=1.2
fi

if test -z "$STEPDEC"; then
	STEPDEC=0.5
fi

if test -z "$HMMWEIGHT"; then
	HMMWEIGHT=1.0
fi

if test -z "$GVWEIGHT"; then
	GVWEIGHT=1.0
fi

if test -z "$OPTKIND"; then
	OPTKIND=NEWTON
fi

if test -z "$NOSILGV"; then
	NOSILGV=1
fi

if test -z "$CDGV"; then
	CDGV=1
fi

# substitute variables
AC_SUBST(MAXEMITER)
AC_SUBST(EMEPSILON)
AC_SUBST(USEGV)
AC_SUBST(MAXGVITER)
AC_SUBST(GVEPSILON)
AC_SUBST(MINEUCNORM)
AC_SUBST(STEPINIT)
AC_SUBST(STEPINC)
AC_SUBST(STEPDEC)
AC_SUBST(HMMWEIGHT)
AC_SUBST(GVWEIGHT)
AC_SUBST(OPTKIND)
AC_SUBST(NOSILGV)
AC_SUBST(CDGV)



# output data/Makefile and scripts/Config.pm
AC_OUTPUT(data/Makefile scripts/Config.pm Makefile)
