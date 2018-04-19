package ubicomp.assignment1;

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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import ubicomp.a2kram.assignement1.R;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private GraphView mGraphFilteredMag;
    private LineGraphSeries<DataPoint> mT_FilteredMag = new LineGraphSeries<>();
    private int mT_Graph = 0;
    private static final int GRAPH_X_BOUNDS = 2000;
    private static final int GRAPH_Y_BOUNDS = 20;
    private int mGraphColor[] = {Color.argb(255,244,170,50),
            Color.argb(255, 60, 175, 240),
            Color.argb(225, 50, 220, 100)};

    private TextView mDisplayText;

    private Vector mV_x = new Vector<Double>();
    private Vector mV_y = new Vector<Double>();
    private Vector mV_z = new Vector<Double>();
    private Vector mV_mag = new Vector<Double>();
    private Vector mV_FilteredMag = new Vector<Double>();

    private boolean mCalibrationComplete = false;
    private boolean mAbove = false;

    private StepCounter mStepCounter;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI initializations
        mDisplayText = findViewById(R.id.text_view);
        initializeGraph();

        // Sensor initialization
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Request external storage permission if we dont have it
        if (!checkPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 0, this);
        }

        // Create StepCounter object
        mStepCounter = new StepCounter();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double magnitude;
        double filteredMagnitude;
        DataPoint dataPointFilteredMag;

        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        magnitude = event.values[0] * event.values[0] + event.values[1] * event.values[1] +
                event.values[2] * event.values[2];
        magnitude = Math.sqrt (magnitude);
        mT_Graph += 1;

        mV_x.add(event.values[0]);
        mV_y.add(event.values[1]);
        mV_z.add(event.values[2]);
        mV_mag.add(magnitude);
        filteredMagnitude = magnitude;

        if (mV_FilteredMag.size() > 0) {
            filteredMagnitude = (Double)mV_FilteredMag.lastElement() + 0.1 * (magnitude -
                (Double) mV_FilteredMag.lastElement());
        }

        mV_FilteredMag.add(filteredMagnitude);

        dataPointFilteredMag = new DataPoint(mT_Graph, (Double)mV_FilteredMag.lastElement());

        mT_FilteredMag.appendData(dataPointFilteredMag, true, GRAPH_X_BOUNDS);
        mGraphFilteredMag.getViewport().setMinX(mT_Graph - GRAPH_X_BOUNDS);
        mGraphFilteredMag.getViewport().setMaxX(mT_Graph);

        if (mCalibrationComplete) {
            mStepCounter.updateStepCount(filteredMagnitude);
            mDisplayText.setText("Steps = " + mStepCounter.getStepCount());
        }
    }

    public void initializeGraph() {
        mGraphFilteredMag = findViewById(R.id.graph_filtered_mag);
        mGraphFilteredMag.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        mGraphFilteredMag.setBackgroundColor(Color.TRANSPARENT);
        mGraphFilteredMag.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        mGraphFilteredMag.getGridLabelRenderer().setVerticalLabelsVisible(true);
        mGraphFilteredMag.getViewport().setXAxisBoundsManual(true);
        mGraphFilteredMag.getViewport().setYAxisBoundsManual(true);
        mGraphFilteredMag.getViewport().setMinX(0);
        mGraphFilteredMag.getViewport().setMaxX(GRAPH_X_BOUNDS);
        mGraphFilteredMag.getViewport().setMinY(-GRAPH_Y_BOUNDS);
        mGraphFilteredMag.getViewport().setMaxY(GRAPH_Y_BOUNDS);
        mT_FilteredMag.setColor(mGraphColor[0]);
        mT_FilteredMag.setThickness(10);
        mGraphFilteredMag.addSeries(mT_FilteredMag);
    }

    public void saveButtonAction(View v) {
        String baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String fileName = "StepCounterData.csv";
        String filePath = baseDir + File.separator + fileName;
        File file = new File(filePath );
        CSVWriter writer;

        try {
            if(file.exists() && !file.isDirectory()){
                FileWriter mFileWriter = new FileWriter(filePath , false);
                writer = new CSVWriter(mFileWriter);
            }
            else {
                writer = new CSVWriter(new FileWriter(filePath));
            }

            for (int i = 0; i < mT_Graph; ++i) {
                String[] data = {String.valueOf(i), String.valueOf(mV_x.elementAt(i)),
                        String.valueOf(mV_y.elementAt(i)), String.valueOf(mV_z.elementAt(i)),
                        String.valueOf(mV_mag.elementAt(i)),
                        String.valueOf(mV_FilteredMag.elementAt(i))};
                writer.writeNext(data);
            }

            writer.close();
        }
        catch (IOException e) {
            Log.d ("Exception", "" + e);
        }
    }

    public void startButtonAction (View v) {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        // Create and schedule a task that runs post calibration
        Timer startTimer = new Timer();
        TimerTask startTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mStepCounter.setStationaryAvg(Common.vectorAverage(mV_FilteredMag));
                        mCalibrationComplete = true;
                    }
                });
            }
        };

        mDisplayText.setText("Stand still");
        startTimer.schedule(startTask, 2000);
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
