package swarms;

import java.util.Comparator;
import java.util.Random;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;

class Agent {

  // Time-varying agent parameters
  private Point2D pos;           // agent's coordinates
  private Vector2D vel;           // agent's velocity (in m/s)
  private Vector2D socialForce;   // sum of social forces on agent (in Newtons)
  private Vector2D myForce;       // agent's own force (in Newtons)
  private double tLastUpdate;     // Simulation time at which agent's update() function was last called
  private double nextUpdateTime;  // Simulation time at which agent's update() function next needs to be called

  // Constant parameters
  private final double forceDecayConstant = 10.0; // Rate at which earlier social forces decay

  // Constant agent-specific parameters
  private final double mass, radius, maxSpeed;

  private final int ID; // Index ID of this agent (0 <= ID < numAgents)

  Agent(int ID, Point2D min, Point2D max) {

    this.ID = ID;

    Random rand = new Random();

    // These are somewhat arbitrary ranges, for now
    mass = 65.0 + 10.0 * rand.nextDouble(); // 65-75
    radius = 0.25 + (0.1 * rand.nextDouble()); // 0.25-0.35
    maxSpeed = 1.0 + 3.0 * rand.nextDouble(); // 1-4

    // Uniformly random valid initial position within rectangle determined by
    // inputs
    pos = new Point2D(min.x() + (max.x() - min.x()) * rand.nextDouble(), min.y() + (max.y() - min.y()) * rand.nextDouble());

    // Uniformly random valid initial velocity within circle of radius maxSpeed
    vel = Vector2D.createPolar((maxSpeed / 5.0) * rand.nextDouble(), 2.0 * Math.PI * rand.nextDouble());

    socialForce = new Vector2D(0.0, 0.0);
    myForce = new Vector2D(0.0, 0.0);
    tLastUpdate = 0.0;

  }

  int getID() {
    return ID;
  }

  // Moves and accelerates the agent, updating its priority
  // It is crucial for synchrony that the Agent is removed and re-inserted
  // into the PriorityQueue whenever update() is called!
  void update(double t, double maxMove) {
    updateIndividualForce();
    accelerate(t);
    move(t);
    tLastUpdate = t;
    setNextUpdateTime(t + (maxMove / getSpeed()));
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

  // Update the position of the agent, based on their velocity and the time since their last update
  // TODO: Check for walls! If an agent's movement goes through a wall, truncate it!
  private void move(double time) {
    pos = pos.plus(vel.times(time - tLastUpdate));
  }

  // It is crucial for synchrony that this is the only function allowed to
  // change nextUpdateTime!
  private void accelerate(double time) {
    double timeSinceUpdate = time - tLastUpdate;
    Vector2D acc = myForce.plus(socialForce).times(1/mass);
    vel = vel.plus(acc.times(timeSinceUpdate));

    // Make sure speed is at most maxSpeed
    if (getSpeed() > maxSpeed) vel = vel.normalize().times(maxSpeed);

    // Let earlier social forces decay over time, once they have been incorporated into the velocity
    socialForce = socialForce.times(Math.exp(-forceDecayConstant*timeSinceUpdate));

  }

  // TODO: Design mechanics for individuals to choose their desired paths
  private void updateIndividualForce() {
    // For now, always move to the right
    myForce = new Vector2D(-7*pos.x()/Math.abs(pos.x()), -7*pos.y()/Math.abs(pos.y()));
  }

  void setNextUpdateTime(double nextUpdateTime) {
    this.nextUpdateTime = nextUpdateTime;
  }

  double getNextUpdateTime() {
    return nextUpdateTime;
  }

  /**
   * @return the current speed (i.e., norm of the velocity) of the agent
   */
  double getSpeed() {
    return vel.norm();
  }

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