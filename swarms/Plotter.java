package swarms;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
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

//  private final XYSeries fractionInRoomOverTime;

  Plotter(final String title) {
    super(title);
  }

  void plotMultiple(ArrayList<XYSeries> allSeries, String plotFilePath) {
    final NumberAxis domainAxis = new NumberAxis("Time");
    final ValueAxis rangeAxis = new NumberAxis("Fraction of Agents");
    final XYItemRenderer renderer0 = new XYLineAndShapeRenderer(true, false);
    XYPlot plot = new XYPlot(new XYSeriesCollection(allSeries.get(0)), domainAxis, rangeAxis, renderer0);
    for (int i = 1; i < allSeries.size(); i++) {
      plot.setDataset(i, new XYSeriesCollection(allSeries.get(i)));
      plot.setRenderer(i, new XYLineAndShapeRenderer(true, false));
    }
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    NumberAxis domain = (NumberAxis) plot.getDomainAxis();
    domain.setTickUnit(new NumberTickUnit(10.0));
    domain.setVerticalTickLabels(true);

    final JFreeChart chart = new JFreeChart("Fraction of Agents in Room over Time", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    final ChartPanel panel = new ChartPanel(chart, true, true, true, true, true);
    panel.setPreferredSize(new java.awt.Dimension(800, 600));
    setContentPane(panel);
    try { // Try to save chart
      ChartUtilities.saveChartAsPNG(new File(plotFilePath), chart, 500, 250);
      System.out.println("Saved plot to " + plotFilePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
