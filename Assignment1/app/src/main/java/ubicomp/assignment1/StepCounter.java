package ubicomp.assignment1;

public class StepCounter {
    private static final double STEP_COUNT_THRESHOLD  = 10.37;
    private double mStationaryAvg;
    private int mStepCount;
    private boolean mAbove;

    StepCounter(){
        mStationaryAvg = 0;
    }

    public void updateStepCount (double mag) {
        if (mag> STEP_COUNT_THRESHOLD) {
            mAbove = true;
        }
        if (mag < mStationaryAvg) {
            if (mAbove) {
                mStepCount += 1;
            }

            mAbove = false;
        }
    }

    public void setStationaryAvg (double avg) {
        mStationaryAvg = avg;
    }

    public int getStepCount () {
        return mStepCount;
    }
}
