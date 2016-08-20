package swarms;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import math.geom2d.Point2D;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class records agent positions at regular time increments for plotting later.
 * Eventually, it should also save these in .mat format.
 *
 * Created by sss1 on 7/27/16.
 */
class MatPlotter {

  private ArrayList<Point2D[]> positions;
  private double[][] room;
  private double[][] walls;
  private final double[] radii;
  private final double frameRate;
  private double nextFrameTime;

  MatPlotter(double frameRate, Agent[] agents, Room room) {
    this.frameRate = frameRate;
    radii = new double[agents.length];
    for (int i = 0; i < agents.length; i++) {
      radii[i] = agents[i].getRadius();
    }
    positions = new ArrayList<>();
    nextFrameTime = 0.0;
    this.room = room.getAsArray();
    this.walls = room.getWallsAsArray();
    saveFrame(agents);
  }

  /**
   * Saves a frame of the simulation at the next frame time.
   *
   * @param agents array of all agents
   */
  void saveFrame(Agent[] agents) {
    Point2D[] newPositions = new Point2D[agents.length];
    for (int i = 0; i < agents.length; i++) {
      newPositions[i] = agents[i].getPos();
    }
    positions.add(newPositions);
    // System.out.println("Saved frame " + positions.size() + " at time " + nextFrameTime + ".");
    nextFrameTime = nextFrameTime + frameRate;
  }

  /**
   * Saves the necessary information to create a frame-by-frame MATLAB movie in a .mat file. Contents:
   * - Agents' x and y positions are stored over time in two
   *   numTimeSteps X numAgents double arrays (positionsX and positionsY)
   * - Agents' radii are stored in a single array of length numAgents
   * - The room is stored as an edge list of pairs of vertex coordinates between which there is an edge
   *   (e.g., an edge between vertices u and v is encoded as [u.x u.y v.x v.y])
   *
   * @param filepath to which to save the .mat file (including the .mat suffix)
   */
  void writeToMAT(String filepath) {

    ArrayList<MLArray> variableList = new ArrayList<>(2);

    // reformat the positions to two numTimeSteps X numAgents double arrays, one each for x and y coordinates
    int numTimeSteps = positions.size();
    int numAgents = positions.get(0).length;
    double[][] positionsXAsArray = new double[numTimeSteps][numAgents];
    double[][] positionsYAsArray = new double[numTimeSteps][numAgents];
    for (int i = 0; i < numTimeSteps; i++) {
      for (int j = 0; j < numAgents; j++) {
        positionsXAsArray[i][j] = positions.get(i)[j].x();
        positionsYAsArray[i][j] = positions.get(i)[j].y();
      }
    }
    variableList.add(new MLDouble("positionsX", positionsXAsArray));
    variableList.add(new MLDouble("positionsY", positionsYAsArray));

    variableList.add(new MLDouble("room", room));
    variableList.add(new MLDouble("walls", walls));
    variableList.add(new MLDouble("radii", radii, radii.length));

    try {
      new MatFileWriter(filepath, variableList);
      System.out.println("Saved results to file: " + filepath);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  /**
   * @return The simulation time at which saveFrame should next be called.
   */
  double getNextFrameTime() {
    return nextFrameTime;
  }
}