package swarms;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class Interactions {

  // Interaction parameters
  // TODO

  public Interactions() {
  }

  // Returns true if and only if agents a1 and a2 collide
  public static boolean collision(Agent a1, Agent a2) {
    double xDist = a1.getXPos() - a2.getXPos();
    double yDist = a1.getYPos() - a2.getYPos();
    double totRadius = a1.getRadius() + a2.getRadius();
    return  totRadius * totRadius < xDist * xDist + yDist * yDist;
  }

}
