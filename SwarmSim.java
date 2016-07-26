package swarms;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class SwarmSim {

  // Simulation parameters
  static int numAgents = 100;
  static double xMin = 0.0;
  static double xMax = 50.0;
  static double yMin = 0.0;
  static double yMax = 50.0;
  static double maxMove = 1.0;

  public static void main(String[] args) {

    Agent[] agents = new Agent[numAgents];

    // Store all the agents sorted by order in which they need to be updated
    PriorityQueue<Agent> orderedAgents
                          = new PriorityQueue<Agent>(numAgents, new AgentComparator());

    for (int i = 0; i < numAgents; i++) {
      agents[i] = new Agent(i, xMin, xMax, yMin, yMax);

      orderedAgents.add(agents[i]);

      // try {
      //   Thread.sleep(1000);
      // } catch (InterruptedException e) {
      //   e.printStackTrace();
      // }
    }

  }

  // Updates agent's social forces due to movement of agent updatedAgent
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
