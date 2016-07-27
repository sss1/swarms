package swarms;

import math.geom2d.Vector2D;

import java.util.ArrayList;

/**
 * This class records agent positions at regular time increments for plotting later.
 * Eventually, it should also save these in .mat format.
 *
 * Created by sss1 on 7/27/16.
 */
class Plotter {

  private ArrayList<Vector2D[]> positions;
  private final double[] radii;
  private final double frameRate;
  private double nextFrameTime;

  Plotter(Agent[] agents, double frameRate) {
    this.frameRate = frameRate;
    radii = new double[agents.length];
    for (int i = 0; i < agents.length; i++) {
      radii[i] = agents[i].getRadius();
    }
    positions = new ArrayList<>();
    nextFrameTime = 0.0;
    saveFrame(agents);
  }

  /**
   * Saves the positions of all the agents at the next frame time.
   *
   * @param agents array of all agents
   */
  void saveFrame(Agent[] agents) {
    Vector2D[] newPositions = new Vector2D[agents.length];
    for (int i = 0; i < agents.length; i++) {
      newPositions[i] = agents[i].getPos();
    }
    positions.add(newPositions);
    System.out.println("Saved frame " + positions.size() + " at time " + nextFrameTime + ".");
    nextFrameTime = nextFrameTime + frameRate;
  }

  /**
   * @return The simulation time at which saveFrame should next be called.
   */
  double getNextFrameTime() {
    return nextFrameTime;
  }
}
