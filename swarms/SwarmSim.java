package swarms;

import math.geom2d.Vector2D;
import java.util.PriorityQueue;

public class SwarmSim {

  // Simulation parameters
  private static final double simDuration = 100.0; // Time (in seconds) to simulate
  private static final int numAgents = 100;
  private static final Vector2D min = new Vector2D(0.0, 0.0); // Bottom left of rectangle in which agents start
  private static final Vector2D max = new Vector2D(50.0, 50.0); // Top right of rectangle in which agents start
  private static final double maxMove = 1.0; // Maximum distance an agent can move before needing to be updated
  private static final double frameRate = 1.0; // Rate at which to save frames for plotting

  public static void main(String[] args) {

    Agent[] agents = new Agent[numAgents];

    // Store all the agents sorted by order in which they need to be next updated
    PriorityQueue<Agent> orderedAgents = new PriorityQueue<>(numAgents, new AgentComparator());

    // Initialize the agents
    for (int i = 0; i < numAgents; i++) {
      agents[i] = new Agent(i, min, max);

      agents[i].setNextUpdateTime(maxMove / agents[i].getSpeed());
      orderedAgents.add(agents[i]);

    }

    // Start running the simulation
    double t = 0.0;
    double lastFrameSave = 0.0;
    Plotter plotter = new Plotter(agents, frameRate);
    while (t < simDuration) {

      // Get next agent to update from PriorityQueue
      Agent nextAgent = orderedAgents.poll();
      t = nextAgent.getNextUpdateTime();
      Vector2D oldPos = nextAgent.getPos(); // TEMP FOR PRINTING

      // Calculate forces, accelerate, move the agent, and update its priority
      nextAgent.update(t, maxMove);

      // Add new social forces to appropriate agents
      updateSocialForces(agents, nextAgent.getID());

      System.out.println("Moved agent " + nextAgent.getID() + " from " + oldPos + " to " + nextAgent.getPos() + " at time t = " + t + ".");

      // Reinsert the agent back into the priority queue
      orderedAgents.add(nextAgent);

      if (t > plotter.getNextFrameTime()) {
        plotter.saveFrame(agents);
      }

//      // For now, slow down sim for readability
//      try {
//        Thread.sleep(500);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
    }

    // TEMP FOR PRINTING: Calculate the final mean position of the agents
    Vector2D mean = new Vector2D(0.0, 0.0);
    for (Agent a : agents) mean = mean.plus(a.getPos());
    mean = mean.times(1.0/numAgents);
    System.out.println("The average agent is at position " + mean + ".");

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
