package swarms;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;
import math.geom2d.line.LineSegment2D;

import java.util.Collection;
import java.util.Comparator;
import java.util.Random;

class Agent {

  // Time-varying agent parameters
  private Point2D pos;            // agent's coordinates
  private Vector2D vel;           // agent's velocity (in m/s)
  private Vector2D socialForce;   // sum of social forces on agent (in Newtons)
  private Vector2D myForce;       // agent's own force (in Newtons)
  private double tLastUpdate;     // Simulation time at which agent's update() function was last called
  private double nextUpdateTime;  // Simulation time at which agent's update() function next needs to be called
  private boolean exited = false; // true if and only if the agent has left the room

  // Constant parameters across all agents
  private static final double myForceWeight = 100.0;
  private static final double noiseFactor = 0.5;

  // Constant agent-specific parameters
  private final double mass, radius, maxSpeed;

  // Simulation settings relevant for agents
  private final double frameRate, maxMove;

  private final int ID; // Index ID of this agent (0 <= ID < numAgents)

  Agent(int ID, Point2D min, Point2D max, double frameRate, double maxMove) {

    this.frameRate = frameRate;
    this.maxMove = maxMove;
    this.ID = ID;

    Random rand = new Random();

    // These are somewhat arbitrary ranges
    mass = (65.0 + 10.0 * rand.nextDouble())/5.0; // 13-15
    radius = 0.4 + (0.1 * rand.nextDouble()); // 0.4-0.5
    maxSpeed = 1.0 + 2.0 * rand.nextDouble(); // 1-3

    // Uniformly random valid initial position; continue generating positions until one is valid
    pos = new Point2D(min.x() + (max.x() - min.x()) * rand.nextDouble(), min.y() + (max.y() - min.y()) * rand.nextDouble());
    while (!SwarmSim.startingPositionIsValid(pos)) {
      pos = new Point2D(min.x() + (max.x() - min.x()) * rand.nextDouble(), min.y() + (max.y() - min.y()) * rand.nextDouble());
    }

    // Uniformly random valid initial velocity within circle of radius maxSpeed
    vel = Vector2D.createPolar(maxSpeed * rand.nextDouble() / 10.0, 2.0 * Math.PI * rand.nextDouble());

    socialForce = new Vector2D(0.0, 0.0);
    myForce = new Vector2D(0.0, 0.0);
    tLastUpdate = 0.0;
    setNextUpdateTime(Math.min(maxMove / getSpeed(), frameRate * rand.nextDouble()));
  }

  int getID() {
    return ID;
  }

  void exit() { exited = true; }

  boolean getExited() { return exited; }

  // Moves and accelerates the agent, updating its priority
  // It is crucial for synchrony that the Agent is removed from (and, if still
  // in the room, re-inserted into) the PriorityQueue whenever update() is called!
  void update(double t, Room room) {
    updateIndividualForce(room);
    accelerate(t);
    move(t, room.getWalls());
    tLastUpdate = t;
    setNextUpdateTime(t + Math.min(maxMove / getSpeed(), frameRate));
  }

  // Agent position is needed to compute social forces and for plotting
  Point2D getPos() {
    return pos;
  }

  // Adds a new social acting upon the agent (e.g., due to a new collision).
  void addForce(Vector2D newForce) {
    socialForce = socialForce.plus(newForce);
  }

  // Returns the agent's radius, needed for plotting and checking collisions
  double getRadius() {
    return radius;
  }

  /**
   * @param path line segment from current position to hypothetical next position
   * @param walls a collection of all walls in the room
   * @return first wall with which the agent's current movement would collide, or null if there is no such wall
   */
  private LineSegment2D getCollidingWall(LineSegment2D path, Collection<LineSegment2D> walls) {
    LineSegment2D collidingWall = null;
    double distanceToCollidingWall = Double.POSITIVE_INFINITY;
    for (LineSegment2D wall : walls) {
      if (LineSegment2D.intersects(wall, path) &&
          Point2D.distance(pos, LineSegment2D.getIntersection(wall, path)) < distanceToCollidingWall) {
        distanceToCollidingWall = Point2D.distance(pos, LineSegment2D.getIntersection(wall, path));
        collidingWall = wall;
      }
    }
    return collidingWall;
  }

  /**
   * Update the position of the agent, based on their velocity and the time since their last update,
   * truncating their movement and velocity based on any walls in the way
   * @param time the time at which the move occurs
   * @param walls a collection of all walls in the room
   */
  private void move(double time, Collection<LineSegment2D> walls) {

    // Figure out if the agent will collide with a wall this move
    Point2D nextPos = pos.plus(vel.times(time - tLastUpdate)); // hypothetical next position, if there were no walls
    nextPos = nextPos.plus(vel.normalize().times(radius)); // extend path to account for positive radius
    LineSegment2D path = new LineSegment2D(pos, nextPos);
    LineSegment2D collidingWall = getCollidingWall(path, walls);

    // Move the agent
    Vector2D move;
    if (collidingWall == null) { // no collision with wall; move normally
      move = vel.times(time - tLastUpdate);
    } else { // truncate movement and velocity due to collision with wall
      move = new Vector2D(pos, LineSegment2D.getIntersection(collidingWall, path));
      move = move.times((move.norm() - radius)/move.norm()); // shorten move to account for positive radius
      Vector2D wallAsVector = new Vector2D(collidingWall.firstPoint(), collidingWall.lastPoint()).normalize();
      // replace the velocity with its part parallel to the wall; i.e., kill its normal part
      // redirect momentum to be parallel to the colliding wall
      vel = wallAsVector.times(Math.signum(Vector2D.dot(vel, wallAsVector))).normalize().times(vel.norm() / 6.0);
    }
    pos = pos.plus(move);
  }

  // It is crucial for synchrony that this is the only function allowed to
  // change nextUpdateTime!
  private void accelerate(double time) {
    double timeSinceUpdate = time - tLastUpdate;
    Vector2D acc = myForce.times(myForceWeight).plus(socialForce).times(1/mass); // a = F/m
    vel = vel.plus(acc.times(timeSinceUpdate)); // dv = a*dt

    // Make sure speed is at most maxSpeed
    if (getSpeed() > maxSpeed) vel = vel.normalize().times(maxSpeed);

    // Remove earlier social forces once they have been incorporated into the velocity
    socialForce = new Vector2D(0.0, 0.0);

  }

  // For now, we should label certain cells as exits, and have agents push towards those
  private void updateIndividualForce(Room room) {
    Random rand = new Random();
    double xNoise = rand.nextGaussian();
    double yNoise = rand.nextGaussian();
    Vector2D gradient = room.getGradient(pos);
    assert !Double.isNaN(gradient.norm());
    myForce = gradient.plus((new Vector2D(xNoise, yNoise)).times(noiseFactor * gradient.norm()));
  }

  private void setNextUpdateTime(double nextUpdateTime) {
    this.nextUpdateTime = nextUpdateTime;
  }

  double getNextUpdateTime() {
    return nextUpdateTime;
  }

  /**
   * @return the current speed (i.e., norm of the velocity) of the agent, or the agent's maximum possible speed if they
   * have left the room
   */
  double getSpeed() { return exited ? maxSpeed : vel.norm(); }

  /**
   * @return the current velocity of the agent
   */
  Vector2D getVel() {
    return vel;
  }

}

class AgentComparator implements Comparator<Agent> {

  @Override
  public int compare(Agent a1, Agent a2) {
    return (a1.getNextUpdateTime() < a2.getNextUpdateTime()) ? -1 : 1;
  }

}