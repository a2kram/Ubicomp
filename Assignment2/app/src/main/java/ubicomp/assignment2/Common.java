package ubicomp.assignment2;

import java.util.Vector;

public class Common {
    public static Double vectorAverage(Vector<Double> v) {
        Double avg = 0.0;

        for (int i = 0; i < v.size(); ++i) {
            avg += v.elementAt(i);
        }

        return avg / v.size();
    }
}
