package ru.ifmo.ctddev.isaev.xchartexamlples;


import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * @author iisaev
 */
public class DynamicScatter {

    public static void main(String[] args) throws Exception {


        double[][] initdata = getSample();

        // Create Chart
        XYChart chart = new XYChartBuilder().width(800).height(600).build();
        chart.addSeries("data", initdata[0], initdata[1]);

        // Customize Chart
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart.getStyler().setMarkerSize(5);

        // Show it
        final SwingWrapper<XYChart> sw = new SwingWrapper<>(chart);
        sw.displayChart();

        while (true) {

            Thread.sleep(100);
            double[][] data = getSample();

            chart.updateXYSeries("data", data[0], data[1], null);
            sw.repaintChart();
        }

    }

    private static double[][] getSample() {
        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        Random random = new Random();
        int size = 100000;
        for (int i = 0; i < size; i++) {
            xData.add(random.nextGaussian() / 1000);
            yData.add(-1000000 + random.nextGaussian());
        }
        return new double[][] {xData.stream().mapToDouble(Double::doubleValue).toArray(), yData.stream().mapToDouble(Double::doubleValue).toArray()};
    }
}