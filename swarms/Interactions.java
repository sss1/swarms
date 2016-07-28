package swarms;

import math.geom2d.Point2D;

class Interactions {

  // Interaction parameters
  // TODO

  public Interactions() {
  }

  // Returns true if and only if agents a1 and a2 collide
  static boolean collision(Agent a1, Agent a2) {
    double dist = Point2D.distance(a1.getPos(), a2.getPos());
    double totRadius = a1.getRadius() + a2.getRadius();
    return  dist < totRadius;
  }

}
