package swarms;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;
import math.geom2d.line.LineSegment2D;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class SwarmSim {

  // Simulation parameters
  private static final double simDuration = 100.0; // Time (in seconds) to simulate
  private static final int numAgents = 300;
  private static final Point2D min = new Point2D(0.0, 0.0);         // Bottom left of room rectangle
  private static final Point2D max = new Point2D(50.0, 50.0);     // Top right of the room rectangle
  private static final Point2D agentMin = max.scale(0.01);    // Bottom left of rectangle in which agents start
  private static final Point2D agentMax = max.scale(0.99); // Top right of rectangle in which agents start
  private static final double maxMove = 0.1;    // Maximum distance an agent can move before needing to be updated
  private static final double frameRate = 1.0;  // Rate at which to save frames for plotting
  private static final double spatialResolution = 0.2;     // Resolution at which to model the room as a graph
  private static final String movieFilePath = "/home/sss1/Desktop/projects/swarms/videos/out.mat";   // Output file from which to make MATLAB video
  private static final String plotFilePath = "/home/sss1/Desktop/withoutSpeedAttract.png";
//  private static final String movieFilePath = "/home/painkiller/Desktop/out.mat";   // Output file from which to make MATLAB video
//  private static final String plotFilePath = "/home/painkiller/Desktop/withoutSpeedAttract.png";
  private static final double exitBufferDist = 100.0; // Distance beyond the exits that the room graph should cover
  private static final boolean asymmetricInitialAgentDistribution = false; // Whether the initial distribution of agents is highly asymmetric
  private static final boolean makeMovie = true;

  // Simulation state variables
  private static Agent[] agents;
  private static PriorityQueue<Agent> orderedAgents;
  private static Room room;
  private static Point2D roomBottomLeft, roomTopRight;

  public static void main(String[] args) {
    double leftDoorWidth = 10.0;
    double rightDoorWidth = 10.0;
    // boolean hasObstacle; // = false;

    ArrayList<XYSeries> allPlots = new ArrayList<>();
    // allPlots.add(runTrial(leftDoorWidth, rightDoorWidth, hasObstacle));
    // hasObstacle = true;
    allPlots.add(runTrial(leftDoorWidth, rightDoorWidth, true, "With Obstacle"));
  }

  private static XYSeries runTrial(double leftDoorWidth, double rightDoorWidth, boolean hasObstacle, String label) {
    System.out.println("Constructing agents...");
    initializeAgents();
    System.out.println("Constructing room...");
    initializeRoom(leftDoorWidth, rightDoorWidth, hasObstacle);

    // Run the simulation
    double t = 0.0;
    Plotter plotter = new Plotter("Fraction of agents in room over time");
    MatPlotter matPlotter;
    if (makeMovie) {
      matPlotter = new MatPlotter(frameRate, agents, room);
    }
    System.out.println("Starting simulation...");
    XYSeries fractionInRoomOverTime = new XYSeries(label); // legend label of item to plot
    while (t < simDuration) {

      // Get next agent to update from PriorityQueue
      Agent nextAgent = orderedAgents.poll();
      boolean wasInRoom = agentIsInRoom(nextAgent);
      t = nextAgent.getNextUpdateTime();
      if (t % 10.0 < 0.002) { System.out.println("The time is " + t); } // Print a bit every 10 timesteps

      // Calculate forces, accelerate, move the agent, and update its priority
      nextAgent.update(t, room);

      // Add new social forces to appropriate agents
      updateSocialForces(agents, nextAgent);

      // Reinsert the agent back into the priority queue
      orderedAgents.add(nextAgent);

      // Track whether the agent left the room
      boolean isInRoom = agentIsInRoom(nextAgent);
      if (wasInRoom && !isInRoom) {
        double fracInRoom = 0.0;
        for (Agent agent : agents) {
          fracInRoom += agentIsInRoom(agent) ? 1.0 : 0.0;
        }
        fracInRoom /= numAgents;
        fractionInRoomOverTime.add(t, fracInRoom);
        if (fracInRoom < 1.0/numAgents) { break; } // no agents left in room; terminate simulation
      }

      if (makeMovie && t > matPlotter.getNextFrameTime()) {
        matPlotter.saveFrame(agents);
      }

    }

    // Export data necessary for movies as .mat file
    if (makeMovie) {
      matPlotter.writeToMAT(movieFilePath);
    }

//    // Old code for running just a single experiment
//    // Plot the fraction of agents in the room over time
//    plotter.plotFractionInRoomOverTime(plotFilePath);
//    plotter.pack();
//    RefineryUtilities.centerFrameOnScreen(plotter);
//    plotter.setVisible(true);

    return plotter.getFractionInRoomOverTime();

  }

  private static void initializeAgents() {

    agents = new Agent[numAgents];

    // Store all the agents sorted by order in which they need to be next updated
    orderedAgents = new PriorityQueue<>(numAgents, new AgentComparator());

    // Initialize the agents
    for (int i = 0; i < numAgents; i++) {
      if (asymmetricInitialAgentDistribution && i > numAgents/10) {
        Point2D shiftedAgentMax = new Point2D(agentMax.x()/4, agentMax.y());
        agents[i] = new Agent(i, agentMin, shiftedAgentMax, frameRate, maxMove, numAgents);
      } else {
        agents[i] = new Agent(i, agentMin, agentMax, frameRate, maxMove, numAgents);
      }

      agents[i].setNextUpdateTime(Math.min(maxMove / agents[i].getSpeed(), frameRate));
      orderedAgents.add(agents[i]);

    }

  }

  private static void initializeRoom(double leftDoorWidth, double rightDoorWidth, boolean hasObstacle) {

    double p = spatialResolution /10; // small perturbation to prevent endpoint bugs

    Vector2D rightShift = new Vector2D(exitBufferDist, 0.0);
    room = new Room(min.minus(rightShift), max.plus(rightShift), spatialResolution);
    Point2D topLeft = new Point2D(min.x() - p, max.y() + p);
    Point2D bottomLeft = new Point2D(min.x() - p, min.y() - p);
    Point2D bottomRight = new Point2D(max.x() + p, min.y() - p);
    Point2D topRight = new Point2D(max.x() + p, max.y() + p);

    roomBottomLeft = bottomLeft;
    roomTopRight = topRight;

    Point2D rightDoorUpper = new Point2D(topRight.x(), topRight.y()/2 + rightDoorWidth/2.0);
    Point2D rightDoorLower = new Point2D(topRight.x(), topRight.y()/2 - rightDoorWidth/2.0);
    Point2D leftDoorUpper = new Point2D(bottomLeft.x(), topRight.y()/2 + leftDoorWidth/2.0);
    Point2D leftDoorLower = new Point2D(bottomLeft.x(), topRight.y()/2 - leftDoorWidth/2.0);

    room.addWall(new LineSegment2D(topRight, topLeft)); // top wall
    room.addWall(new LineSegment2D(topLeft, leftDoorUpper)); // upper left wall
    room.addWall(new LineSegment2D(leftDoorLower, bottomLeft)); // lower left wall
    room.addWall(new LineSegment2D(bottomLeft, bottomRight)); // bottom wall
    room.addWall(new LineSegment2D(bottomRight, rightDoorLower)); // lower right wall
    room.addWall(new LineSegment2D(rightDoorUpper, topRight)); // upper right wall
    room.updateExitDistances();
    if (hasObstacle) {
      double horizontalObstacleOffset = 1.000001;
      Point2D obstacleUpper = new Point2D(bottomLeft.x() + horizontalObstacleOffset, topLeft.y() * 0.6);
      Point2D obstacleLower = new Point2D(bottomLeft.x() + horizontalObstacleOffset, topLeft.y() * 0.4);
      room.addWall(new LineSegment2D(obstacleUpper, obstacleLower));
    }

//    room.updateExitDistances();

  }


  /**
   * Returns whether the agent is still in the room (or has left)
   * @param agent an Agent whose position to check
   * @return true if the agent is still in the room, and false if it has left
   */
  private static boolean agentIsInRoom(Agent agent) {
    return roomBottomLeft.x() <= agent.getPos().x() &&
        agent.getPos().x() <= roomTopRight.x() &&
        roomBottomLeft.y() <= agent.getPos().y() &&
        agent.getPos().y() <= roomTopRight.y();
  }

  /**
   * Updates social forces due to movement of agent updatedAgent
   *
   * @param agents array of all agents, sorted by ID
   * @param updatedAgent ID of the agent that was just updated
   */
  private static void updateSocialForces(Agent[] agents, Agent updatedAgent) {

    for (Agent agent : agents) {

      // no self-interactions
      if (agent.getID() == updatedAgent.getID()) { continue; }

//      // no self-interactions or interactions with agents outside the room
//      if (agent.getID() == updatedAgent.getID() || !agentIsInRoom(agent)) { continue; }

      if (Interactions.collision(agent, updatedAgent)) {
        Interactions.push(updatedAgent, agent);
      }
      // Updated agent is attracted to more quickly moving agents
      if (agentIsInRoom(updatedAgent)) {
        Interactions.speedAttract(agent, updatedAgent);
      }
    }

  }

}
