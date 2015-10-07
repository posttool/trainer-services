package hmi.annotate;

import hmi.train.VoiceRepo;
import org.apache.commons.io.FileUtils;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class ProsodyTrainer {
    public static void main(String[] args) throws Exception {
        VoiceRepo repo = new VoiceRepo("gri-vocc");
        ProsodyTrainer g = new ProsodyTrainer();
        g.train(repo);
        //g.loadTrainedModel();
    }

    //Number of units in each GravesLSTM layer
    int lstmLayerSize = 200;
    //Size of mini batch to use when  training
    int miniBatchSize = 32;
    //Total number of training + sample generation epochs
    int numEpochs = 30;
    //i.e., how many examples to learn on between generating samples
    int examplesPerEpoch = 100 * miniBatchSize;
    //Length of each training example
    int maxLength = 140;
    //Number of samples to generate after each training epoch
    int nSamplesToGenerate = 4;
    //Length of each sample to generate
    int nCharactersToSample = 140;
    //Optional character initialization; a random character is used if null
    String generationInitialization = null;

    String BP = "/Users/david/la/src/main/java/hmi/annotate";

    public ProsodyTrainer() {
    }

    public void train(VoiceRepo repo) throws Exception {
        Random rng = new Random(12345);
        ProsodyIterator iter = new ProsodyIterator(repo, miniBatchSize, examplesPerEpoch, 60, 200);
        int nOut = iter.totalOutcomes();

        //Set up network configuration:
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).iterations(1)
                .learningRate(2e-3)
                .rmsDecay(0.95)
                .seed(12345)
                .regularization(true)
                .l2(0.001)
                .list(3)
                .layer(0, new GravesLSTM.Builder().nIn(iter.inputColumns()).nOut(lstmLayerSize)
                        .updater(Updater.RMSPROP)
                        .activation("tanh").weightInit(WeightInit.DISTRIBUTION)
                        .dist(new UniformDistribution(-0.08, 0.08)).build())
                .layer(1, new GravesLSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
                        .updater(Updater.RMSPROP)
                        .activation("tanh").weightInit(WeightInit.DISTRIBUTION)
                        .dist(new UniformDistribution(-0.08, 0.08)).build())
                .layer(2, new RnnOutputLayer.Builder(LossFunction.MCXENT).activation("softmax")        //MCXENT + softmax for classification
                        .updater(Updater.RMSPROP)
                        .nIn(lstmLayerSize).nOut(nOut).weightInit(WeightInit.DISTRIBUTION)
                        .dist(new UniformDistribution(-0.08, 0.08)).build())
                .pretrain(false).backprop(true)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(1));

        Layer[] layers = net.getLayers();
        int totalNumParams = 0;
        for (int i = 0; i < layers.length; i++) {
            int nParams = layers[i].numParams();
            System.out.println("Number of parameters in layer " + i + ": " + nParams);
            totalNumParams += nParams;
        }
        System.out.println("Total number of network parameters: " + totalNumParams);

        for (int i = 0; i < numEpochs; i++) {
            net.fit(iter);
            System.out.println("--------------------");
            System.out.println("Completed epoch " + i);
            System.out.println("Sampling characters from network given initialization \"" + (generationInitialization == null ? "" : generationInitialization) + "\"");
            double[][] samples = getSamples(new double[]{0}, net, iter, rng, nCharactersToSample, nSamplesToGenerate);
            for (int j = 0; j < samples.length; j++) {
                System.out.println("----- Sample " + j + " -----");
                for (int k = 0; k < samples[j].length; k++) {
                    System.out.print(samples[j][k] + " ");
                }
                System.out.println();
            }
            iter.reset();
        }

        System.out.println("complete");

        OutputStream fos = Files.newOutputStream(Paths.get(BP + "/coefficient-prosody.bin"));
        DataOutputStream dos = new DataOutputStream(fos);
        Nd4j.write(net.params(), dos);
        dos.flush();
        dos.close();
        FileUtils.writeStringToFile(new File(BP + "/conf-prosody.json"), net.getLayerWiseConfigurations().toJson());

        System.out.println("model saved");
    }

//    public void loadTrainedModel() throws Exception {
//        MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(FileUtils.readFileToString(new File(BP+"/conf.json")));
//        DataInputStream dis = new DataInputStream(new FileInputStream(BP+"/coefficients.bin"));
//        INDArray newParams = Nd4j.read(dis);
//        dis.close();
//        MultiLayerNetwork net = new MultiLayerNetwork(confFromJson);
//        net.init();
//        net.setParameters(newParams);
//        System.out.println(net.params());
//        Random rng = new Random(12345);
//        ProsodyIterator iter = new ProsodyIterator( 20, 200);
//        double[][] samples = getSamples(null, net, iter, rng, nCharactersToSample, nSamplesToGenerate);
//        for (int j = 0; j < samples.length; j++) {
//            System.out.println("----- Sample " + j + " -----");
//            System.out.println(samples[j]);
//            System.out.println();
//        }
//    }
//


    /**
     * Generate a sample from the network, given an initialization. Initialization
     * is used to 'prime' the RNN with a sequence you want to extend/continue.<br>
     *
     * @param initialIntervals   initial intervals :-)
     * @param charactersToSample Number of characters to sample from network (excluding initialization)
     * @param net                MultiLayerNetwork with one or more GravesLSTM/RNN layers and a softmax output layer
     * @param iter               CharacterIterator. Used for going from indexes back to characters
     */
    private double[][] getSamples(double[] initialIntervals, MultiLayerNetwork net,
                                  ProsodyIterator iter, Random rng, int charactersToSample, int numSamples) {

        //Create input for initialization
        INDArray initializationInput = Nd4j.zeros(numSamples, iter.inputColumns(), initialIntervals.length);
        for (int i = 0; i < initialIntervals.length; i++) {
            int idx = iter.intervalToGrid(initialIntervals[i]);
            for (int j = 0; j < numSamples; j++) {
                initializationInput.putScalar(new int[]{j, idx, i}, 1.0f);
            }
        }

        double[][] intervalResults = new double[numSamples][];
        for (int i = 0; i < numSamples; i++) {
            intervalResults[i] = new double[maxLength];
            for (int j = 0; j < initialIntervals.length; j++)
                intervalResults[i][j] = initialIntervals[j];
        }

        //Sample from network (and feed samples back into input) one character at a time (for all samples)
        //Sampling is done in parallel here
        net.rnnClearPreviousState();
        INDArray output = net.rnnTimeStep(initializationInput);
        output = output.tensorAlongDimension(output.size(2) - 1, 1, 0);    //Gets the last time step output

        for (int i = 0; i < charactersToSample; i++) {
            //Set up next input (single time step) by sampling from previous output
            INDArray nextInput = Nd4j.zeros(numSamples, iter.inputColumns());
            //Output is a probability distribution. Sample from this for each example we want to generate, and add it to the new input
            for (int s = 0; s < numSamples; s++) {
                double[] outputProbDistribution = new double[iter.totalOutcomes()];
                for (int j = 0; j < outputProbDistribution.length; j++)
                    outputProbDistribution[j] = output.getDouble(s, j);
                int sampledCharacterIdx = sampleFromDistribution(outputProbDistribution, rng);
                nextInput.putScalar(new int[]{s, sampledCharacterIdx}, 1.0f);        //Prepare next time step input
                intervalResults[s][i] = iter.gridToInterval(sampledCharacterIdx);
            }
            output = net.rnnTimeStep(nextInput);    //Do one time step of forward pass
        }

        return intervalResults;
    }

    /**
     * Given a probability distribution over discrete classes, sample from the distribution
     * and return the generated class index.
     *
     * @param distribution Probability distribution over classes. Must sum to 1.0
     */
    private int sampleFromDistribution(double[] distribution, Random rng) {
        double d = rng.nextDouble();
        double sum = 0.0;
        for (int i = 0; i < distribution.length; i++) {
            sum += distribution[i];
            if (d <= sum) return i;
        }
        //Should never happen if distribution is a valid probability distribution
        throw new IllegalArgumentException("Distribution is invalid? d=" + d + ", sum=" + sum);
    }
}