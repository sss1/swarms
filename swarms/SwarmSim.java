package swarms;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;
import math.geom2d.line.LineSegment2D;
import org.jfree.ui.RefineryUtilities;

import java.util.PriorityQueue;

public class SwarmSim {

  // Simulation parameters
  private static final double simDuration = 500.0; // Time (in seconds) to simulate
  private static final int numAgents = 500;
  private static final Point2D min = new Point2D(0.0, 0.0);         // Bottom left of room rectangle
  private static final Point2D max = new Point2D(50.0, 50.0);     // Top right of the room rectangle
  private static final Point2D agentMin = max.scale(0.25);    // Bottom left of rectangle in which agents start
  private static final Point2D agentMax = max.scale(0.75); // Top right of rectangle in which agents start
  private static final double maxMove = 0.3;    // Maximum distance an agent can move before needing to be updated
  private static final double frameRate = 0.5;  // Rate at which to save frames for plotting
  private static final double spatialResolution = 1;     // Resolution at which to model the room as a graph
  // private static final String outputPath = "/home/sss1/Desktop/projects/swarms/videos/out.mat";   // Output file from which to make MATLAB video
  private static final String outputPath = "/home/painkiller/Desktop/out.mat";   // Output file from which to make MATLAB video
  private static final double exitBufferDist = 100.0; // Distance beyond the exits that the room graph should cover

  // Simulation state variables
  private static Agent[] agents;
  private static PriorityQueue<Agent> orderedAgents;
  private static Room room;
  private static Point2D roomBottomLeft, roomTopRight;

  public static void main(String[] args) {

    initializeAgents();
    initializeRoom();

    // Run the simulation
    double t = 0.0;
    Plotter plotter = new Plotter("Fraction of agents in room over time");
    MatPlotter matPlotter = new MatPlotter(frameRate, agents, room);
    System.out.println("Starting simulation...");
    while (t < simDuration) {

      // Get next agent to update from PriorityQueue
      Agent nextAgent = orderedAgents.poll();
      boolean wasInRoom = agentIsInRoom(nextAgent);
      t = nextAgent.getNextUpdateTime();
      if (t % 10.0 < 0.001) { System.out.println("The time is " + t); } // Print a bit every 50 timesteps

      // Calculate forces, accelerate, move the agent, and update its priority
      nextAgent.update(t, room);

      // Add new social forces to appropriate agents
      updateSocialForces(agents, nextAgent);

      // Reinsert the agent back into the priority queue
      orderedAgents.add(nextAgent);

      // Report whether the agent left the room
      boolean isInRoom = agentIsInRoom(nextAgent);
      if (wasInRoom && !isInRoom) {
//        System.out.println("Agent " + nextAgent.getID() + " left the room at time " + t + ", at position " +
//            nextAgent.getPos() + ".");
        double fracInRoom = 0.0;
        for (Agent agent : agents) {
          fracInRoom += agentIsInRoom(agent) ? 1.0 : 0.0;
        }
        fracInRoom /= numAgents;
        plotter.addFractionInRoom(t, fracInRoom);
        if (fracInRoom < 1.0/numAgents) { break; } // no agents left in room; terminate simulation
      }

      if (t > matPlotter.getNextFrameTime()) {
        matPlotter.saveFrame(agents);
      }

    }

    // Plot the fraction of agents in the room over time
    plotter.plotFractionInRoomOverTime();
    plotter.pack();
    RefineryUtilities.centerFrameOnScreen(plotter);
    plotter.setVisible(true);
    matPlotter.writeToMAT(outputPath);

  }

  private static void initializeAgents() {

    agents = new Agent[numAgents];

    // Store all the agents sorted by order in which they need to be next updated
    orderedAgents = new PriorityQueue<>(numAgents, new AgentComparator());

    // Initialize the agents
    for (int i = 0; i < numAgents; i++) {
      agents[i] = new Agent(i, agentMin, agentMax, frameRate, maxMove);

      agents[i].setNextUpdateTime(Math.min(maxMove / agents[i].getSpeed(), frameRate));
      orderedAgents.add(agents[i]);

    }

  }

  private static void initializeRoom() {

    double p = spatialResolution /10; // small perturbation to prevent endpoint bugs

    Vector2D rightShift = new Vector2D(exitBufferDist, 0.0);
    room = new Room(min.minus(rightShift), max.plus(rightShift), spatialResolution);
    Point2D topLeft = new Point2D(min.x() - p, max.y() + p);
    Point2D bottomLeft = new Point2D(min.x() - p, min.y() - p);
    Point2D bottomRight = new Point2D(max.x() + p, min.y() - p);
    Point2D topRight = new Point2D(max.x() + p, max.y() + p);

    roomBottomLeft = bottomLeft;
    roomTopRight = topRight;

    Point2D rightDoorUpper = new Point2D(topRight.x(), topRight.y()/2 + 10);
    Point2D rightDoorLower = new Point2D(topRight.x(), topRight.y()/2 - 10);
    Point2D leftDoorUpper = new Point2D(bottomLeft.x(), topRight.y()/2 + 0.5);
    Point2D leftDoorLower = new Point2D(bottomLeft.x(), topRight.y()/2 - 0.5);

    room.addWall(new LineSegment2D(topRight, topLeft)); // top wall
    room.addWall(new LineSegment2D(topLeft, leftDoorUpper)); // upper left wall
    room.addWall(new LineSegment2D(leftDoorLower, bottomLeft)); // lower left wall
    room.addWall(new LineSegment2D(bottomLeft, bottomRight)); // bottom wall
    room.addWall(new LineSegment2D(bottomRight, rightDoorLower)); // lower right wall
    room.addWall(new LineSegment2D(rightDoorUpper, topRight)); // upper right wall

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

      // no self-interactions or interactions with agents outside the room
      if (agent.getID() == updatedAgent.getID() || !agentIsInRoom(agent)) { continue; }

      if (Interactions.collision(agent, updatedAgent)) {
        Interactions.push(updatedAgent, agent);
      }
      // Updated agent is attracted to more quickly moving agents
      // Interactions.speedAttract(agent, updatedAgent);
    }

  }

}
