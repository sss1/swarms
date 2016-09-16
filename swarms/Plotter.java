package swarms;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.ApplicationFrame;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Stores necessary information over time and produces plots.
 *
 * Created by painkiller on 8/20/16.
 */
class Plotter extends ApplicationFrame {

  private final double simDuration;

  Plotter(final String title, double simDuration) {
    super(title);
    this.simDuration = simDuration;
  }

  void plotMultiple(ArrayList<XIntervalSeriesCollection> allSeries, String plotFilePath) {
    final NumberAxis domainAxis = new NumberAxis("Time");
    final ValueAxis rangeAxis = new NumberAxis("Fraction of Agents");
    final XYItemRenderer renderer0 = new XYErrorRenderer();
    renderer0.setSeriesStroke(0, new BasicStroke(1.0f));
    XYPlot plot = new XYPlot(allSeries.get(0), domainAxis, rangeAxis, renderer0);
    for (int i = 1; i < allSeries.size(); i++) {
      plot.setDataset(i, allSeries.get(i));
      final XYItemRenderer renderer = new XYErrorRenderer();
      renderer.setSeriesStroke(0, new BasicStroke(1.0f));
      plot.setRenderer(i, renderer);

    }
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    NumberAxis domain = (NumberAxis) plot.getDomainAxis();
    domain.setTickUnit(new NumberTickUnit(10.0));
    domain.setVerticalTickLabels(true);
    domain.setRange(0.0, simDuration);

    final JFreeChart chart = new JFreeChart("Fraction of Agents in Room over Time", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    final ChartPanel panel = new ChartPanel(chart, true, true, true, true, true);
    panel.setPreferredSize(new java.awt.Dimension(800, 600));
    setContentPane(panel);
    try { // Try to save chart
      ChartUtilities.saveChartAsPNG(new File(plotFilePath), chart, 600, 600);
      System.out.println("Saved plot to " + plotFilePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static XIntervalSeriesCollection averageTrials(ArrayList<XYSeries> results, String label) {
    int maxLen = 0;
    XIntervalSeries averagedSeries = new XIntervalSeries(label);
    double yIncrement = 1.0 - ((double) results.get(0).getY(0));
    for(XYSeries series : results) {
      maxLen = Math.max(maxLen, series.getItemCount());
    }
    for (int i = 0; i < maxLen; i++) {
      int numSamples = 0;
      double sum = 0.0;
      for (XYSeries series : results) { // compute pointwise means
        if (series.getItemCount() > i) {
          sum += (double) (series.getX(i));
          numSamples++;
        }
      }
      double mean = sum / numSamples;
      double sumSquaredDeviations = 0.0;
      for (XYSeries series : results) { // compute pointwise standard deviations
        if (series.getItemCount() > i) {
          sumSquaredDeviations += Math.pow(((double) series.getX(i)) - mean, 2.0);
        }
      }
      double standardDeviation = Math.sqrt(sumSquaredDeviations / numSamples);
      double zScore95 = 1.96; // Number of standard deviations away from mean for a two-sided 95% normal confidence interval
      double CIRadius = standardDeviation * zScore95 / Math.sqrt(numSamples);
      averagedSeries.add(mean,
          mean - CIRadius, // lower 95% confidence bound
          mean + CIRadius, // upper 95% confidence bound
          1 - (i + 1) * yIncrement); // TODO: Check that this doesn't break if agents re-enter the room
    }
    XIntervalSeriesCollection averagedSeriesAsCollection = new XIntervalSeriesCollection();
    averagedSeriesAsCollection.addSeries(averagedSeries);
    return averagedSeriesAsCollection;
  }


}
