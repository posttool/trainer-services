package hmi.synth.voc.test;

import hmi.sig.AudioPlayer;
import hmi.sig.BufferedDoubleDataSource;
import hmi.sig.DDSAudioInputStream;
import hmi.synth.voc.PData;
import hmi.synth.voc.PStream;
import hmi.synth.voc.Vocoder;
import hmi.util.LDataInputStream;
import hmi.util.MathUtils;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

public class VocoderTest extends Vocoder {

    /**
     * Stand alone testing reading parameters from files in SPTK format
     */
    public static void main(String[] args) throws IOException, InterruptedException, Exception {

        PData pdata = new PData();
        PStream lf0Pst, mcepPst, strPst, magPst;
        boolean[] voiced = null;
        LDataInputStream lf0Data, mcepData, strData, magData;

        String lf0File, mcepFile, strFile, outFile, magFile, residualFile;

        outFile = "/Users/posttool/Documents/tmp.wav";

        String hmmTrainDir = "/Users/posttool/Documents/github/hmi-www/app/build/data/test-2/hts/";
        String voiceExample = "sdsg0596"; // sstr0007 sdsg0596 slis0208 snum1142
        // sbas0150

        // Properties p = new Properties();
        // p.setProperty("base",
        // "/Users/posttool/Documents/github/voice-enst-catherine-hsmm/src/main/resources/marytts/voice/EnstCatherineHsmm/");
        // p.setProperty("gender", "female");
        // p.setProperty("rate", "16000");
        // p.setProperty("alpha", "0.42");
        // p.setProperty("beta", "0.3");
        // p.setProperty("logGain", "false");
        // p.setProperty("useGV", "false");
        // p.setProperty("maxMgcGvIter", "200");
        // p.setProperty("maxLf0GvIter", "200");
        // p.setProperty("featuresFile", "utt_2513.pfeats");

//        Properties p = new Properties();
//        p.setProperty("base",
//                "/Users/posttool/Documents/github/marytts/voice-cmu-slt-hsmm/src/main/resources/marytts/voice/CmuSltHsmm/");
//        p.setProperty("gender", "female");
//        p.setProperty("rate", "16000");
//        p.setProperty("alpha", "0.42");
//        p.setProperty("beta", "0.0");
//        p.setProperty("logGain", "true");
//        p.setProperty("useGV", "true");
//        p.setProperty("maxMgcGvIter", "200");
//        p.setProperty("maxLf0GvIter", "200");
//        p.setProperty("featuresFile", "cmu_us_arctic_slt_b0487.pfeats");
        
        
        Properties p = new Properties();
        p.setProperty("base",
                "/Users/posttool/Documents/github/hmi-www/app/build/data/test-2/mary/voice-my_hmmmm_voice-hsmm/src/main/resources/marytts/voice/My_hmmmm_voiceHsmm/");
        p.setProperty("gender", "female");
        p.setProperty("rate", "16000");
        p.setProperty("alpha", "0.42");
        p.setProperty("beta", "0.0");
        p.setProperty("logGain", "true");
        p.setProperty("useGV", "true");
        p.setProperty("maxMgcGvIter", "200");
        p.setProperty("maxLf0GvIter", "200");
        p.setProperty("featuresFile", "features_example.pfeats");
        p.setProperty("excitationFilters", "mix_excitation_5filters_99taps_16Kz.txt");
       
        
        // # acoustic models to use (HMM models or carts from other voices can
        // be specified)
        // acousticModels = duration F0
        // duration.model = hmm
        // duration.attribute = d
        // F0.model = hmm
        // F0.attribute = f0

        pdata.initHMMData(p);
        pdata.setUseMixExc(true);
        /* use Fourier magnitudes for pulse generation */
        pdata.setUseFourierMag(false);

        /* parameters extracted from real data with SPTK and snack */
        lf0File = hmmTrainDir + "data/lf0/" + voiceExample + ".lf0";
        mcepFile = hmmTrainDir + "data/mgc/" + voiceExample + ".mgc";
        strFile = hmmTrainDir + "data/str/" + voiceExample + ".str";
        magFile = hmmTrainDir + "data/mag/" + voiceExample + ".mag";

        int mcepVsize = pdata.getCartTreeSet().getMcepVsize();
        int strVsize = pdata.getCartTreeSet().getStrVsize();
        int lf0Vsize = pdata.getCartTreeSet().getLf0Stream();
        int magVsize = pdata.getCartTreeSet().getMagVsize();

        int totalFrame = 0;
        int lf0VoicedFrame = 0;
        float fval;
        int i, j;
        lf0Data = new LDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));

        /* First i need to know the size of the vectors */
        try {
            while (true) {
                fval = lf0Data.readFloat();
                totalFrame++;
                if (fval > 0)
                    lf0VoicedFrame++;
            }
        } catch (EOFException e) {
        }
        lf0Data.close();

        /*
         * CHECK: I do not know why mcep has totalframe-2 frames less than lf0
         * and str ???
         */
        totalFrame = totalFrame - 2;
        System.out.println("Total number of Frames = " + totalFrame);
        voiced = new boolean[totalFrame];

        /* Initialise HTSPStream-s */
        lf0Pst = new PStream(lf0Vsize, totalFrame, PData.FeatureType.LF0, 0);
        mcepPst = new PStream(mcepVsize, totalFrame, PData.FeatureType.MGC, 0);
        strPst = new PStream(strVsize, totalFrame, PData.FeatureType.STR, 0);
        magPst = new PStream(magVsize, totalFrame, PData.FeatureType.MAG, 0);

        /* load lf0 data */
        /* for lf0 i just need to load the voiced values */
        lf0VoicedFrame = 0;
        lf0Data = new LDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));
        for (i = 0; i < totalFrame; i++) {
            fval = lf0Data.readFloat();

            // lf0Pst.setPar(i, 0, fval);
            if (fval < 0)
                voiced[i] = false;
            else {
                voiced[i] = true;
                lf0Pst.setPar(lf0VoicedFrame, 0, fval);
                lf0VoicedFrame++;
            }
        }
        lf0Data.close();

        /* load mgc data */
        mcepData = new LDataInputStream(new BufferedInputStream(new FileInputStream(mcepFile)));
        for (i = 0; i < totalFrame; i++) {
            for (j = 0; j < mcepPst.getOrder(); j++)
                mcepPst.setPar(i, j, mcepData.readFloat());
        }
        mcepData.close();

        /* load str data */
        strData = new LDataInputStream(new BufferedInputStream(new FileInputStream(strFile)));
        for (i = 0; i < totalFrame; i++) {
            for (j = 0; j < strPst.getOrder(); j++)
                strPst.setPar(i, j, strData.readFloat());
        }
        strData.close();

        /* load mag data */
        // magData = new LEDataInputStream(new BufferedInputStream(new
        // FileInputStream(magFile)));
        // for (i = 0; i < totalFrame; i++) {
        // for (j = 0; j < magPst.getOrder(); j++)
        // magPst.setPar(i, j, magData.readFloat());
        // // System.out.println("i:" + i + "  f0=" + Math.exp(lf0Pst.getPar(i,
        // // 0)) + "  mag(1)=" + magPst.getPar(i, 0) +
        // // "  str(1)=" + strPst.getPar(i, 0) );
        // }
        // magData.close();

        AudioFormat af = getHTSAudioFormat(pdata);
        double[] audio_double = null;

        VocoderTest par2speech = new VocoderTest();

        // par2speech.setUseLpcVocoder(true);

        audio_double = par2speech.htsMLSAVocoder(lf0Pst, mcepPst, strPst, magPst, voiced, pdata, null);
        // audio_double = par2speech.htsMLSAVocoder_residual(htsData, mcepPst,
        // resFile);

        long lengthInSamples = (audio_double.length * 2) / (af.getSampleSizeInBits() / 8);
        System.out.println("length in samples=" + lengthInSamples);

        /*
         * Normalize the signal before return, this will normalize between 1 and
         * -1
         */
        double MaxSample = MathUtils.getAbsMax(audio_double);
        for (i = 0; i < audio_double.length; i++)
            audio_double[i] = 0.3 * (audio_double[i] / MaxSample);

        DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);

        File fileOut = new File(outFile);
        System.out.println("saving to file: " + outFile);

        if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, oais)) {
            AudioSystem.write(oais, AudioFileFormat.Type.WAVE, fileOut);
        }

        System.out.println("Calling audioplayer:");
        AudioPlayer player = new AudioPlayer(fileOut);
        player.start();
        player.join();
        System.out.println("audioplayer finished...");

    }

    /**
     * Stand alone vocoder reading parameters from files in SPTK format,
     * parameters in args[] array in the following order:
     * <p>
     * The type of spectrum parameters is set through the parameters gamma and
     * alpha
     * <p>
     * 
     * @param gamma
     *            : 0
     * @param alpha
     *            : 0.45
     *            <p>
     *            MGC: stage=gamma=0 alpha=0.42 linear gain
     *            <p>
     *            LSP: gamma>0
     *            <p>
     *            LSP : gamma=1 alpha=0.0
     *            <p>
     *            Mel-LSP: gamma=1 alpha=0.42
     *            <p>
     *            MGC-LSP: gamma=3 alpha=0.42
     *            <p>
     * @param useLoggain
     *            : 0 (1:true 0:false)
     * @param beta
     *            : 0.0 0.0 --> 1.0 (postfiltering)
     * @param rate
     *            : 16000
     * @param fperiod
     *            : 80 (5 milisec)
     * @param mcepFile
     *            : filename
     * @param mcepVsize
     *            : vector size (75 if using a file from amain2 hmm voice
     *            training data, otherwise specify)
     * @param lf0File
     *            : filename
     * @param lf0Vsize
     *            : vector size (3 if using a file from a hmm voice training
     *            data, otherwise specify)
     * @param outputfile
     *            : filename
     * 
     *            The following are optional:
     *            <p>
     *            if using mixed excitation:
     *            <p>
     * @param strFile
     *            : filename
     * @param strVsize
     *            : vector size (15 if using a file from a hmm voice training
     *            data, it can be found in
     *            data/filters/mix_excitation_filters.txt, otherwise specify)
     * @param filtersFile
     *            : filename
     * @param numFilters
     *            : 5 (if using the filters file used in the demo, otherwise
     *            specify)
     * @param orderFilters
     *            : 48 (if using the filters file used in the demo, otherwise
     *            specify)
     * 
     *            <p>
     *            if using Fourier magnitudes:
     *            <p>
     * @param magFile
     *            : filename
     * @param magVsize
     *            : vector size (30 if using a file from a hmm voice training
     *            data, otherwise specify)
     * 
     *            <p>
     *            example iput parameters:
     *            <p>
     *            0 0.45 0 0.0 16000 80 cmu_us_arctic_slt_a0001.mgc 75
     *            cmu_us_arctic_slt_a0001.lf0 3 vocoder_out.wav
     *            cmu_us_arctic_slt_a0001.str 15 mix_excitation_filters.txt 5 48
     *            cmu_us_arctic_slt_a0001.mag 30
     *            <p>
     *            example input parameters without mixed excitation:
     *            <p>
     *            0 0.45 0 0.0 16000 80 cmu_us_arctic_slt_a0001.mgc 75
     *            cmu_us_arctic_slt_a0001.lf0 3 vocoder_out.wav
     * */
    public static void htsMLSAVocoderCommand(String[] args) throws IOException, InterruptedException, Exception {

        PData pdata = new PData();
        PStream lf0Pst, mcepPst, strPst = null, magPst = null;
        boolean[] voiced = null;
        LDataInputStream lf0Data, mcepData, strData, magData;

        String lf0File, mcepFile, strFile = "", magFile = "", outDir, outFile;
        int mcepVsize, lf0Vsize, strVsize = 0, magVsize = 0;
        // -----------------------------------
        // Values for FEMALE:
        // LOUD:
        float f0LoudFemale = 0.01313791f;
        float strLoudFemale[] = { -0.002995137f, -0.042511885f, 0.072285673f, 0.127030178f, 0.006603170f };
        float magLoudFemale[] = { 0.0417336550f, 0.0002531457f, -0.0436839922f, -0.0335192265f, -0.0217501786f,
                -0.0166272925f, -0.0424825309f, -0.0460119758f, -0.0307114900f, -0.0327369397f };
        float mcepLoudFemale[] = { -0.245401838f, -0.062825965f, -0.360973095f, 0.117120506f, 0.917223265f,
                0.138920770f, 0.338553265f, -0.004857140f, 0.285192007f, -0.358292740f, -0.062907335f, -0.008040502f,
                0.029470562f, -0.485079992f, -0.006727651f, -1.313869583f, -0.353797651f, 0.797097747f, -0.164614609f,
                -0.311173881f, -0.205134527f, -0.478116992f, -0.311340181f, -1.485855332f, -0.045632626f };
        // SOFT:
        float f0SoftFemale = 0.3107256f;
        float strSoftFemale[] = { 0.22054621f, 0.11091616f, 0.06378487f, 0.02110654f, -0.05118725f };
        float magSoftFemale[] = { 0.5747024f, 0.3248238f, 0.2356782f, 0.2441387f, 0.2702851f, 0.2895966f, 0.2437654f,
                0.2959747f, 0.2910529f, 0.2508167f };
        float mcepSoftFemale[] = { -0.103318169f, 0.315698439f, 0.170000964f, 0.223589719f, 0.262139649f,
                -0.062646758f, -4.998160141f, 0.008026212f, 1.742740835f, 1.990719666f, 0.548177521f, 0.999093856f,
                0.262868363f, 1.755019406f, 0.330058590f, -5.241305159f, -0.021005177f, -5.890942393f, 0.344385084f,
                0.242179454f, 0.200936671f, -1.630683357f, 0.110674201f, -53.525043676f, -0.223682764f };

        // -----------------------------------
        // Values for MALE:
        // LOUD:
        float f0LoudMale = -0.08453168f;
        float strLoudMale[] = { 0.07092900f, 0.41149292f, 0.24479925f, 0.01326785f, -0.01517731f };
        float magLoudMale[] = { -0.21923620f, -0.11031120f, -0.02786084f, -0.10640244f, -0.12020442f, -0.08508762f,
                -0.08171423f, -0.08000552f, -0.07291968f, -0.09478534f };
        float mcepLoudMale[] = { 0.15335238f, 0.30880292f, -0.22922052f, -0.01116095f, 1.04088351f, -0.31693632f,
                -19.36510752f, -0.12210441f, 0.81743415f, -0.19799409f, 0.44572112f, -0.24845725f, -1.39545409f,
                -0.88788491f, 8.83006358f, -1.26623882f, 0.52428102f, -1.02615700f, -0.28092043f, -0.82543015f,
                0.33081815f, 0.39498874f, 0.20100945f, 0.60890790f, -0.37892217f };
        // SOFT:
        float f0SoftMale = 0.05088677f;
        float strSoftMale[] = { 0.07595702f, 0.02348965f, -0.02038628f, -0.08572970f, -0.06090386f };
        float magSoftMale[] = { 0.08869109f, 0.05517088f, 0.08902098f, 0.09263865f, 0.04866824f, 0.04554406f,
                0.04937004f, 0.05082076f, 0.04988959f, 0.03459440f };
        float mcepSoftMale[] = { 0.098129393f, 0.124686819f, 0.195709008f, -0.007066379f, -1.795620578f, 0.089982916f,
                15.371711686f, -0.051023831f, -0.213521945f, 0.009725292f, 0.361488718f, 0.118609995f, 1.794143134f,
                0.100130942f, 0.005999542f, -0.593128934f, -0.165385304f, 0.101705681f, 0.175534153f, 0.049246302f,
                0.009530379f, -0.272557042f, -0.043030771f, 0.158694874f, 0.099107970f };

        float f0Trans = 0f;
        float strTrans[] = null;
        float magTrans[] = null;
        float mcepTrans[] = null;

        // set values that the vocoder needs
        // Type of features:
        int ind = 0;
        pdata.setStage(Integer.parseInt(args[ind++])); // sets gamma
        pdata.setAlpha(Float.parseFloat(args[ind++])); // set alpha
        if (args[ind++].contentEquals("1"))
            pdata.setUseLogGain(true); // use log gain
        else
            pdata.setUseLogGain(false);
        pdata.setBeta(Float.parseFloat(args[ind++])); // set beta: for
                                                        // postfiltering
        pdata.setRate(Integer.parseInt(args[ind++])); // rate
        pdata.setFperiod(Integer.parseInt(args[ind++])); // period

        /* parameters extracted from real data with SPTK and snack */
        mcepFile = args[ind++];
        mcepVsize = Integer.parseInt(args[ind++]);

        lf0File = args[ind++];
        lf0Vsize = Integer.parseInt(args[ind++]);

        // output wav file
        outFile = args[ind++];

        // Optional:
        // if using mixed excitation
        if (args.length > (ind + 1)) {
            pdata.setUseMixExc(true);
            strFile = args[ind++];
            strVsize = Integer.parseInt(args[ind++]);
            FileInputStream mixedFiltersStream = new FileInputStream(args[ind++]);
            pdata.setNumFilters(Integer.parseInt(args[ind++]));
            pdata.readMixedExcitationFilters(mixedFiltersStream);
            pdata.setPdfStrStream(null);
        } else {
            pdata.setUseMixExc(false);
        }

        // Optional:
        // if using Fourier magnitudes in mixed excitation
        if (args.length > (ind + 1)) {
            pdata.setUseFourierMag(true);
            magFile = args[ind++];
            magVsize = Integer.parseInt(args[ind++]);
            pdata.setPdfMagStream(null);
        } else {
            pdata.setUseFourierMag(false);
        }

        // last argument true or false to play the file
        boolean play = Boolean.parseBoolean(args[ind++]);

        boolean trans = true;
        // if (args[ind].contentEquals("loud")) {
        f0Trans = f0LoudFemale;
        strTrans = strLoudFemale;
        magTrans = magLoudFemale;
        mcepTrans = mcepLoudFemale;
        System.out.println("Generating loud voice");
        // } else if (args[ind].contentEquals("soft")) {
        // f0Trans = f0SoftFemale;
        // strTrans = strSoftFemale;
        // magTrans = magSoftFemale;
        // mcepTrans = mcepSoftFemale;
        // System.out.println("Generating soft voice");
        // } else {
        // trans = false;
        // System.out.println("Generating modal voice");
        // }

        // Change these for voice effects:
        // [min][max]
        pdata.setF0Std(1.0); // variable for f0 control, multiply f0
                               // [1.0][0.0--5.0]
        pdata.setF0Mean(0.0); // variable for f0 control, add f0
                                // [0.0][0.0--100.0]

        int totalFrame = 0;
        int lf0VoicedFrame = 0;
        float fval;
        int i, j;
        lf0Data = new LDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));

        /* First i need to know the size of the vectors */
        File lf0 = new File(lf0File);
        long lengthLf0 = lf0.length(); // Get the number of bytes in the file
        lengthLf0 = lengthLf0 / ((lf0Vsize / 3) * 4); // 4 bytes per float

        File mcep = new File(mcepFile);
        long lengthMcep = mcep.length();
        lengthMcep = lengthMcep / ((mcepVsize / 3) * 4);
        int numSize = 2;
        long lengthStr;
        if (pdata.getUseMixExc()) {
            File str = new File(strFile);
            lengthStr = str.length();
            lengthStr = lengthStr / ((strVsize / 3) * 4);
            numSize++;
        } else
            lengthStr = 0;

        long lengthMag;
        if (pdata.getUseFourierMag()) {
            File mag = new File(magFile);
            lengthMag = mag.length();
            lengthMag = lengthMag / ((magVsize / 3) * 4);
            numSize++;
        } else
            lengthMag = 0;

        float sizes[] = new float[numSize];
        int n = 0;
        sizes[n++] = lengthMcep;
        sizes[n++] = lengthLf0;
        if (lengthStr > 0)
            sizes[n++] = lengthStr;
        if (lengthMag > 0)
            sizes[n++] = lengthMag;

        // choose the lowest
        // float sizes[] = {lengthLf0, lengthMcep, lengthStr, lengthMag};

        totalFrame = (int) MathUtils.getMin(sizes);
        System.out.println("Total number of Frames = " + totalFrame);
        voiced = new boolean[totalFrame];

        /* Initialise HTSPStream-s */
        lf0Pst = new PStream(lf0Vsize, totalFrame, PData.FeatureType.LF0, 0);
        mcepPst = new PStream(mcepVsize, totalFrame, PData.FeatureType.MGC, 0);

        /* load lf0 data */
        /* for lf0 i just need to load the voiced values */
        lf0VoicedFrame = 0;
        lf0Data = new LDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));
        for (i = 0; i < totalFrame; i++) {
            fval = lf0Data.readFloat();
            // lf0Pst.setPar(i, 0, fval);
            if (fval < 0)
                voiced[i] = false;
            else {
                voiced[i] = true;

                // apply here the change to loud
                if (trans) {
                    fval = (float) Math.exp(fval);
                    fval = fval + (fval * f0Trans);
                    fval = (float) Math.log(fval);
                }
                lf0Pst.setPar(lf0VoicedFrame, 0, fval);
                lf0VoicedFrame++;
            }
        }
        lf0Data.close();

        /* load mgc data */
        mcepData = new LDataInputStream(new BufferedInputStream(new FileInputStream(mcepFile)));
        for (i = 0; i < totalFrame; i++) {
            for (j = 0; j < mcepPst.getOrder(); j++) {
                // apply here the change to loud
                fval = mcepData.readFloat();
                if (trans & j < 4)
                    fval = fval + (fval * mcepTrans[j]);
                mcepPst.setPar(i, j, fval);
            }
        }
        mcepData.close();

        /* load str data */
        if (pdata.getUseMixExc()) {
            strPst = new PStream(strVsize, totalFrame, PData.FeatureType.STR, 0);
            strData = new LDataInputStream(new BufferedInputStream(new FileInputStream(strFile)));
            for (i = 0; i < totalFrame; i++) {
                for (j = 0; j < strPst.getOrder(); j++) {
                    // apply here the change to loud/soft
                    fval = strData.readFloat();
                    if (trans)
                        fval = fval + (fval * strTrans[j]);
                    strPst.setPar(i, j, fval);
                }
            }
            strData.close();
        }

        /* load mag data */
        n = 0;
        if (pdata.getUseFourierMag()) {
            magPst = new PStream(magVsize, totalFrame, PData.FeatureType.MAG, 0);
            magData = new LDataInputStream(new BufferedInputStream(new FileInputStream(magFile)));
            for (i = 0; i < totalFrame; i++) {
                // System.out.print(n + " : ");
                for (j = 0; j < magPst.getOrder(); j++) {
                    n++;
                    fval = magData.readFloat();
                    if (trans)
                        fval = fval + (fval * magTrans[j]);
                    magPst.setPar(i, j, fval);
                    // System.out.format("mag(%d,%d)=%.2f ",i, j,
                    // magPst.getPar(i, j) );
                }
                // System.out.println();
            }
            magData.close();
        }

        AudioFormat af = getHTSAudioFormat(pdata);
        double[] audio_double = null;

        VocoderTest par2speech = new VocoderTest();

        // par2speech.setUseLpcVocoder(true);
        // audio_double = par2speech.htsMLSAVocoder_residual(htsData, mcepPst,
        // resFile);

        audio_double = par2speech.htsMLSAVocoder(lf0Pst, mcepPst, strPst, magPst, voiced, pdata, null);

        long lengthInSamples = (audio_double.length * 2) / (af.getSampleSizeInBits() / 8);
        System.out.println("length in samples=" + lengthInSamples);

        /*
         * Normalise the signal before return, this will normalise between 1 and
         * -1
         */
        double MaxSample = MathUtils.getAbsMax(audio_double);
        for (i = 0; i < audio_double.length; i++)
            audio_double[i] = (audio_double[i] / MaxSample);

        DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);

        File fileOut = new File(outFile);
        System.out.println("saving to file: " + outFile);

        if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, oais)) {
            AudioSystem.write(oais, AudioFileFormat.Type.WAVE, fileOut);
        }

        if (play) {
            System.out.println("Calling audioplayer:");
            AudioPlayer player = new AudioPlayer(fileOut);
            player.start();
            player.join();
            System.out.println("audioplayer finished...");
        }

    }

    public static void main0(String[] args) throws IOException, InterruptedException, Exception {

        // sequ0202 sstr0007 sdsg0596 slis0208 snum1142 sbas0150

        String path = "/Users/posttool/Documents/github/hmi-www/app/build/data/test-2/hts/data/";
        String topic = "sbas0150";
        String args1[] = { "0", "0.42", "0.02", "0.15", "16000", "80", path + "mgc/" + topic + ".mgc", "75",
                path + "lf0/" + topic + ".lf0", "3", path + "vocoder/" + topic + ".wav",
                path + "str/" + topic + ".str", "15", path + "filters/mix_excitation_5filters_99taps_16Kz.txt", "5",
                "true" };
        htsMLSAVocoderCommand(args1);

        /*
         * String path = "/HMM-voices/BITS/bits1/hts/data/"; String args3[] =
         * {"0", "0.42", "0.05", "0.3", "16000", "80", path +
         * "mgc/US10010046_0.mgc", "75", path + "lf0-100-270/US10010046_0.lf0",
         * "3", path + "vocoder_out-100-270.wav", path +
         * "str-100-270/US10010046_0.str", "15", path +
         * "filters/mix_excitation_filters.txt", "5", "true"}; HTSVocoder
         * vocoder = new HTSVocoder(); vocoder.htsMLSAVocoderCommand(args3);
         */
        /*
         * String path = "/quality_parameters/necadbs/hts/data/"; String args3[]
         * = {"0", "0.42", "0.05", "0.15", "16000", "80", path +
         * "mgc/modal0001.mgc", "75", path + "lf0/modal0001.lf0", "3", path +
         * "vocoder_out-modal-soft.wav", path + "str/soft0001.str", "15", path +
         * "filters/mix_excitation_filters.txt", "5", "true"}; HTSVocoder
         * vocoder = new HTSVocoder(); vocoder.htsMLSAVocoderCommand(args3);
         */

        /*
         * String path = "/HMM-voices/arctic_slt/hts/data/"; String fileName =
         * "modal0002"; //String fileName = "de_0001"; String args4[] = {"0",
         * "0.42", "0.05", "0.25", "16000", "80", path + "mgc/" + fileName +
         * ".mgc", "75", path + "lf0/" + fileName + ".lf0", "3", path +
         * "vocoder/" + fileName + "_vocoder_soft.wav", path + "str/" + fileName
         * + ".str", "15", path + "filters/mix_excitation_filters.txt", "5",
         * path + "mag/" + fileName + ".mag", "30", "true", "soft"}; HTSVocoder
         * vocoder = new HTSVocoder(); vocoder.htsMLSAVocoderCommand(args4);
         */

        /*
         * Use this for running HTSVocoder for a list, see vocoderList for the
         * parameters
         */

        /*
         * HTSVocoder vocoder = new HTSVocoder(); vocoder.vocoderList(args);
         */

    }

    public static void vocoderList(String[] args) throws IOException, InterruptedException, Exception {

        String path = "/ccc/hts/data/";

        File outDir = new File(path + "vocoder");
        if (!outDir.exists())
            outDir.mkdir();
        File directory = new File(path + "raw");
        String files[] = listBasenames(directory, ".raw");

        // the output will be in path/vocoder directory, it has to be created
        // beforehand

        for (int i = 0; i < files.length; i++) {

            System.out.println("file: " + files[i]);

            // MGC stage=0.0 alpha=0.42 logGain=0 (false)
            // MGC-LSP stage=3.0 alpha=0.42 loggain=1 (true)

            /*
             * String args1[] = {"0", "0.42", "0", "0.15", "16000", "80", path +
             * "mgc/" + files[i] + ".mgc", "75", path + "lf0/" + files[i] +
             * ".lf0", "3", path + "vocoder/" + files[i] + ".wav", path + "str/"
             * + files[i] + ".str", "15", path +
             * "filters/mix_excitation_filters.txt", "5", path + "mag/" +
             * files[i] + ".mag", "30", "true"}; // the last true/false is for
             * playing or not the generated file
             */

            // without Fourier magnitudes
            // the last true/false is for playing or not the generated file
            String args1[] = { "0", "0.42", "0.05", "0.15", "16000", "80", path + "mgc/" + files[i] + ".mgc", "75",
                    path + "lf0/" + files[i] + ".lf0", "3", path + "vocoder/" + files[i] + ".wav",
                    path + "str/" + files[i] + ".str", "15", path + "filters/mix_excitation_filters.txt", "5", "true" };

            // without Mixed excitation and Fourier magnitudes
            /*
             * String args1[] = {"0", "0.42", "0", "0.0", "16000", "80", path +
             * "mgc/" + files[i] + ".mgc", "75", path + "lf0/" + files[i] +
             * ".lf0", "3", path + "vocoder/" + files[i] + ".wav", "true"}; //
             * the last true/false is for playing or not the generated file
             */
            htsMLSAVocoderCommand(args1);

        }

    }

    public static String[] listBasenames(File directory, String suffix) {
        final String theSuffix = suffix;
        String[] filenames = directory.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(theSuffix);
            }
        });

        /* Sort the file names alphabetically */
        Arrays.sort(filenames);

        for (int i = 0; i < filenames.length; i++) {
            filenames[i] = filenames[i].substring(0, filenames[i].length() - suffix.length());
        }
        return filenames;
    }

}
