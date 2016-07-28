package swarms;

import math.geom2d.Point2D;
import math.geom2d.line.LineSegment2D;

import java.util.PriorityQueue;

public class SwarmSim {

  // Simulation parameters
  private static final double simDuration = 100.0; // Time (in seconds) to simulate
  private static final int numAgents = 100;
  private static final Point2D min = new Point2D(0.0, 0.0); // Bottom left of rectangle in which agents start
  private static final Point2D max = new Point2D(50.0, 50.0); // Top right of rectangle in which agents start
  private static final double maxMove = 1.0;    // Maximum distance an agent can move before needing to be updated
  private static final double frameRate = 1.0;  // Rate at which to save frames for plotting
  private static final double fineness = 0.5;   // Resolution at which to model the room as a graph

  // Simulation state variables
  private static Agent[] agents;
  private static PriorityQueue<Agent> orderedAgents;
  private static Room room;

  public static void main(String[] args) {

    initializeAgents();
    initializeRoom();

    // Start running the simulation
    double t = 0.0;
    Plotter plotter = new Plotter(frameRate, agents, room);
    while (t < simDuration) {

      // Get next agent to update from PriorityQueue
      Agent nextAgent = orderedAgents.poll();
      t = nextAgent.getNextUpdateTime();

      // Calculate forces, accelerate, move the agent, and update its priority
      nextAgent.update(t, maxMove);

      // Add new social forces to appropriate agents
      updateSocialForces(agents, nextAgent.getID());

      // Reinsert the agent back into the priority queue
      orderedAgents.add(nextAgent);

      if (t > plotter.getNextFrameTime()) {
        plotter.saveFrame(agents, room);
      }

    }

  }

  private static void initializeAgents() {

    agents = new Agent[numAgents];

    // Store all the agents sorted by order in which they need to be next updated
    orderedAgents = new PriorityQueue<>(numAgents, new AgentComparator());

    // Initialize the agents
    for (int i = 0; i < numAgents; i++) {
      agents[i] = new Agent(i, min, max);

      agents[i].setNextUpdateTime(maxMove / agents[i].getSpeed());
      orderedAgents.add(agents[i]);

    }

  }

  private static void initializeRoom() {

    room = new Room(min, max, fineness);

    // TODO: Add some walls and other more interesting structure
    room.addWall(new LineSegment2D(25.0, 10.0, 25.0, 40.0));

  }

  /**
   * Updates agent's social forces due to movement of agent updatedAgent
   *
   * @param agents array of all agents, sorted by ID
   * @param updatedAgent ID of the agent that was just updated
   */
  private static void updateSocialForces(Agent[] agents, int updatedAgent) {

    for (int i = 0; i < agents.length; i++) {

      if (i == updatedAgent) { continue; } // no self-interactions

      if (Interactions.collision(agents[i], agents[updatedAgent])) {
          // TODO: Update social forces
          System.out.println("Agents " + i + " and " + updatedAgent + " collided!");
      }
    }

  }

}
