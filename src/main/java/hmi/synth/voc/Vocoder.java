package hmi.synth.voc;

import hmi.sig.AmplitudeNormalizer;
import hmi.sig.DDSAudioInputStream;
import hmi.sig.FFT;
import hmi.sig.ProducingDoubleDataSource;

import java.util.Arrays;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

public class Vocoder {

    public static final int IPERIOD = 1; /* interpolation period */
    public static final int SEED = 1;

    public static final double ZERO = 1.0e-10; /* ~(0) */
    public static final double LZERO = (-1.0e+10); /* ~log(0) */

   

    private Random rand;
    private int stage; /* Gamma=-1/stage : if stage=0 then Gamma=0 */
    private double gamma; /* Gamma */
    private boolean use_log_gain; /* log gain flag (for LSP) */
    private int fprd; /* frame shift */
    private double p1; /* used in excitation generation */
    private double pc; /* used in excitation generation */

    private double C[]; /* used in the MLSA/MGLSA filter */
    private double CC[]; /* used in the MLSA/MGLSA filter */
    private double CINC[]; /* used in the MLSA/MGLSA filter */
    private double D1[]; /* used in the MLSA/MGLSA filter */

    private double rate;
    int pt2; /* used in mlsadf2 */
    private final int pt3[] = new int[CUtils.PADEORDER + 1]; /* used in mlsadf2 */

    /* mixed excitation variables */
    private int numM; /* Number of bandpass filters for mixed excitation */
    private int orderM; /* Order of filters for mixed excitation */
    private double h[][]; /* filters for mixed excitation */
    private double xpulseSignal[]; /* the size of this should be orderM */
    private double xnoiseSignal[]; /* the size of this should be orderM */
    private boolean mixedExcitation = false;
    private boolean fourierMagnitudes = false;

    /**
     * The initialisation of VocoderSetup should be done when there is already
     * information about the number of feature vectors to be processed, size of
     * the mcep vector file, etc.
     */
    private void initVocoder(int mcep_order, int mcep_vsize, PData htsData) {

        stage = htsData.getStage();
        gamma = htsData.getGamma();
        use_log_gain = htsData.getUseLogGain();

        fprd = htsData.getFperiod();
        rate = htsData.getRate();

        rand = new Random(SEED);

        C = new double[mcep_order];
        CC = new double[mcep_order];
        CINC = new double[mcep_order];

        int PADEORDER = CUtils.PADEORDER;
        if (stage == 0) { /* for MGC */

            /* mcep_order=74 and pd=PADEORDER=5 (if no HTS_EMBEDDED is used) */
            int vector_size = (mcep_vsize * (3 + PADEORDER) + 5 * PADEORDER + 6) - (3 * (mcep_order));
            D1 = new double[vector_size];

            pt2 = (2 * (PADEORDER + 1)) + (PADEORDER * (mcep_order + 1));

            for (int i = PADEORDER; i >= 1; i--)
                pt3[i] = (2 * (PADEORDER + 1)) + ((i - 1) * (mcep_order + 1));

        } else { /* for LSP */
            int vector_size = ((mcep_vsize + 1) * (stage + 3)) - (3 * (mcep_order));
            D1 = new double[vector_size];
        }

        /* excitation initialisation */
        p1 = -1;
        pc = 0.0;

    } /* method initVocoder */

    /**
     * HTS_MLSA_Vocoder: Synthesis of speech out of mel-cepstral coefficients.
     * This procedure uses the parameters generated in pdf2par stored in:
     * PStream mceppst: Mel-cepstral coefficients PStream strpst : Filter bank
     * stregths for mixed excitation PStream magpst : Fourier magnitudes PStream
     * lf0pst : Log F0
     */
    public AudioInputStream htsMLSAVocoder(ParameterGenerator pdf2par, PData htsData) throws Exception {

        int audioSize = computeAudioSize(pdf2par.getMcepPst(), htsData);
        HTSVocoderDataProducer producer = new HTSVocoderDataProducer(audioSize, pdf2par, htsData);
        producer.start();
        return new DDSAudioInputStream(producer, getHTSAudioFormat(htsData));

        /*
         * double [] audio_double = null;
         * 
         * audio_double = htsMLSAVocoder(pdf2par.getlf0Pst(),
         * pdf2par.getMcepPst(), pdf2par.getStrPst(), pdf2par.getMagPst(),
         * pdf2par.getVoicedArray(), htsData);
         * 
         * long lengthInSamples = (audio_double.length * 2 ) /
         * (sampleSizeInBits/8); System.out.println("length in samples=" +
         * lengthInSamples );
         * 
         * // Normalise the signal before return, this will normalise between 1
         * and -1 double MaxSample = MathUtils.getAbsMax(audio_double); for (int
         * i=0; i<audio_double.length; i++) audio_double[i] = ( audio_double[i]
         * / MaxSample ); //audio_double[i] = 0.3 * ( audio_double[i] /
         * MaxSample );
         * 
         * return new DDSAudioInputStream(new
         * BufferedDoubleDataSource(audio_double), af);
         */
    }

    public static AudioFormat getHTSAudioFormat(PData htsData) {
        float sampleRate = htsData.getRate(); // 8000,11025,16000,22050,44100,48000
        int sampleSizeInBits = 16; // 8,16
        int channels = 1; // 1,2
        boolean signed = true; // true,false
        boolean bigEndian = false; // true,false
        AudioFormat af = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        return af;
    }

    public double[] htsMLSAVocoder(PStream lf0Pst, PStream mcepPst, PStream strPst, PStream magPst,
            boolean[] voiced, PData htsData, HTSVocoderDataProducer audioProducer) throws Exception {

        double inc, x, MaxSample;
        double xp = 0.0, xn = 0.0, fxp, fxn, mix; /*
                                                   * samples for pulse and for
                                                   * noise and the filtered ones
                                                   */
        int k, m, mcepframe, lf0frame;
        double alpha = htsData.getAlpha();
        double beta = htsData.getBeta();
        double[] magPulse = null; /* pulse generated from Fourier magnitudes */
        int magSample, magPulseSize;

        double f0Std, f0Shift, f0MeanOri;
        double hp[] = null; /*
                             * pulse shaping filter, it is initd once it is
                             * known orderM
                             */
        double hn[] = null; /*
                             * noise shaping filter, it is initd once it is
                             * known orderM
                             */

        /*
         * init vocoder and mixed excitation, once initd it is known the order
         * of the filters so the shaping filters hp and hn can be initd.
         */
        m = mcepPst.getOrder();
        initVocoder(m, mcepPst.getVsize() - 1, htsData);
        double pulse[] = new double[fprd];
        double noise[] = new double[fprd];
        double source[] = new double[fprd];

        double[] d = new double[m];
        mixedExcitation = htsData.getUseMixExc();
        fourierMagnitudes = htsData.getUseFourierMag();

        if (mixedExcitation && htsData.getPdfStrStream() != null) {
            numM = htsData.getNumFilters();
            orderM = htsData.getOrderFilters();

            xpulseSignal = new double[orderM];
            xnoiseSignal = new double[orderM];
            /* init xp_sig and xn_sig */// -> automatically initialized to
                                        // 0.0

            h = htsData.getMixFilters();
            hp = new double[orderM];
            hn = new double[orderM];

            // Check if the number of filters is equal to the order of strpst
            // i.e. the number of filters is equal to the number of generated
            // strengths per frame.
            if (numM != strPst.getOrder()) {
                System.out.println("htsMLSAVocoder: error num mix-excitation filters =" + numM
                        + " in configuration file is different from generated str order=" + strPst.getOrder());
                throw new Exception("htsMLSAVocoder: error num mix-excitation filters = " + numM
                        + " in configuration file is different from generated str order=" + strPst.getOrder());
            }
            System.out.println("HMM speech generation with mixed-excitation.");
        } else
            System.out.println("HMM speech generation without mixed-excitation.");

        if (fourierMagnitudes && htsData.getPdfMagStream() != null)
            System.out.println("Pulse generated with Fourier Magnitudes.");
        // else
        // logger.info("Pulse generated as a unit pulse.");

        if (beta != 0.0)
            System.out.println("Postfiltering applied with beta=" + beta);
        else
            System.out.println("No postfiltering applied.");

        f0Std = htsData.getF0Std();
        f0Shift = htsData.getF0Mean();
        f0MeanOri = 0.0;

        for (mcepframe = 0, lf0frame = 0; mcepframe < mcepPst.getT(); mcepframe++) {
            if (voiced[mcepframe]) {
                f0MeanOri = f0MeanOri + Math.exp(lf0Pst.getPar(lf0frame, 0));
                // System.out.println("voiced t=" + mcepframe + "  " +
                // lf0Pst.getPar(lf0frame, 0) + "  ");
                lf0frame++;
            }
            // else
            // System.out.println("unvoiced t=" + mcepframe + "  0.0  ");
        }
        f0MeanOri = f0MeanOri / lf0frame;

        /*
         * _______________________Synthesize speech
         * waveforms_____________________
         */
        /* generate Nperiod samples per mcepframe */
        int s = 0; /* number of samples */
        int s_double = 0;
        int audio_size = computeAudioSize(mcepPst, htsData); /*
                                                              * audio size in
                                                              * samples,
                                                              * calculated as
                                                              * num frames *
                                                              * frame period
                                                              */
        double[] audio_double = new double[audio_size]; /*
                                                         * init buffer for audio
                                                         */

        magSample = 1;
        magPulseSize = 0;
        for (mcepframe = 0, lf0frame = 0; mcepframe < mcepPst.getT(); mcepframe++) { /*
                                                                                      * for
                                                                                      * each
                                                                                      * mcep
                                                                                      * frame
                                                                                      */

            /** feature vector for a particular frame */
            double mc[] = new double[m]; /*
                                          * feature vector for a particular
                                          * frame
                                          */
            /* get current feature vector mgc */
            for (int i = 0; i < m; i++)
                mc[i] = mcepPst.getPar(mcepframe, i);

            /* f0 modification */
            double f0 = 0.0;
            if (voiced[mcepframe]) {
                f0 = f0Std * Math.exp(lf0Pst.getPar(lf0frame, 0)) + (1 - f0Std) * f0MeanOri + f0Shift;
                lf0frame++;
                f0 = Math.max(0.0, f0);
            }

            /*
             * if mixed excitation get shaping filters for this frame the
             * strength of pulse, is taken from the predicted value, which can
             * be maximum 1.0, and the strength of noise is the rest -> 1.0 -
             * strPulse
             */
            double str = 0.0;
            if (mixedExcitation) {
                for (int j = 0; j < orderM; j++) {
                    hp[j] = hn[j] = 0.0;
                    for (int i = 0; i < numM; i++) {

                        str = strPst.getPar(mcepframe, i);
                        hp[j] += str * h[i][j];
                        hn[j] += (1 - str) * h[i][j];

                        // hp[j] += strPst.getPar(mcepframe, i) * h[i][j];
                        // hn[j] += ( 0.9 - strPst.getPar(mcepframe, i) ) *
                        // h[i][j];
                    }
                }
            }

            /*
             * f0 -> pitch , in original code here it is used p, so f0=p in the
             * c code
             */
            if (f0 != 0.0)
                f0 = rate / f0;

            /*
             * p1 is initd in -1, so this will be done just for the first frame
             */
            if (p1 < 0) {
                p1 = f0;
                pc = p1;
                /* for LSP */
                if (stage != 0) {
                    C[0] = (use_log_gain) ? LZERO : ZERO;
                    double PI_m = Math.PI / m;
                    for (int i = 0; i < m; i++)
                        C[i] = i * PI_m;
                    /* LSP -> MGC */
                    lsp2mgc(C, C, (m - 1), alpha);
                    CUtils.mc2b(C, C, (m - 1), alpha);
                    CUtils.gnorm(C, C, (m - 1), gamma);
                    for (int i = 1; i < m; i++)
                        C[i] *= gamma;
                }
            }

            if (stage == 0) {
                /* postfiltering, this is done if beta>0.0 */
                CUtils.postfilter_mgc(mc, (m - 1), alpha, beta);
                /*
                 * mc2b: transform mel-cepstrum to MLSA digital filter
                 * coefficients
                 */
                CUtils.mc2b(mc, CC, (m - 1), alpha);
                for (int i = 0; i < m; i++)
                    CINC[i] = (CC[i] - C[i]) * IPERIOD / fprd;
            } else {

                lsp2mgc(mc, CC, (m - 1), alpha);

                CUtils.mc2b(CC, CC, (m - 1), alpha);

                CUtils.gnorm(CC, CC, (m - 1), gamma);

                for (int i = 1; i < m; i++)
                    CC[i] *= gamma;

                for (int i = 0; i < m; i++)
                    CINC[i] = (CC[i] - C[i]) * IPERIOD / fprd;

            }

            /* p=f0 in c code!!! */

            if (p1 != 0.0 && f0 != 0.0) {
                inc = (f0 - p1) * (double) IPERIOD / (double) fprd;
            } else {
                inc = 0.0;
                pc = f0;
                p1 = 0.0;
                // System.out.println("  inc=" + inc + "  ***pc=" + pc + "  p1="
                // + p1);
            }

            /*
             * Here i need to generate both xp:pulse and xn:noise signals
             * separately
             */
            // gauss = false; /* Mixed excitation works better with nomal noise
            // */

            /*
             * Generate fperiod samples per feature vector, normally 80 samples
             * per frame
             */
            // p1=0.0;
            for (int j = fprd - 1, i = (IPERIOD + 1) / 2; j >= 0; j--) {
                if (p1 == 0.0) {

                    x = uniformRand(); /*
                                        * returns 1.0 or -1.0 uniformly
                                        * distributed
                                        */

                    if (mixedExcitation) {
                        xn = x;
                        xp = 0.0;
                    }
                } else {
                    if ((pc += 1.0) >= p1) {
                        if (fourierMagnitudes) {
                            magPulse = CUtils.genPulseFromFourierMag(magPst, mcepframe, p1);
                            magSample = 0;
                            magPulseSize = magPulse.length;
                            x = magPulse[magSample];
                            magSample++;
                        } else
                            x = Math.sqrt(p1);

                        pc = pc - p1;
                    } else {

                        if (fourierMagnitudes) {
                            if (magSample >= magPulseSize) {
                                x = 0.0;
                            } else
                                x = magPulse[magSample];
                            magSample++;
                        } else
                            x = 0.0;
                    }

                    if (mixedExcitation) {
                        xp = x;
                        xn = uniformRand();
                    }
                }
                // System.out.print("    x=" + x);

                /* apply the shaping filters to the pulse and noise samples */
                /* i need memory of at least for M samples in both signals */
                if (mixedExcitation) {
                    fxp = 0.0;
                    fxn = 0.0;
                    for (k = orderM - 1; k > 0; k--) {
                        fxp += hp[k] * xpulseSignal[k];
                        fxn += hn[k] * xnoiseSignal[k];
                        xpulseSignal[k] = xpulseSignal[k - 1];
                        xnoiseSignal[k] = xnoiseSignal[k - 1];
                    }
                    fxp += hp[0] * xp;
                    fxn += hn[0] * xn;
                    xpulseSignal[0] = xp;
                    xnoiseSignal[0] = xn;

                    /* x is a pulse noise excitation and mix is mixed excitation */
                    mix = fxp + fxn;
                    pulse[j] = fxp;
                    noise[j] = fxn;
                    source[j] = mix;
                    // System.out.format("%d = %f \n", j, mix);

                    /*
                     * comment this line if no mixed excitation, just pulse and
                     * noise
                     */
                    x = mix; /* excitation sample */
                }

                if (stage == 0) {
                    if (x != 0.0)
                        x *= Math.exp(C[0]);
                    x = CUtils.mlsadf(x, C, m, alpha, D1, pt2, pt3);

                } else {
                    x *= C[0];
                    x = CUtils.mglsadf(x, C, (m - 1), alpha, stage, D1);
                }

                // System.out.format("%f ", x);
                audio_double[s_double] = x;
                if (audioProducer != null) {
                    audioProducer.putOneDataPoint(x);
                }

                s_double++;

                if ((--i) == 0) {
                    p1 += inc;
                    for (k = 0; k < m; k++) {
                        C[k] += CINC[k];
                    }
                    i = IPERIOD;
                }

            } /* for each sample in a period fprd */

            /*********
             * For debuging if(voiced[mcepframe]) { double magf[] =
             * SignalProcUtils.getFrameHalfMagnitudeSpectrum(source, 512, 1);
             * .plot(magf, "magf"); } System.out.format("str=%.2f\n", str);
             */

            p1 = f0;

            /* move elements in c */
            System.arraycopy(CC, 0, C, 0, m);

        } /* for each mcep frame */

        System.out.println("Finish processing " + mcepframe + " mcep frames.");

        return (audio_double);

    } 

    /**
     * Compute the audio size, in samples, that this vocoder is going to produce
     * for the given data.
     * 
     * @param mcepPst
     * @param htsData
     * @return
     */
    private int computeAudioSize(PStream mcepPst, PData htsData) {
        return mcepPst.getT() * htsData.getFperiod();
    }

    private void printVector(String val, int m, double vec[]) {
        int i;
        System.out.println(val);
        for (i = 0; i < m; i++)
            System.out.println("v[" + i + "]=" + vec[i]);
    }
    
    /** uniform_rand: generate uniformly distributed random numbers 1 or -1 */
    public double uniformRand() {
        return (rand.nextBoolean()) ? 1.0 : -1.0;
    }
    
    

    /** lsp2mgc: transform LSP to MGC. lsp=C[0..m] mgc=C[0..m] */
    public void lsp2mgc(double lsp[], double mgc[], int m, double alpha) {
        /* lsp2lpc */
        CUtils.lsp2lpc(lsp, mgc, m); /* lsp starts in 1! lsp[1..m] --> mgc[0..m] */
        if (use_log_gain)
            mgc[0] = Math.exp(lsp[0]);
        else
            mgc[0] = lsp[0];

        /* mgc2mgc */
        CUtils.ignorm(mgc, mgc, m, gamma);
        for (int i = m; i >= 1; i--)
            mgc[i] *= -stage;
        CUtils.mgc2mgc(mgc, m, alpha, gamma, mgc, m, alpha, gamma); /*
                                                              * input and output
                                                              * is in mgc=C
                                                              */
    }

    private void circularShift(double y[], int T, int n) {

        double x[] = new double[T];
        for (int i = 0; i < T; i++)
            x[i] = y[modShift(i - n, T)];
        for (int i = 0; i < T; i++)
            y[i] = x[i];
    }

    private int modShift(int n, int N) {
        if (n < 0)
            while (n < 0)
                n = n + N;
        else
            while (n >= N)
                n = n - N;

        return n;
    }

    protected class HTSVocoderDataProducer extends ProducingDoubleDataSource {
        private static final double INITIAL_MAX_AMPLITUDE = 17000.;

        private PStream lf0Pst;
        private PStream mcepPst;
        private PStream strPst;
        private PStream magPst;
        private boolean[] voiced;
        private PData htsData;

        public HTSVocoderDataProducer(int audioSize, ParameterGenerator pdf2par, PData htsData) {
            super(audioSize, new AmplitudeNormalizer(INITIAL_MAX_AMPLITUDE));
            lf0Pst = pdf2par.getlf0Pst();
            mcepPst = pdf2par.getMcepPst();
            strPst = pdf2par.getStrPst();
            magPst = pdf2par.getMagPst();
            voiced = pdf2par.getVoicedArray();
            this.htsData = htsData;

        }

        public void run() {
            try {
                htsMLSAVocoder(lf0Pst, mcepPst, strPst, magPst, voiced, htsData, this);
                putEndOfStream();
            } catch (Exception e) {
                System.err.println("Cannot vocode");
                e.printStackTrace();
            }
        }

    }
} 
