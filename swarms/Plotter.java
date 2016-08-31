package swarms;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Stores necessary information over time and produces plots.
 *
 * Created by painkiller on 8/20/16.
 */
class Plotter extends ApplicationFrame {

  private final XYSeries fractionInRoomOverTime;

  Plotter(final String title) {
    super(title);
    fractionInRoomOverTime = new XYSeries("Fraction of agents in the room."); // legend label of item to plot
    fractionInRoomOverTime.add(0.0, 1.0);
  }

  void addFractionInRoom(double t, double fractionInRoom) {
    fractionInRoomOverTime.add(t, fractionInRoom);
  }

  void plotFractionInRoomOverTime(String plotFilePath) {
    final XYSeriesCollection data = new XYSeriesCollection(fractionInRoomOverTime);
    final JFreeChart chart = ChartFactory.createXYLineChart(
        "Fraction of agents in room over time", // plot title
        "Time",                                 // X-axis label
        "Fraction of agents in room",           // Y-axis label
        data,                                   // data to plot
        PlotOrientation.VERTICAL,               // plot vertically
        true,                                   // include legend
        true,                                   // include tooltips
        false                                   // don't include URLs
    );
    final ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(500, 250));
    setContentPane(chartPanel);

    try { // Try to save chart
      ChartUtilities.saveChartAsPNG(new File(plotFilePath), chart, 500, 250);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  XYSeries getFractionInRoomOverTime() {
    return fractionInRoomOverTime;
  }

  static void plotMultiple(ArrayList<XYSeries> allSeries) {
    final NumberAxis domainAxis = new NumberAxis("Time");
    final ValueAxis rangeAxis = new NumberAxis("Fraction of Agents");
    final XYItemRenderer renderer0 = new XYLineAndShapeRenderer(true, false);
    XYPlot plot = new XYPlot(new XYSeriesCollection(allSeries.get(0)), domainAxis, rangeAxis, renderer0);
    for (int i = 1; i < allSeries.size(); i++) {
      plot.setDataset(i, new XYSeriesCollection(allSeries.get(i)));
      plot.setRenderer(i, new XYLineAndShapeRenderer(true, false));
      /*TODO: Finish this code, based on http://stackoverflow.com/questions/18789343/draw-a-multiple-plot-with-jfreechart-bar-xy*/
    }
  }

}
