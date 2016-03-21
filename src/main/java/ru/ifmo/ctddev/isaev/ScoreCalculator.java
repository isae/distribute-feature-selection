package ru.ifmo.ctddev.isaev;

import java.util.List;


/**
 * @author iisaev
 */
public class ScoreCalculator {
    public double calculateF1Score(List<Integer> expectedList, List<Integer> actualList) {
        if (expectedList.size() != actualList.size()) {
            throw new IllegalArgumentException("Expected and  actual lists must have same size");
        }
        int truePositive = 0;
        int trueNegative = 0;
        int falsePositive = 0;
        int falseNegative = 0;
        for (int i = 0; i < expectedList.size(); ++i) {
            int expected = expectedList.get(i);
            int actual = actualList.get(i);
            if (expected == 1 && actual == 1) {
                ++truePositive;
            }
            if (expected == 0 && actual == 0) {
                ++trueNegative;
            }
            if (expected == 0 && actual == 1) {
                ++falsePositive;
            }
            if (expected == 1 && actual == 0) {
                ++falseNegative;
            }
        }
        double precision = (double) truePositive / (truePositive + falsePositive);
        double recall = (double) truePositive / (truePositive + falseNegative);
        double result = 2 * precision * recall / (precision + recall);
        return Double.isNaN(result) ? Double.MIN_VALUE : result;
    }
}
