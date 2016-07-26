package swarms;

import java.util.Comparator;

public class AgentComparator implements Comparator<Agent> {

  @Override
  public int compare(Agent a1, Agent a2) {
    return (a1.getNextUpdateTime() < a2.getNextUpdateTime()) ? -1 : 1;
  }

}
