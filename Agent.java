package swarms;

import java.util.Random;

public class Agent {

  // Time-varying agent parameters
  private double xPos, yPos;                  // agent's coordinates
  private double xVel, yVel;                  // agent's velocity
  private double socialXForce, socialYForce;  // sum of social forces on agent (in Newtons)
  private double myXForce, myYForce;          // agent's own force (in Newtons)
  private double tLastUpdate, nextUpdateTime;

  // Constant agent-specific parameters
  private final double mass, radius, maxSpeed;

  private final int ID;

  public Agent(int ID, double xMin, double xMax, double yMin, double yMax) {

    this.ID = ID;

    Random rand = new Random();

    // These are somewhat arbitrary ranges, for now
    mass = 65.0 + 10.0 * rand.nextDouble(); // 65-75
    radius = 0.4 + 0.2 * rand.nextDouble(); // 0.4-0.6
    maxSpeed = 1.0 + 3.0 * rand.nextDouble(); // 1-4

    // Uniformly random valid initial position within rectangle determined by
    // inputs
    xPos = xMin + (xMax - xMin) * rand.nextDouble();
    yPos = yMin + (yMax - yMin) * rand.nextDouble();

    // Uniformly random valid initial velocity within circle of radius maxSpeed
    double theta = 2.0 * rand.nextDouble();
    double speed = maxSpeed * rand.nextDouble();
    xVel = speed * Math.cos(theta);
    yVel = speed * Math.sin(theta);

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
  public double getXPos() {
    return xPos;
  }
  public double getYPos() {
    return yPos;
  }

  // Adds a new social acting upon the agent (e.g., due to a new collision).
  public void addForce(double newXForce, double newYForce) {
    socialXForce = socialXForce + newXForce;
    socialYForce = socialYForce + newYForce;
  }

  // Returns the agent's radius, needed for plotting and checking collisions
  public double getRadius() {
    return radius;
  }

  // Internal methods implementing motion mechanics
  private void move(double time) {
    xPos = xPos + xVel * (time - tLastUpdate);
    yPos = yPos + yVel * (time - tLastUpdate);
  }

  // It is crucial for synchrony that this is the only function allowed to
  // change nextUpdateTime!
  private void accelerate(double time, double maxMove) {
    xVel = xVel + ((socialXForce + myXForce) / mass) * (time - tLastUpdate);
    yVel = yVel + ((socialYForce + myYForce) / mass) * (time - tLastUpdate);

    // Make sure speed is at most maxSpeed
    double tmpSpeed = getSpeed();
    if (tmpSpeed > maxSpeed) {
      xVel = xVel * maxSpeed / tmpSpeed;
      yVel = yVel * maxSpeed / tmpSpeed;
    }
 
    nextUpdateTime = maxMove / getSpeed() + time;

  }


  // TODO: Desing mechanics for individuals to choose their desired paths
  private void updateIndividualForce() {
    // For now, always move to the right
    myXForce = 1;
    myYForce = 0;
  }

  public double getNextUpdateTime() {
    return nextUpdateTime;
  }







  public double getSpeed() {
    return Math.sqrt(xVel*xVel + yVel*yVel);
  }

  public double getTLastUpdate() {
    return tLastUpdate;
  }

}
