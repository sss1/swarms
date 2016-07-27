package swarms;

import java.util.Random;
import math.geom2d.Vector2D;

class Agent {

  // Time-varying agent parameters
  private Vector2D pos;         // agent's coordinates
  private Vector2D vel;         // agent's velocity
  private Vector2D socialForce; // sum of social forces on agent (in Newtons)
  private Vector2D myForce;     // agent's own force (in Newtons)
  private double tLastUpdate, nextUpdateTime;

  // Constant agent-specific parameters
  private final double mass, radius, maxSpeed;

  private final int ID;

  Agent(int ID, Vector2D min, Vector2D max) {

    this.ID = ID;

    Random rand = new Random();

    // These are somewhat arbitrary ranges, for now
    mass = 65.0 + 10.0 * rand.nextDouble(); // 65-75
    radius = 0.4 + (0.2 * rand.nextDouble()); // 0.4-0.6
    maxSpeed = 1.0 + 3.0 * rand.nextDouble(); // 1-4

    // Uniformly random valid initial position within rectangle determined by
    // inputs
    pos = min.plus(max.minus(min).times(rand.nextDouble()));

    // Uniformly random valid initial velocity within circle of radius maxSpeed
    vel = Vector2D.createPolar(maxSpeed * rand.nextDouble(), 2.0 * Math.PI * rand.nextDouble());

  }

  public int getID() {
    return ID;
  }

  // Moves and accelerates the agent, updating its priority
  // It is crucial for synchrony that the Agent is removed and re-inserted
  // into the PriorityQueue whenever update() is called!
  public void update(double time, double maxMove) {
    updateIndividualForce();
    accelerate(time, maxMove);
    move(time);
    tLastUpdate = time;
  }

  // Agent position is needed to compute social forces and for plotting
  Vector2D getPos() {
    return pos;
  }

  // Adds a new social acting upon the agent (e.g., due to a new collision).
  public void addForce(Vector2D newForce) {
    socialForce = socialForce.plus(newForce);
  }

  // Returns the agent's radius, needed for plotting and checking collisions
  double getRadius() {
    return radius;
  }

  // Internal methods implementing motion mechanics
  private void move(double time) {
    pos = pos.plus(vel.times(time - tLastUpdate));
  }

  // It is crucial for synchrony that this is the only function allowed to
  // change nextUpdateTime!
  private void accelerate(double time, double maxMove) {
    vel = vel.plus(socialForce.plus(myForce).times((time - tLastUpdate)/mass));

    // Make sure speed is at most maxSpeed
    if (getSpeed() > maxSpeed) vel = vel.normalize().times(maxSpeed);
 
    nextUpdateTime = maxMove / getSpeed() + time;

  }


  // TODO: Design mechanics for individuals to choose their desired paths
  private void updateIndividualForce() {
    // For now, always move to the right
    myForce = new Vector2D(1.0, 0.0);
  }

  double getNextUpdateTime() {
    return nextUpdateTime;
  }

  /**
   * @return the current speed (i.e., norm of the velocity) of the agent
   */
  private double getSpeed() {
    return vel.norm();
  }

  public double getTLastUpdate() {
    return tLastUpdate;
  }

}
