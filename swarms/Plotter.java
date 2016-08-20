package swarms;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

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
  }

  void addFractionInRoom(double t, double fractionInRoom) {
    fractionInRoomOverTime.add(t, fractionInRoom);
  }

  void plotFractionInRoomOverTime() {
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
  }

}
