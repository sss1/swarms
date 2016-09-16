package swarms;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;
import math.geom2d.line.LineSegment2D;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class SwarmSim {

  // Basic simulation parameters
  private static final double simDuration = 20.0; // Time (in seconds) to simulate
  private static final int numAgents = 30; // Number of agents in the simulation

  // Parameters determining the size of the room
  private static final Point2D min = new Point2D(0.0, 0.0);   // Bottom left of room rectangle
  private static final Point2D max = new Point2D(50.0, 50.0); // Top right of the room rectangle

  // Parameters determining starting positions of agents
  private static final Point2D agentMin = max.scale(0.01);  // Bottom left of rectangle in which agents start
  private static final Point2D agentMax = max.scale(0.99);  // Top right of rectangle in which agents start
  private static final boolean asymmetricInitialAgentDistribution = false; // Whether the initial distribution of agents is highly asymmetric

  // Parameters determining "fineness" of the simulation. These heavily affect runtime.
  private static final double maxMove = 0.1;    // Maximum distance an agent can move before needing to be updated
  private static final double frameRate = 1.0;  // Rate at which to save frames for plotting
  private static final double spatialResolution = 0.2;     // Resolution at which to model the room as a graph
  private static final double exitBufferDist = 100.0; // Distance beyond the exits that the room graph should cover

  // Parameters determining the output of the simulation
//  private static final String movieFilePath = "/home/painkiller/Desktop/out.mat";   // Output file from which to make MATLAB video
//  private static final String plotFilePath = "/home/painkiller/Desktop/withoutSpeedAttract.png";
  private static final String movieFilePath = "/home/sss1/Desktop/projects/swarms/videos/out.mat";   // Output file from which to make MATLAB video
  private static final String plotFilePath = "/home/sss1/Desktop/test_" + numAgents + "agents_" + simDuration + "seconds.png";
  private static final boolean makeMovie = true;

  // Simulation state variables
  private static Agent[] agents;
  private static PriorityQueue<Agent> orderedAgents;
  private static Room room;
  private static Point2D roomBottomLeft, roomTopRight;

  @SuppressWarnings("ConstantConditions") // Several constant variables are explicitly named here just for readability
  public static void main(String[] args) {
    final double leftDoorWidth = 10.0;
    final double rightDoorWidth = 10.0;
    final boolean hasObstacle = false;

    boolean hasOrient = false;
    boolean hasAttract = false;

    ArrayList<XYSeries> allPlots = new ArrayList<>();

    allPlots.add(runTrial(leftDoorWidth, rightDoorWidth, hasObstacle, "No communication", hasOrient, hasAttract));
    hasOrient = true;
    allPlots.add(runTrial(leftDoorWidth, rightDoorWidth, hasObstacle, "No speed", hasOrient, hasAttract));
    hasAttract = true;
    allPlots.add(runTrial(leftDoorWidth, rightDoorWidth, hasObstacle, "Full communication", hasOrient, hasAttract));


    (new Plotter("Test", simDuration)).plotMultiple(allPlots, plotFilePath);
  }

  private static XYSeries runTrial(double leftDoorWidth,
                                   double rightDoorWidth,
                                   boolean hasObstacle,
                                   String label,
                                   boolean hasOrient,
                                   boolean hasAttract) {

    System.out.println("Preparing \"" + label + "\" condition...");
    System.out.println("Constructing agents...");
    initializeAgents();
    System.out.println("Constructing room...");
    initializeRoom(leftDoorWidth, rightDoorWidth, hasObstacle, "Basic");

    // Run the simulation
    double t = 0.0;
    MatPlotter matPlotter;
    if (makeMovie) {
      matPlotter = new MatPlotter(frameRate, agents, room);
    }
    System.out.println("Starting simulation...");
    XYSeries fractionInRoomOverTime = new XYSeries(label); // legend label of item to plot
    // Terminate the simulation when there are no agents left in the room or when the simulation duration has ended;
    // whichever comes first
    while (t < simDuration && getNumInRoom(agents) >= 1.0) {

      // Get next agent to update from PriorityQueue
      Agent nextAgent = orderedAgents.poll();
      boolean wasInRoom = agentIsInRoom(nextAgent);
      t = nextAgent.getNextUpdateTime();
      if (t % 10.0 < 0.002) { System.out.println("The time is " + t); } // Print a bit every 10 timesteps

      // Calculate forces, accelerate, move the agent, and update its priority
      nextAgent.update(t, room);

      // Add new social forces to appropriate agents
      updateSocialForces(agents, nextAgent, hasOrient, hasAttract);

      // Reinsert the agent back into the priority queue
      orderedAgents.add(nextAgent);

      // Track whether the agent left the room
      boolean isInRoom = agentIsInRoom(nextAgent);
      if (wasInRoom && !isInRoom) {
        double fracInRoom = getFracInRoom(agents);
        fractionInRoomOverTime.add(t, fracInRoom);
      }

      if (makeMovie && t > matPlotter.getNextFrameTime()) {
        matPlotter.saveFrame(agents);
      }

    }

    // Add a final point to the plot at the last frame
    fractionInRoomOverTime.add(t, getFracInRoom(agents));

    // Export data necessary for movies as .mat file
    if (makeMovie) {
      matPlotter.writeToMAT(movieFilePath);
    }

    return fractionInRoomOverTime;

  }

  private static double getFracInRoom(Agent[] agents) {
    return getNumInRoom(agents) / numAgents;
  }

  private static double getNumInRoom(Agent[] agents) {
    double fracInRoom = 0.0;
    for (Agent agent : agents) {
      fracInRoom += agentIsInRoom(agent) ? 1.0 : 0.0;
    }
    return fracInRoom;
  }

  private static void initializeAgents() {

    agents = new Agent[numAgents];

    // Store all the agents sorted by order in which they need to be next updated
    orderedAgents = new PriorityQueue<>(Math.max(numAgents, 1), new AgentComparator());

    // Initialize the agents
    for (int i = 0; i < numAgents; i++) {
      if (asymmetricInitialAgentDistribution && i > numAgents/4) {
        Point2D shiftedAgentMax = new Point2D(agentMax.x()/4, agentMax.y());
        agents[i] = new Agent(i, agentMin, shiftedAgentMax, frameRate, maxMove, numAgents);
      } else {
        agents[i] = new Agent(i, agentMin, agentMax, frameRate, maxMove, numAgents);
      }

      agents[i].setNextUpdateTime(Math.min(maxMove / agents[i].getSpeed(), frameRate));
      orderedAgents.add(agents[i]);

    }

  }

  private static void initializeRoom(double leftDoorWidth, double rightDoorWidth, boolean hasObstacle, String roomType) {

    if (roomType.equalsIgnoreCase("Gates8")) {
      buildGates8();
    } else if (roomType.equalsIgnoreCase("Basic")) {
      buildBasic(leftDoorWidth, rightDoorWidth, hasObstacle);
    } else {
      throw new IllegalArgumentException("Room type must be one of \"Gates8\" or \"Basic\".");
    }
  }

  private static void buildGates8() {
    Point2D min = new Point2D(-10.0, -15.0);
    Point2D max = new Point2D(60.0, 60.0);
    room = new Room(min, max, spatialResolution);

    // Construct out walls, going clockwise from top
    room.addWall(new LineSegment2D(0.0, 50.001, 55.0, 50.001)); // main top wall
    room.addWall(new LineSegment2D(55.001, 50.0, 50.001, 30.0)); // upper-right block right wall
    room.addWall(new LineSegment2D(50.0, 29.999, 35.0, 29.999)); // upper-right block bottom wall
    room.addWall(new LineSegment2D(35.001, 30.0, 30.001, 0.0)); // main corridor middle right wall
    room.addWall(new LineSegment2D(30.0, 0.001, 55.0, -5.001)); // bottom corridor top wall
    room.addWall(new LineSegment2D(55.001, -5.0, 50.001, -10.0)); // bottom corridor right end
    room.addWall(new LineSegment2D(50.0, -10.001, 0.0, -0.001)); // main bottom wall
    room.addWall(new LineSegment2D(-0.001, 0.0, -0.001, 50.0)); // main left wall

    // Construct 8102 block
    room.addWall(new LineSegment2D(10.001, 19.999, 24.999, 19.999)); // top
    room.addWall(new LineSegment2D(24.999, 19.999, 19.999, 0.001)); // right
    room.addWall(new LineSegment2D(19.999, 0.001, 10.001, 10.001)); // bottom
    room.addWall(new LineSegment2D(10.001, 10.001, 10.001, 19.999)); // left

    // Construct 8118 block: TODO
    // Construct 8126 block: TODO

    // Construct 8216 block
    room.addWall(new LineSegment2D(35.001, 44.999, 44.999, 44.999)); // top
    room.addWall(new LineSegment2D(44.999, 44.999, 39.999, 35.001)); // right
    room.addWall(new LineSegment2D(39.999, 35.001, 30.001, 35.001)); // bottom
    room.addWall(new LineSegment2D(30.001, 35.001, 35.001, 44.999)); // left



    // TODO: Add doors/exits, interior walls, and reimplement communication in terms of graph distances
  }

  private static void buildBasic(double leftDoorWidth, double rightDoorWidth, boolean hasObstacle) {

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

    room.addExit(new Point2D(min.minus(rightShift).x() + p, max.y()/2));
    room.addExit(new Point2D(max.plus(rightShift).x() - p, max.y()/2));

    room.addWall(new LineSegment2D(topRight, topLeft)); // top wall
    room.addWall(new LineSegment2D(topLeft, leftDoorUpper)); // upper left wall
    room.addWall(new LineSegment2D(leftDoorLower, bottomLeft)); // lower left wall
    room.addWall(new LineSegment2D(bottomLeft, bottomRight)); // bottom wall
    room.addWall(new LineSegment2D(bottomRight, rightDoorLower)); // lower right wall
    room.addWall(new LineSegment2D(rightDoorUpper, topRight)); // upper right wall

    // Agents shouldn't know about the obstacle, to we update exit distances BEFORE adding the obstacle
    room.updateExitDistances();
    if (hasObstacle) {
      double horizontalObstacleOffset = 1.000001;
      Point2D obstacleUpper = new Point2D(bottomLeft.x() + horizontalObstacleOffset, topLeft.y() * 0.6);
      Point2D obstacleLower = new Point2D(bottomLeft.x() + horizontalObstacleOffset, topLeft.y() * 0.4);
      room.addWall(new LineSegment2D(obstacleUpper, obstacleLower));
    }
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
  private static void updateSocialForces(Agent[] agents, Agent updatedAgent, boolean hasOrient, boolean hasAttract) {

    if (agentIsInRoom(updatedAgent)) {
      for (Agent agent : agents) {

        // don't include self-interactions
        if (agent.getID() != updatedAgent.getID()) {

          // Updated agent pushes away from colliding agents
          if (Interactions.collision(agent, updatedAgent)) { Interactions.push(updatedAgent, agent); }

          // Updated agent tries to orient with nearby agents
          if (hasOrient) { Interactions.orient(agent, updatedAgent); }

          // Updated agent is attracted to more quickly moving agents
          if (hasAttract) { Interactions.speedAttract(agent, updatedAgent); }
        }

      }

    }

  }

}
