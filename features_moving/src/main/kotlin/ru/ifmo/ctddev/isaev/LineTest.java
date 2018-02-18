package ru.ifmo.ctddev.isaev;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.rendering.canvas.Quality;


/**
 * @author iisaev
 */
public class LineTest extends AbstractAnalysis {
    public static void main(String[] args) throws Exception {
        AnalysisLauncher.open(new LineTest());
    }

    @Override
    public void init() throws Exception {
        chart = AWTChartComponentFactory.chart(Quality.Fastest, getCanvasType());
        LineStrip lineStrip = new LineStrip(
                new org.jzy3d.plot3d.primitives.Point(new Coord3d(0.0, 1.0, 3.0)),
                new org.jzy3d.plot3d.primitives.Point(new Coord3d(1.0, 2.0, 3.0))
        );
        lineStrip.setWireframeColor(Color.RED);
        chart.getScene().getGraph().add(lineStrip);
    }
}
