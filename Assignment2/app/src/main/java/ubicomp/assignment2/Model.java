package ubicomp.assignment2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.transform.FastFourierTransformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Instances;

import static java.lang.Math.ceil;
import static java.lang.Math.log;
import static java.lang.Math.pow;

public class Model {
    // Examples of how the .arff format:
    // https://www.programcreek.com/2013/01/a-simple-machine-learning-example-in-java/
    // https://www.cs.waikato.ac.nz/~ml/weka/arff.html
    private Map<String, ArrayList<String[]>> trainingData;
    private String[] testData;
    public Map<String, String> featureNames;
    private String trainDataFilepath = "trainData.arff";
    private String testDataFilepath = "testData.arff";
    private Classifier model;
    private Context context;

    public String[] outputClasses = {"Raise", "Figure8", "Circle"};


    public Model(Context context) {
        this.context = context;
        resetTrainingData();

        // Specify the features
        featureNames = new TreeMap<>();
        featureNames.put("ax_mean", "numeric");
        featureNames.put("ay_mean", "numeric");
        featureNames.put("az_mean", "numeric");
        featureNames.put("gx_mean", "numeric");
        featureNames.put("gy_mean", "numeric");
        featureNames.put("gz_mean", "numeric");
        featureNames.put("ax_variance", "numeric");
        featureNames.put("ay_variance", "numeric");
        featureNames.put("az_variance", "numeric");
        featureNames.put("gx_variance", "numeric");
        featureNames.put("gy_variance", "numeric");
        featureNames.put("gz_variance", "numeric");
        featureNames.put("ax_max_freq", "numeric");
        featureNames.put("ay_max_freq", "numeric");
        featureNames.put("az_max_freq", "numeric");
        featureNames.put("gx_max_freq", "numeric");
        featureNames.put("gy_max_freq", "numeric");
        featureNames.put("gz_max_freq", "numeric");
        featureNames.put("ax_kurtosis", "numeric");
        featureNames.put("ay_kurtosis", "numeric");
        featureNames.put("az_kurtosis", "numeric");
        featureNames.put("gx_kurtosis", "numeric");
        featureNames.put("gy_kurtosis", "numeric");
        featureNames.put("gz_kurtosis", "numeric");
    }

    /**
     * Add a sample to the training or testing set with the corresponding label
     * @param atime: the time for the accelerometer data
     * @param ax: the x-acceleration data
     * @param ay: the y-acceleration data
     * @param az: the z-acceleration data
     * @param gtime: the time for the accelerometer data
     * @param gx: the x-gyroscope data
     * @param gy: the y-gyroscope data
     * @param gz: the z-gyroscope data
     * @param outputLabel: the label for the data
     * @param isTraining: whether the sample should go into the train or test set
     */
    public void addSample(DescriptiveStatistics atime, DescriptiveStatistics ax, DescriptiveStatistics ay, DescriptiveStatistics az,
                             DescriptiveStatistics gtime, DescriptiveStatistics gx, DescriptiveStatistics gy, DescriptiveStatistics gz,
                             String outputLabel, boolean isTraining) {
        Double[] data = new Double[featureNames.keySet().size()];

        int next_aN = (int)pow(2, ceil(log(ax.getN())/log(2)));
        int next_gN = (int)pow(2, ceil(log(gx.getN())/log(2)));
        int maxAx = 0;
        int maxAy = 0;
        int maxAz = 0;
        int maxGx = 0;
        int maxGy = 0;
        int maxGz = 0;

        double[] ax_fft_in = new double[next_aN];
        double[] ay_fft_in = new double[next_aN];
        double[] az_fft_in = new double[next_aN];
        double[] gx_fft_in = new double[next_gN];
        double[] gy_fft_in = new double[next_gN];
        double[] gz_fft_in = new double[next_gN];
        double[] ax_fft_out = new double[next_aN];
        double[] ay_fft_out = new double[next_aN];
        double[] az_fft_out = new double[next_aN];
        double[] gx_fft_out = new double[next_gN];
        double[] gy_fft_out = new double[next_gN];
        double[] gz_fft_out = new double[next_gN];

        FFT a_fft = new FFT(next_aN);
        FFT g_fft = new FFT(next_gN);

        for (int i = 0; i < next_aN; ++i) {
            if (i < ax.getN()) {
                ax_fft_in[i] = ax.getElement(i);
                ay_fft_in[i] = ay.getElement(i);
                az_fft_in[i] = az.getElement(i);
            }
            else {
                ax_fft_in[i] = 0;
                ay_fft_in[i] = 0;
                az_fft_in[i] = 0;
            }
        }

        for (int i = 0; i < next_gN; ++i) {
            if (i < gx.getN()) {
                gx_fft_in[i] = gx.getElement(i);
                gy_fft_in[i] = gy.getElement(i);
                gz_fft_in[i] = gz.getElement(i);
            }
            else {
                gx_fft_in[i] = 0;
                gy_fft_in[i] = 0;
                gz_fft_in[i] = 0;
            }
        }

        a_fft.fft(ax_fft_in, ax_fft_out);
        a_fft.fft(ay_fft_in, ay_fft_out);
        a_fft.fft(az_fft_in, az_fft_out);
        g_fft.fft(gy_fft_in, gx_fft_out);
        g_fft.fft(gy_fft_in, gy_fft_out);
        g_fft.fft(gz_fft_in, gz_fft_out);

        for (int i = 0; i < ax_fft_out.length; ++i) {
            maxAx = ax_fft_out[i] > ax_fft_out[maxAx] ? i : maxAx;
            maxAy = ay_fft_out[i] > ay_fft_out[maxAy] ? i : maxAy;
            maxAz = az_fft_out[i] > az_fft_out[maxAz] ? i : maxAz;
        }

        for (int i = 0; i < gx_fft_out.length; ++i) {
            maxGx = gx_fft_out[i] > gx_fft_out[maxGx] ? i : maxGx;
            maxGy = gy_fft_out[i] > gy_fft_out[maxGy] ? i : maxGy;
            maxGz = gz_fft_out[i] > gz_fft_out[maxGz] ? i : maxGz;
        }

        // Compute features
        data[0] = ax.getMean();
        data[1] = ay.getMean();
        data[2] = az.getMean();
        data[3] = gx.getMean();
        data[4] = gy.getMean();
        data[5] = gz.getMean();
        data[6] = ax.getVariance();
        data[7] = ay.getVariance();
        data[8] = az.getVariance();
        data[9] = gx.getVariance();
        data[10] = gy.getVariance();
        data[11] = gz.getVariance();
        data[12] = new Double(maxAx);
        data[13] = new Double(maxAy);
        data[14] = new Double(maxAz);
        data[15] = new Double(maxGx);
        data[16] = new Double(maxGy);
        data[17] = new Double(maxGz);
        data[18] = ax.getKurtosis();
        data[19] = ay.getKurtosis();
        data[20] = az.getKurtosis();
        data[21] = gx.getKurtosis();
        data[22] = gy.getKurtosis();
        data[23] = gz.getKurtosis();

        // Convert the feature vector to Strings
        String[] stringData = new String[featureNames.keySet().size()];
        for (int i=0; i<featureNames.keySet().size(); i++) {
            stringData[i] = Double.toString(data[i]);
        }

        // Add to the list of feature samples as strings
        if (isTraining) {
            ArrayList<String[]> currentSamples = trainingData.get(outputLabel);
            currentSamples.add(stringData);
            trainingData.put(outputLabel, currentSamples);
        }
        else {
            testData = stringData;
        }
    }

    /**
     * Clears all of the data for the model
     */
    public void resetTrainingData() {
        // Create a blank list for each gesture
        trainingData = new LinkedHashMap<>();
        for (String s: outputClasses) {
            trainingData.put(s, new ArrayList<String[]>());
        }
    }

    /**
     * Returns the number of training samples for the given class index
     * @param index: the class index
     * @return the number of samples for the given class index
     */
    public int getNumTrainSamples(int index) {
        String className = outputClasses[index];
        return trainingData.get(className).size();
    }

    /**
     * Create an .arff file for the dataset
     * @param isTraining: whether the data is training or testing data
     */
    private void createDataFile(boolean isTraining) {
        PrintWriter writer;
        // Setup the file writer depending on whether it is training or testing data
        if (isTraining)
            writer = createPrintWriter(trainDataFilepath);
        else
            writer = createPrintWriter(testDataFilepath);

        // Name the dataset
        writer.println("@relation gestures");
        writer.println("");

        // Define the features
        for (String s: featureNames.keySet()) {
            writer.println("@attribute "+s+" "+featureNames.get(s));
        }

        // Define the possible output classes
        String outputOptions = "@attribute gestureName {";
        for (String s: outputClasses) {
            outputOptions += s+", ";
        }
        outputOptions = outputOptions.substring(0, outputOptions.length()-2);
        outputOptions += "}";
        writer.println(outputOptions);
        writer.println("");

        // Write the data
        writer.println("@data");
        if (isTraining) {
            // Go through each category of possible outputs and save their samples
            for (String s: outputClasses) {
                ArrayList<String[]> gestureSamples = trainingData.get(s);
                for (String[] sampleData: gestureSamples) {
                    String sample = "";
                    for (String x: sampleData) {
                        sample += x+",";
                    }
                    sample += s;
                    writer.println(sample);
                }
            }
        }
        else {
            // Write the new sample with a blank label
            String sample = "";
            for (String x: testData) {
                sample += x+",";
            }
            sample += "?";
            writer.println(sample);
        }
        writer.close();
    }

    /**
     * Trains a model for the training data
     */
    public void train() {
        // Create the file for training
        createDataFile(true);

        // Read the file and specify the last index as the class
        Instances trainInstances = createInstances(trainDataFilepath);
        if (trainInstances == null) {
            return;
        }
        trainInstances.setClassIndex(trainInstances.numAttributes()-1);

        // Define the classifier
        // TODO optional: try out different classifiers provided by Weka
        model = new J48();
        try {
            model.buildClassifier(trainInstances);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Returns the string label for the recently tested gesture
     * @return the string label
     */
    public String test() {
        // Create the file for testing
        createDataFile(false);

        // Read the file and specify the last index as the class
        Instances testInstances = createInstances(testDataFilepath);
        testInstances.setClassIndex(testInstances.numAttributes()-1);

        // Predict
        String classLabel = null;
        try {
            double classIndex = model.classifyInstance(testInstances.instance(0));
            classLabel = testInstances.classAttribute().value((int) classIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classLabel;
    }

    /**
     * Reads the .arff file and converts it into an Instances object
     * @param filename the filepath for the .arff file
     * @return a newly created Instances object
     */
    private Instances createInstances(String filename) {
        // Read the file
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + filename;
        BufferedReader dataReader;
        try {
            FileReader fileReader = new FileReader(fullFileName);
            dataReader = new BufferedReader(fileReader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        // Create the training instance
        Instances instances;
        try {
            instances = new Instances(dataReader);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context,
                    "Something is wrong with your .arff file!",
                    Toast.LENGTH_SHORT).show();
            return null;
        }
        return instances;
    }

    /**
     * Creates the file at the location
     * @param filename: the filename that appears at the root of external storage
     * @return writer: the PrintWriter object to be used
     */
    public PrintWriter createPrintWriter(String filename) {
        // Create the file
        File SDFile = android.os.Environment.getExternalStorageDirectory();
        String fullFileName = SDFile.getAbsolutePath() + File.separator + filename;
        PrintWriter writer;
        try {
            writer = new PrintWriter(fullFileName);
        } catch(FileNotFoundException e) {
            return null;
        }
        return writer;
    }
}
