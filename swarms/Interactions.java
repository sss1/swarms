package swarms;

import java.util.ArrayList;
import java.util.PriorityQueue;

class Interactions {

  // Interaction parameters
  // TODO

  public Interactions() {
  }

  // Returns true if and only if agents a1 and a2 collide
  static boolean collision(Agent a1, Agent a2) {
    double dist = a1.getPos().minus(a2.getPos()).norm();
    double totRadius = a1.getRadius() + a2.getRadius();
    return  dist < totRadius;
  }

}
