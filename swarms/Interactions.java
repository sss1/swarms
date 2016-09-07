package swarms;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;

class Interactions {

  // Interaction parameters:
  //    Pushing parameters:
  private static final double compressiveTolerance = 1.0; // Increase this to reduce how much repulsion increases with proximity
  private static final double repulsionStrength = 10.0; // Scalar weight of interpersonal repulsion term
  private static final double frictionStrength = 1.0; // Scalar weight of interpersonal friction term

  //    Orientation parameters
  private static final double orientRange = 3.0; // Maximum distance between agents at which orientation applies
  private static final double orientWeight = 0.1; // Multiplicative weight for orientation term

  //    Speed attraction parameters:
  private static final double speedPenalty = 0.3; // Minimum difference in speeds for speed attraction to apply
  private static final double speedAttractWeight = 5.0; // Multiplicative weight for the speedAttraction term

  // Returns true if and only if agents a1 and a2 collide
  static boolean collision(Agent a1, Agent a2) {
    double dist = Point2D.distance(a1.getPos(), a2.getPos());
    double totRadius = a1.getRadius() + a2.getRadius();
    return dist < totRadius;
  }

  /**
   * When two agents come into contact, one (the pusher) may push the other (the pushee).
   * This interaction has two components:
   *  1) The pusher can push against the pushee (repulsion)
   *  2) If an agent is moving, they can drag the other agent with them (friction)
   *
   *  Note that, due to Newton's third law of motion, equal but opposite forces are exerted on the pusher!
   *
   * @param pusher agent doing the pushing
   * @param pushee agent being pushed by the pusher
   */
  static void push(Agent pusher, Agent pushee) {

    double distance = Point2D.distance(pusher.getPos(), pushee.getPos()); // distance between centers of agents
    double compression = pusher.getRadius() + pushee.getRadius() - distance; // distance that agents are being compressed

    // Compute normal and tangent vectors of agents' interaction
    Vector2D normalVec = new Vector2D(pusher.getPos(), pushee.getPos()).normalize();
    Vector2D tangentVec = new Vector2D(normalVec.y(), - normalVec.x()); // orthogonal to normalVec

    // Compute repulsion component
    double repulsionMagnitude = repulsionStrength * Math.exp(compression/compressiveTolerance);
    Vector2D repulsionForce = normalVec.times(repulsionMagnitude);

    // Compute friction component
    Vector2D velDiff = pusher.getVel().minus(pushee.getVel());
    double frictionMagnitude = frictionStrength * compression * Vector2D.dot(velDiff, tangentVec);
    Vector2D frictionForce = tangentVec.times(frictionMagnitude);

    // Compute total force and apply it to each agent
    Vector2D totalForce = repulsionForce.plus(frictionForce);
    pushee.addForce(totalForce);
    pusher.addForce(totalForce.opposite());

  }

  static void orient(Agent orientor, Agent orientee) {
    if (Point2D.distance(orientee.getPos(), orientee.getPos()) < orientRange) {
      orientee.addForce(orientor.getVel().times(orientWeight));
    }
  }

  static void speedAttract(Agent attractor, Agent attractee) {
    double magnitude = attractor.getSpeed() - attractee.getSpeed() - speedPenalty;
    if (magnitude > Double.MIN_VALUE) {
      Vector2D direction = (new Vector2D(attractee.getPos(), attractor.getPos())).normalize();
      attractee.addForce(direction.times(speedAttractWeight * Math.max(magnitude, 0.0)));
    }
  }

}
