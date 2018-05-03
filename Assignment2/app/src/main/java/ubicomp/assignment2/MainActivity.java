package ubicomp.assignment2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import ubicomp.a2kram.assignment2.R;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private GraphView mGraphAccel;
    private GraphView mGraphGyro;
    private LineGraphSeries<DataPoint> mT_ax = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> mT_ay = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> mT_az = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> mT_gx = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> mT_gy = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> mT_gz = new LineGraphSeries<>();
    private int mT_AccelGraph = 0;
    private int mT_GyroGraph = 0;
    private static final int GRAPH_X_BOUNDS = 50;
    private static final int GRAPH_Y_BOUNDS = 50;
    private int mGraphColor[] = {Color.argb(255,244,170,50),
            Color.argb(255, 60, 175, 240),
            Color.argb(225, 50, 220, 100)};

    private TextView mDisplayText;

    private Vector mV_ax = new Vector<Double>();
    private Vector mV_ay = new Vector<Double>();
    private Vector mV_az = new Vector<Double>();

    private Vector mV_gx = new Vector<Double>();
    private Vector mV_gy = new Vector<Double>();
    private Vector mV_gz = new Vector<Double>();

    private Model mModel;

    private DescriptiveStatistics accelTime, accelX, accelY, accelZ;
    private DescriptiveStatistics gyroTime, gyroX, gyroY, gyroZ;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;

    private boolean isRecording;

    private static final int GESTURE_DURATION_SECS = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI initializations
        mDisplayText = findViewById(R.id.text_view);
        initializeGraphs();

        // Sensor initialization
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Request external storage permission if we dont have it
        if (!checkPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 0, this);
        }

        // Create Model object
        mModel = new Model(this);

        // Initialize data structures for gesture recording
        accelTime = new DescriptiveStatistics();
        accelX = new DescriptiveStatistics();
        accelY = new DescriptiveStatistics();
        accelZ = new DescriptiveStatistics();
        gyroTime = new DescriptiveStatistics();
        gyroX = new DescriptiveStatistics();
        gyroY = new DescriptiveStatistics();
        gyroZ = new DescriptiveStatistics();


        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        DataPoint dataPointAx;
        DataPoint dataPointAy;
        DataPoint dataPointAz;
        DataPoint dataPointGx;
        DataPoint dataPointGy;
        DataPoint dataPointGz;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mT_AccelGraph += 1;

            mV_ax.add(event.values[0]);
            mV_ay.add(event.values[1]);
            mV_az.add(event.values[2]);

            dataPointAx = new DataPoint(mT_AccelGraph, (Float)mV_ax.lastElement());
            dataPointAy = new DataPoint(mT_AccelGraph, (Float)mV_ay.lastElement());
            dataPointAz = new DataPoint(mT_AccelGraph, (Float)mV_az.lastElement());
            mT_ax.appendData(dataPointAx, true, GRAPH_X_BOUNDS);
            mT_ay.appendData(dataPointAy, true, GRAPH_X_BOUNDS);
            mT_az.appendData(dataPointAz, true, GRAPH_X_BOUNDS);

            mGraphAccel.getViewport().setMinX(mT_AccelGraph - GRAPH_X_BOUNDS);
            mGraphAccel.getViewport().setMaxX(mT_AccelGraph);

            if (isRecording) {
                accelTime.addValue(event.timestamp);
                accelX.addValue((Float)mV_ax.lastElement());
                accelY.addValue((Float)mV_ay.lastElement());
                accelZ.addValue((Float)mV_az.lastElement());
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            mT_GyroGraph += 1;

            mV_gx.add(event.values[0]);
            mV_gy.add(event.values[1]);
            mV_gz.add(event.values[2]);

            dataPointGx = new DataPoint(mT_GyroGraph, (Float)mV_gx.lastElement());
            dataPointGy = new DataPoint(mT_GyroGraph, (Float)mV_gy.lastElement());
            dataPointGz = new DataPoint(mT_GyroGraph, (Float)mV_gz.lastElement());
            mT_gx.appendData(dataPointGx, true, GRAPH_X_BOUNDS);
            mT_gy.appendData(dataPointGy, true, GRAPH_X_BOUNDS);
            mT_gz.appendData(dataPointGz, true, GRAPH_X_BOUNDS);

            mGraphGyro.getViewport().setMinX(mT_GyroGraph - GRAPH_X_BOUNDS);
            mGraphGyro.getViewport().setMaxX(mT_GyroGraph);

            if (isRecording) {
                gyroTime.addValue(event.timestamp);
                gyroX.addValue((Float)mV_gx.lastElement());
                gyroY.addValue((Float)mV_gy.lastElement());
                gyroZ.addValue((Float)mV_gz.lastElement());
            }
        }
    }

    public void initializeGraphs() {
        mGraphAccel = findViewById(R.id.graph_accel_raw);
        mGraphAccel.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        mGraphAccel.setBackgroundColor(Color.TRANSPARENT);
        mGraphAccel.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        mGraphAccel.getGridLabelRenderer().setVerticalLabelsVisible(false);
        mGraphAccel.getViewport().setXAxisBoundsManual(true);
        mGraphAccel.getViewport().setYAxisBoundsManual(true);
        mGraphAccel.getViewport().setMinX(0);
        mGraphAccel.getViewport().setMaxX(GRAPH_X_BOUNDS);
        mGraphAccel.getViewport().setMinY(-GRAPH_Y_BOUNDS);
        mGraphAccel.getViewport().setMaxY(GRAPH_Y_BOUNDS);
        mT_ax.setColor(mGraphColor[0]);
        mT_ax.setThickness(10);
        mGraphAccel.addSeries(mT_ax);
        mT_ay.setColor(mGraphColor[1]);
        mT_ay.setThickness(10);
        mGraphAccel.addSeries(mT_ay);
        mT_az.setColor(mGraphColor[2]);
        mT_az.setThickness(10);
        mGraphAccel.addSeries(mT_az);

        mGraphGyro = findViewById(R.id.graph_gyro_raw);
        mGraphGyro.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        mGraphGyro.setBackgroundColor(Color.TRANSPARENT);
        mGraphGyro.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        mGraphGyro.getGridLabelRenderer().setVerticalLabelsVisible(false);
        mGraphGyro.getViewport().setXAxisBoundsManual(true);
        mGraphGyro.getViewport().setYAxisBoundsManual(true);
        mGraphGyro.getViewport().setMinX(0);
        mGraphGyro.getViewport().setMaxX(GRAPH_X_BOUNDS);
        mGraphGyro.getViewport().setMinY(-GRAPH_Y_BOUNDS);
        mGraphGyro.getViewport().setMaxY(GRAPH_Y_BOUNDS);
        mT_gx.setColor(mGraphColor[0]);
        mT_gx.setThickness(10);
        mGraphGyro.addSeries(mT_gx);
        mT_gy.setColor(mGraphColor[1]);
        mT_gy.setThickness(10);
        mGraphGyro.addSeries(mT_gy);
        mT_gz.setColor(mGraphColor[2]);
        mT_gz.setThickness(10);
        mGraphGyro.addSeries(mT_gz);

    }

    public void recordButtonAction(View v) {
        final View v2 = v;
        final String label;
        final boolean isTraining;
        Timer startTimer = new Timer();

        // Create the timer to start data collection
        TimerTask startTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        accelTime.clear(); accelX.clear(); accelY.clear(); accelZ.clear();
                        gyroTime.clear(); gyroX.clear(); gyroY.clear(); gyroZ.clear();
                        isRecording = true;
                        v2.setEnabled(false);
                        mDisplayText.setText("Collecting data...");
                    }
                });
            }
        };

        // Figure out which button got pressed to determine label
        switch (v.getId()) {
            case R.id.Gesture1Button:
                label = mModel.outputClasses[0];
                isTraining = true;
                break;
            case R.id.Gesture2Button:
                label = mModel.outputClasses[1];
                isTraining = true;
                break;
            case R.id.Gesture3Button:
                label = mModel.outputClasses[2];
                isTraining = true;
                break;
            default:
                label = "?";
                isTraining = false;
                break;
        }

        // Create the timer to stop data collection
        Timer endTimer = new Timer();
        TimerTask endTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Add the recent gesture to the train or test set
                        isRecording = false;
                        mModel.addSample(accelTime, accelX, accelY, accelZ,
                                gyroTime, gyroX, gyroY, gyroZ, label, isTraining);

                        mDisplayText.setText("Gesture data collected");
                        v2.setEnabled(true);
                    }
                });
            }
        };

        // Start the timers
        startTimer.schedule(startTask, 0);
        endTimer.schedule(endTask, GESTURE_DURATION_SECS*1000);
    }

    public void trainButtonAction(View v) {
        // Make sure there is training data for each gesture
        for (int i = 0; i < mModel.outputClasses.length; ++i) {
            if (mModel.getNumTrainSamples(i) == 0) {
                Toast.makeText(getApplicationContext(), "Need examples for gesture" + (i+1),
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Train
        mModel.train();
        mDisplayText.setText("Training completed");
    }

    public void recognizeButtonAction (View v) {
        final View v2 = v;
        Timer startTimer = new Timer();

        // Create the timer to start data collection
        TimerTask startTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        accelTime.clear(); accelX.clear(); accelY.clear(); accelZ.clear();
                        gyroTime.clear(); gyroX.clear(); gyroY.clear(); gyroZ.clear();
                        isRecording = true;
                        v2.setEnabled(false);
                        mDisplayText.setText("Observing...");
                    }
                });
            }
        };

        // Create the timer to stop data collection
        Timer endTimer = new Timer();
        TimerTask endTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Add the recent gesture to the train or test set
                        isRecording = false;
                        mModel.addSample(accelTime, accelX, accelY, accelZ,
                                gyroTime, gyroX, gyroY, gyroZ, "?", false);

                        String result = mModel.test();
                        mDisplayText.setText("Gesture Recognized: "+result);

                        v2.setEnabled(true);
                    }
                });
            }
        };

        // Start the timers
        startTimer.schedule(startTask, 0);
        endTimer.schedule(endTask, GESTURE_DURATION_SECS*1000);
    }

    public void resetButtonAction (View v) {
        mDisplayText.setText("Collect data for gestures");
        mModel.resetTrainingData();
    }

    public static boolean checkPermission(Context _c, String strPermission)
    {
        int result = ContextCompat.checkSelfPermission(_c, strPermission);
        return (result == PackageManager.PERMISSION_GRANTED);
    }

    public static void requestPermission(String strPermission, int perCode, Activity _a)
    {
        ActivityCompat.requestPermissions(_a, new String[]{strPermission}, perCode);
    }
}
