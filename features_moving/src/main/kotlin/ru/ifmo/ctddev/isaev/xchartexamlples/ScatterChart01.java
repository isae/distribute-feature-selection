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
public class ScatterChart01 {

    public static void main(String[] args) {

        ScatterChart01 exampleChart = new ScatterChart01();
        XYChart chart = exampleChart.getChart();
        new SwingWrapper<>(chart).displayChart();
    }

    public XYChart getChart() {

        // Create Chart
        XYChart chart = new XYChartBuilder().width(800).height(600).build();

        // Customize Chart
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart.getStyler().setMarkerSize(5);

        // Series
        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        Random random = new Random();
        int size = 1000;
        for (int i = 0; i < size; i++) {
            xData.add(random.nextGaussian() / 1000);
            yData.add(-1000000 + random.nextGaussian());
        }
        chart.addSeries("Gaussian Blob", xData, yData);

        return chart;
    }

}
