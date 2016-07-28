package swarms;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;

import java.util.ArrayList;

/**
 * This class records agent positions at regular time increments for plotting later.
 * Eventually, it should also save these in .mat format.
 *
 * Created by sss1 on 7/27/16.
 */
class Plotter {

  private ArrayList<Point2D[]> positions;
  private ArrayList<Room> rooms;
  private final double[] radii;
  private final double frameRate;
  private double nextFrameTime;

  Plotter(double frameRate, Agent[] agents, Room room) {
    this.frameRate = frameRate;
    radii = new double[agents.length];
    for (int i = 0; i < agents.length; i++) {
      radii[i] = agents[i].getRadius();
    }
    positions = new ArrayList<>();
    rooms = new ArrayList<>();
    nextFrameTime = 0.0;
    saveFrame(agents, room);
  }

  /**
   * Saves a frame of the simulation at the next frame time.
   *
   * @param agents array of all agents
   */
  void saveFrame(Agent[] agents, Room room) {
    Point2D[] newPositions = new Point2D[agents.length];
    for (int i = 0; i < agents.length; i++) {
      newPositions[i] = agents[i].getPos();
    }
    positions.add(newPositions);
    rooms.add(room); // TODO: Come up with a more compressed representation of the room!
    System.out.println("Saved frame " + positions.size() + " at time " + nextFrameTime + ".");
    nextFrameTime = nextFrameTime + frameRate;
  }

  /**
   * Saves the necessary information to create a frame-by-frame MATLAB movie in a .mat file.
   * @param filepath to which to save the .mat file (including the .mat suffix)
   */
  void saveToMAT(String filepath) {
    // TODO: Save positions, radii, rooms, and possibly other history
  }

  /**
   * @return The simulation time at which saveFrame should next be called.
   */
  double getNextFrameTime() {
    return nextFrameTime;
  }
}