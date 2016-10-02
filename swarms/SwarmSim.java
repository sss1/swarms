package swarms;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;
import math.geom2d.line.LineSegment2D;
import org.jfree.data.xy.XIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class SwarmSim {

  private enum RoomType { BASIC, GATES8 }

  // Basic simulation parameters
  private static final double simDuration = 250.0; // Time (in seconds) to simulate
  private static final int numAgents = 200; // Number of agents in the simulation

  // Parameters determining the size of the room
  private static final Point2D min = new Point2D(0.0, 0.0);   // Bottom left of room rectangle
  private static final Point2D max = new Point2D(50.0, 50.0); // Top right of the room rectangle
  private static final RoomType roomType = RoomType.GATES8;

  // Parameters determining starting positions of agents
  private static final Point2D agentMin = max.scale(0.01).plus(new Vector2D(0.0, -10.0));  // Bottom left of rectangle in which agents start
  private static final Point2D agentMax = max.scale(0.99);  // Top right of rectangle in which agents start
  private static final boolean asymmetricInitialAgentDistribution = false; // Whether the initial distribution of agents is highly asymmetric

  // Parameters determining "fineness" of the simulation.
  // These heavily affect runtime, but, beyond a point, shouldn't affect results.
  // Runtime and memory are O(1/spatialResolution^4), O(1/maxMove)
  private static final double maxMove = 0.1;    // Maximum distance an agent can move before needing to be updated
  private static final double frameRate = 1.0;  // Rate at which to save frames for plotting
  private static final double spatialResolution = 0.7;  // Resolution at which to model the room as a graph; TODO: used to be 0.2
  private static final double exitBufferDist = 100.0;   // Distance beyond the exits that the room graph should cover

  // Parameters determining the output of the simulation
//  private static final String movieFilePath = "/home/painkiller/Desktop/out.mat";   // Output file from which to make MATLAB video
//  private static final String plotFilePath = "/home/painkiller/Desktop/withoutSpeedAttract.png";
  private static final String movieFilePath = "/home/sss1/Desktop/projects/swarms/videos/out.mat";   // Output file from which to make MATLAB video
  private static final String plotFilePath = "/home/sss1/Desktop/gates8/gates8_obstacle_" + numAgents + "agents_" + simDuration + "seconds.png";
  private static final boolean makeMovie = true;

  private static final int numTrials = 10; // Number of trials over which to average results and compute error bars

  // Simulation state variables
  private static Agent[] agents;
  private static PriorityQueue<Agent> orderedAgents;
  private static Room room;
  private static Point2D roomBottomLeft, roomTopRight;

  @SuppressWarnings("ConstantConditions") // Several constant variables are explicitly named here just for readability
  public static void main(String[] args) {
    final double leftDoorWidth = 10.0;
    final double rightDoorWidth = 10.0;
    final boolean hasObstacle = true;

    ArrayList<XIntervalSeriesCollection> allPlots = new ArrayList<>();

    boolean hasOrient = false; boolean hasAttract = false;
    allPlots.add(runTrials(leftDoorWidth, rightDoorWidth, hasObstacle, "No communication", hasOrient, hasAttract));
    hasOrient = true;
    allPlots.add(runTrials(leftDoorWidth, rightDoorWidth, hasObstacle, "No speed", hasOrient, hasAttract));
    hasOrient = false; hasAttract = true;
    allPlots.add(runTrials(leftDoorWidth, rightDoorWidth, hasObstacle, "No bacterial", hasOrient, hasAttract));
    hasOrient = true;
    allPlots.add(runTrials(leftDoorWidth, rightDoorWidth, hasObstacle, "Full communication", hasOrient, hasAttract));


    (new Plotter("Test", simDuration)).plotMultiple(allPlots, plotFilePath);
  }

  private static XIntervalSeriesCollection runTrials(double leftDoorWidth,
                                                     double rightDoorWidth,
                                                     boolean hasObstacle,
                                                     String label,
                                                     boolean hasOrient,
                                                     boolean hasAttract) {

    System.out.print("Constructing room... ");
    initializeRoom(leftDoorWidth, rightDoorWidth, hasObstacle);

    ArrayList<XYSeries> resultsByTrial = new ArrayList<>(numTrials);
    for (int i = 0; i < numTrials; i++) {
      System.out.print("Running trial " + i + " of \"" + label + "\" condition: ");
      long startTime = System.nanoTime();

      resultsByTrial.add(runTrial(label, hasOrient, hasAttract));

      long endTime = System.nanoTime();
      System.out.println("Took " + ((endTime - startTime)/(Math.pow(10, 9))) + " seconds...");

      System.out.println("Computed " + room.numDestsComputed + " destinations.");

    }
    return Plotter.averageTrials(resultsByTrial, label);
  }

  /**
   * Runs a single self-contained simulation and returns results detailing the fraction of agents in the room over time
   * @param label name of this condition (only used for labeling plots)
   * @param hasOrient if true, the agents will use the orientation component of communication
   * @param hasAttract if true, the agents will use the attraction component of communication
   * @return XYSeries each X-value is a time between 0.0 and simDuration and each Y-value is a number in [0, 1]
   * indicating the fraction of agents remaining in the rooms; if all agents escaped the room, the XYSeries should have
   * numAgents items; else, the last item should be at time simDuration
   */
  private static XYSeries runTrial(String label,
                                   boolean hasOrient,
                                   boolean hasAttract) {

    System.out.print("Constructing agents... ");
    initializeAgents();

    // Run the simulation
    double t = 0.0;
    MatPlotter matPlotter;
    if (makeMovie) {
      matPlotter = new MatPlotter(frameRate, agents, room);
    }

    XYSeries fractionInRoomOverTime = new XYSeries(label); // legend label of item to plot

    System.out.print("Starting simulation... ");
    // Terminate the simulation when there are no agents left in the room or when the simulation duration has ended;
    // whichever comes first
    while (t < simDuration && !orderedAgents.isEmpty()) {

      // Get next agent to update from PriorityQueue
      Agent nextAgent = orderedAgents.poll();
      t = nextAgent.getNextUpdateTime();
//      if (t % 10.0 < 0.002) { System.out.println("The time is " + t + ". Computed " + room.numDestsComputed + " destinations."); } // Print a bit every 10 timesteps

      // Calculate forces, accelerate, move the agent, and update its priority
      nextAgent.update(t, room);

      // Track whether the agent left the room
      boolean isInRoom = agentIsInRoom(nextAgent);
      if (isInRoom) {

        // Add new social forces to the agent
        updateSocialForces(agents, nextAgent, hasOrient, hasAttract);

        // Reinsert the agent back into the priority queue
        orderedAgents.add(nextAgent);

      } else { // agent left the room;
//        System.out.println("An agent left at " + nextAgent.getPos());
        nextAgent.exit();
        fractionInRoomOverTime.add(t, getFracInRoom(agents));
      }

      if (makeMovie && t > matPlotter.getNextFrameTime()) {
        matPlotter.saveFrame(agents);
      }

    }

    System.out.println("Final Simulation time: " + t);

    // Add a final point to the plot at the last frame
    fractionInRoomOverTime.add(t, getFracInRoom(agents));

    // Export data necessary for movies as .mat file
    if (makeMovie) {
      matPlotter.writeToMAT(movieFilePath);
    }

    return fractionInRoomOverTime;

  }
  /**
   * Computes the fraction of agents remaining in the room; this is a minor convenience over getNumInRoom()
   * @param agents array of numAgents agents in the simulation
   * @return value of getNumInRoom() / numAgents
   */
  private static double getFracInRoom(Agent[] agents) {
    return getNumInRoom(agents) / numAgents;
  }

  /**
   * Counts the number of agents remaining in the room
   * @param agents array of numAgents agents in the simulation
   * @return the number of agents remaining in the room, according to agentIsInRoom();
   * the value is always an integer (up to round-off), despite the double type
   */
  private static double getNumInRoom(Agent[] agents) {
    double fracInRoom = 0.0;
    for (Agent agent : agents) {
      fracInRoom += agentIsInRoom(agent) ? 1.0 : 0.0;
    }
    return fracInRoom;
  }

  /**
   * Initialize numAgents Agents, stored in both an array and a PriorityQueue
   */
  private static void initializeAgents() {

    agents = new Agent[numAgents];

    // Store all the agents sorted by order in which they need to be next updated
    orderedAgents = new PriorityQueue<>(Math.max(numAgents, 1), new AgentComparator());

    // Initialize the agents
    for (int i = 0; i < numAgents; i++) {
      if (asymmetricInitialAgentDistribution && i > numAgents/4) {
        Point2D shiftedAgentMax = new Point2D(agentMax.x()/4, agentMax.y());
        agents[i] = new Agent(i, agentMin, shiftedAgentMax, frameRate, maxMove);
      } else {
        agents[i] = new Agent(i, agentMin, agentMax, frameRate, maxMove);
      }

      orderedAgents.add(agents[i]);

    }

  }

  private static void initializeRoom(double leftDoorWidth, double rightDoorWidth, boolean hasObstacle) {

    if (roomType == RoomType.GATES8) {
      buildGates8(hasObstacle);
    } else if (roomType == RoomType.BASIC) {
      buildBasic(leftDoorWidth, rightDoorWidth, hasObstacle);
    }
  }

  /**
   * Builds a room based on the floor plan of the 8th floor of the Gates Center for Computer Science at Carnegie Mellon
   * University. This room has 3 exits and requires agents to navigate complex non-convex obstacles.
   */
  private static void buildGates8(boolean hasObstacle) {
    roomBottomLeft = new Point2D(-1.0, -11.0);
    roomTopRight = new Point2D(60.0, 51.0);

    room = new Room(roomBottomLeft, roomTopRight, spatialResolution);

    // Construct out walls, going clockwise from top
    room.addWall(new LineSegment2D(0.0, 50.001, 60.0, 50.001)); // main top wall
    room.addWall(new LineSegment2D(60.001, 50.0, 50.001, 30.0)); // upper-right block right wall
    room.addWall(new LineSegment2D(50.0, 29.999, 38.0, 29.999)); // upper-right block bottom wall
    room.addWall(new LineSegment2D(38.001, 29.999, 32.001, 5.001)); // main corridor middle right wall

    // 8800 Stairwell
    double doorWidth8800 = 4.9;
    Point2D wallStart = new Point2D(55.001, -0.001);
    Point2D wallEnd = new Point2D(32.001, 5.001);
    Vector2D wallVec = new Vector2D(wallStart, wallEnd);
    wallVec = wallVec.times(1.0 - doorWidth8800/wallVec.norm()); // short wall by length doorWidth
    room.addWall(new LineSegment2D(wallStart, wallStart.plus(wallVec))); // bottom corridor top wall
    room.addExit(new Point2D(34.5, 5.0));

    // Remainder of outer structure
    room.addWall(new LineSegment2D(55.001, 0.001, 50.001, -10.0)); // bottom corridor right end
    room.addWall(new LineSegment2D(50.0, -10.001, 0.0, -0.001)); // main bottom wall
    room.addWall(new LineSegment2D(-0.001, 0.0, -0.001, 50.0)); // main left wall

    // Construct 8102 block
    room.addWall(new LineSegment2D(5.001, 24.999, 29.999, 24.999)); // top
    room.addWall(new LineSegment2D(29.999, 24.999, 24.999, 2.001)); // right
    room.addWall(new LineSegment2D(24.999, 2.001, 10.001, 5.001)); // bottom
    room.addWall(new LineSegment2D(5.001, 6.001, 5.001, 19.999)); // left

    // Construct 8118 (plus clear stair area) block
    room.addWall(new LineSegment2D(15.001, 44.999, 19.999, 44.999)); // top
    room.addWall(new LineSegment2D(19.999, 44.999, 14.999, 30.001)); // right
    room.addWall(new LineSegment2D(14.999, 30.001, 5.001, 30.001)); // bottom
    room.addWall(new LineSegment2D(5.001, 30.001, 5.001, 40.999)); // bottom left
    // 8100 Stairwell
    double doorWidth8100 = 4.8;
    wallStart = new Point2D(15.001, 44.999);
    wallEnd = new Point2D(5.001, 40.999);
    wallVec = new Vector2D(wallStart, wallEnd);
    wallVec = wallVec.times(1.0 - doorWidth8100/wallVec.norm()); // short wall by length doorWidth
    room.addWall(new LineSegment2D(wallStart, wallStart.plus(wallVec))); // top left
    room.addExit(new Point2D(7.5, 41.0));

    // Construct 8126 block
    room.addWall(new LineSegment2D(25.001, 44.999, 29.999, 44.999)); // top
    room.addWall(new LineSegment2D(34.999, 44.999, 29.999, 30.001)); // right
    room.addWall(new LineSegment2D(29.999, 30.001, 20.001, 30.001)); // bottom
    room.addWall(new LineSegment2D(20.001, 30.001, 25.001, 44.999)); // left

    // Construct 8228 block
    room.addWall(new LineSegment2D(40.001, 44.999, 49.999, 44.999)); // top
    room.addWall(new LineSegment2D(49.999, 44.999, 44.999, 35.001)); // right
    room.addWall(new LineSegment2D(44.999, 35.001, 35.001, 35.001)); // bottom
    // 8807 Stairwell
    double doorWidth8807 = 5.3;
    wallStart = new Point2D(40.001, 44.999);
    wallEnd = new Point2D(35.001, 35.001);
    wallVec = new Vector2D(wallStart, wallEnd);
    wallVec = wallVec.times(1.0 - doorWidth8807/wallVec.norm()); // short wall by length doorWidth
    room.addWall(new LineSegment2D(wallStart, wallStart.plus(wallVec))); // left
    room.addExit(new Point2D(38.0, 37.5));

    room.updateExitDistances();

    if (hasObstacle) {
      room.addWall(new LineSegment2D(0.0, 35.001, 5.0, 35.001));
    }

  }


  /**
   * Returns whether or not the input position is a valid starting position for an agent
   * (e.g., that the agent is inside the building, not already in an exit, etc.)
   * @param position proposed position at which to start the agent
   * @return true if and only
   */
  static boolean startingPositionIsValid(Point2D position) {

    // No invalid spaces in basic room
    if (roomType == RoomType.BASIC) { return true; }

    if (position.y() < 30.0 &&
        (new LineSegment2D(32.0, 5.0, 55.0, 0.0).isInside(position)) &&
        (new LineSegment2D(37.0, 29.0, 31.0, 5.0)).isInside(position)) {
      return false; // Behind 8807 stairwell
    }
    if (35.0 < position.y() && position.y() < 45.0 &&
        (new LineSegment2D(45.0, 35.0, 50.0, 45.0)).isInside(position) &&
        (new LineSegment2D(40, 45, 35, 35)).isInside(position)) {
      return false; // Behind 8807 stairwell
    }
    if (30.0 < position.y() && position.y() < 45.0 &&
        5.0 < position.x() && (new LineSegment2D(14.999, 30.001, 19.999, 44.999)).isInside(position)) {
      return false; // Behind 8100 stairwell
    }
    if ((new LineSegment2D(50.0, -9.8, 0.0, 0.2)).isInside(position)) {
      return false; // below main bottom wall
    }

    return true;
  }

  // TODO: Create an InnerRoom class, which is a collection of contiguous walls
  // An InnerRoom should:
  // 1) be constructable from a sequence of points (possibly with some indicator for doors
  // 2) be able to test whether a point is in its interior

  /**
   * Builds a simple rectangular room with two (not necessarily identical) exits (left and right).
   * @param leftDoorWidth size of the left door
   * @param rightDoorWidth size of the right door
   * @param hasObstacle if true, there will be an obstacle in front of the left door
   */
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
      double horizontalObstacleOffset = 0.4000001;
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

    if (agent.getExited()) {
      return false;
    }

    Point2D pos = agent.getPos();

    // Check outer room boundary
    return !(roomBottomLeft.x() > pos.x() || pos.x() > roomTopRight.x() ||
        roomBottomLeft.y() > pos.y() || pos.y() > roomTopRight.y())
        && !room.atExit(agent.getPos(), Math.max(spatialResolution, 2.2));

  }

  /**
   * Updates social forces due to movement of agent updatedAgent
   *
   * @param agents array of all agents, sorted by ID
   * @param updatedAgent ID of the agent that was just updated
   */
  private static void updateSocialForces(Agent[] agents, Agent updatedAgent, boolean hasOrient, boolean hasAttract) {

    for (Agent agent : agents) {

      // don't include self-interactions
      if (agent.getID() != updatedAgent.getID()) {

        // Updated agent pushes away from colliding agents
        if (Interactions.collision(agent, updatedAgent)) { Interactions.push(updatedAgent, agent); }

        // Updated agent tries to orient with nearby agents
        if (hasOrient) { Interactions.orient(agent, updatedAgent, room); }

        // Updated agent is attracted to more quickly moving agents
        if (hasAttract) { Interactions.speedAttract(agent, updatedAgent, room); }
      }
    }

  }

}
