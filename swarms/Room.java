package swarms;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;
import math.geom2d.line.LineSegment2D;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A room is modeled as a grid-shaped graph. Walls can be simulated by removing edges.
 * This allows distances and directions to be computed with respect to very general
 * non-Euclidean topologies, such as non-convex spaces.
 * Created by sss1 on 7/28/16.
 */
class Room {

  private Graph<Cell, CellEdge> roomGraph;
  private ArrayList<LineSegment2D> walls;
  private ArrayList<Cell> exits;
  private ArrayList<Point2D> exactExitPositions; // the Cell versions of the exits loose some precision
  private final double fineness;
  private final Point2D min;
  private final Cell[][] grid;

  int numDestsComputed = 0; // TODO: This is a temporary variable for printing; remove it.


  Room(Point2D min, Point2D max, double fineness) {

    this.min = min;
    this.fineness = fineness;

    int nCellsX = 1 + ((int) ((max.x() - min.x()) / fineness));
    int nCellsY = 1 + ((int) ((max.y() - min.y()) / fineness));

    System.out.println("Total number of nodes: " + (nCellsX * nCellsY));

    roomGraph = new SimpleGraph<>(CellEdge.class);

    grid = new Cell[nCellsX][nCellsY]; // Temporary organization for easily adding grid edges

    for (int i = 0; i < nCellsX; i++) {
      for (int j = 0; j < nCellsY; j++) {

        double x = min.x() + i * fineness;
        double y = min.y() + j * fineness;
        Cell here = new Cell(x, y);
        grid[i][j] = here;
        roomGraph.addVertex(grid[i][j]);
        if (i > 0 && j > 0) {
          Cell topLeft = grid[i - 1][j - 1];
          roomGraph.addEdge(here, topLeft, new CellEdge(here, topLeft)); // add edge to top-left
        }
        if (i > 0) {
          Cell left = grid[i - 1][j];
          roomGraph.addEdge(here, left, new CellEdge(here, left)); // add edge to left
        }
        if (i > 0 && j < nCellsY - 2) {
          Cell bottomLeft = grid[i - 1][j + 1];
          roomGraph.addEdge(here, bottomLeft, new CellEdge(here, bottomLeft)); // add edge to bottom-left
        }
        if (j > 0) {
          Cell top = grid[i][j - 1];
          roomGraph.addEdge(here, top, new CellEdge(here, top)); // add edge to top
        }
      }


    }

    exits = new ArrayList<>();
    exactExitPositions = new ArrayList<>();
    walls = new ArrayList<>();

  }

  void addExit(Point2D exitLocation) {
    Cell cell;
    try {
      cell = getCellFromPosition(exitLocation);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Tried to place an exit outside of the graph.");
    }
    exactExitPositions.add(exitLocation);
    exits.add(cell);
  }

  /**
   * Simulates a wall by removing any edges that cross the line segment between the two input points
   */
  void addWall(LineSegment2D wall) {

    walls.add(wall);

    Set<CellEdge> toRemove = roomGraph.edgeSet()
                                      .stream()
                                      .filter(e -> LineSegment2D.intersects(wall, e.asLineSegment()))
                                      .collect(Collectors.toSet());

    roomGraph.removeAllEdges(toRemove);

  }
  ArrayList<LineSegment2D> getWalls() {
    return walls;
  }


  /**
   * Encodes the graph of the room as a 2D array of size numEdges X 4, for saving in a .mat file.
   * Each row is an edge, and the columns correspond to (x1, y1, x2, y2).
   * @return a 2D array encoding of the walls in the room
   */
  double[][] getAsArray() {
    Set<CellEdge> edgeSet = roomGraph.edgeSet();
    double[][] asArray = new double[edgeSet.size()][4];
    int nextIdx = 0;
    for (CellEdge e : edgeSet ) {
      asArray[nextIdx] = e.getAsArray();
      nextIdx++;
    }
    return asArray;
  }

  Vector2D getGradient(Point2D position) { return getCellFromPosition(position).getGradient(); }

  /**
   * Returns a vector pointing (according to the graph) from the source to the sink
   * @param source point from which the gradient should start
   * @param sink point where the gradient should lead
   * @return graph gradient from source to sink
   */
  Vector2D getGradientBetween(Point2D source, Point2D sink) {
    return getCellFromPosition(source).getGradientToCell(getCellFromPosition(sink));
  }


  /**
   * Returns the graph-based distance from the source to the sink
   * @param source point from which the distance should be measured
   * @param sink point to which the distance should be measured
   * @return graph distance from source to sink
   */
  double getDistanceBetween(Point2D source, Point2D sink) {
    return getCellFromPosition(source).getDistToCell(getCellFromPosition(sink));
  }

  /**
   * Labels each cell with its distance to the nearest exit
   */
  void updateExitDistances() {
    // For each exit, do a BFS. Upon traversing a vertex, set its distance to the minimum distance through each of its
    // neighboring vertices.
    for (Cell exit : exits) {
      BreadthFirstIterator<Cell, CellEdge>  iterator = new BreadthFirstIterator<>(roomGraph, exit);
      iterator.addTraversalListener(new ExitSearchListener());
      while (iterator.hasNext()) { iterator.next(); }
    }
//    logarithmizeDistance();
    rootDistance();
  }

  private void computeDistancesToCell(Cell targetCell) {
    // Compute distance from all cells to the target cell by running a
    // BFS originating from the target cell
    BreadthFirstIterator<Cell, CellEdge> iterator = new BreadthFirstIterator<>(roomGraph, targetCell);
    iterator.addTraversalListener(new NodeSearchListener(targetCell));
    while (iterator.hasNext()) { iterator.next(); }
    numDestsComputed++;
  }

  private void rootDistance() {
    for (Cell cell : roomGraph.vertexSet()) {
      cell.setDistToExit(10*Math.pow(cell.getDistToExit(), 0.75));
    }
  }

  private void logarithmizeDistance() {
    for (Cell cell : roomGraph.vertexSet()) {
      cell.setDistToExit((cell.getDistToExit() > Double.MIN_VALUE) ? 200.0 * Math.log(cell.getDistToExit()) : 0.0);
    }
  }

  private Cell getCellFromPosition(Point2D position) {
    int idxX = (int) ((position.x() - min.x()) / fineness);
    if (idxX < 0 || idxX >= grid.length) {
      throw new IllegalArgumentException("Tried to get cell outside of map at coordinates: " + position);
    }
    int idxY = (int) ((position.y() - min.y()) / fineness);
    if (idxY < 0 || idxY >= grid[0].length) {
      throw new IllegalArgumentException("Tried to get cell outside of map at coordinates: " + position);
    }
    return grid[idxX][idxY];
  }

  boolean atExit(Point2D position, double tolerance) {
    for (Point2D exitPosition : exactExitPositions) {
      if (Point2D.distance(position, exitPosition) < tolerance) { return true; }
    }
    return false;
  }

  /**
   * Encodes the walls in the room as a 2D array of size numWalls X 4, for saving in a .mat file.
   * Each row is a wall, and the columns correspond to (x1, y1, x2, y2).
   * @return a 2D array encoding of the walls in the room
   */
  double[][] getWallsAsArray() {
    double[][] wallsAsArray = new double[walls.size()][4];
    for (int i = 0; i < walls.size(); i++) {
      wallsAsArray[i] = new double[]{   walls.get(i).firstPoint().x(),
                                        walls.get(i).firstPoint().y(),
                                        walls.get(i).lastPoint().x(),
                                        walls.get(i).lastPoint().y() };
    }
    return wallsAsArray;
  }

  /**
   * A single ``grid cell'' of the room
   */
  private class Cell {

    private final Point2D coordinates; // location of the cell
    private double distToExit;
    private Vector2D exitGradient;
    private HashMap<Cell, Double> distMap; // Distance to each other cell

    Cell(double x, double y) {
      coordinates = new Point2D(x, y);
      distToExit = Double.POSITIVE_INFINITY;
      exitGradient = null;
      distMap = new HashMap<>();
    }

    /**
     * Two non-null Cells are equal if they occupy the same coordinate (i.e., if
     * their distance is strictly less than fineness).
     * @param obj Cell to which to test equality with this
     * @return true if this and obj are equal
     */
    @Override
    public boolean equals(Object obj) {
      return obj != null &&
          getClass() == obj.getClass() &&
          euclideanDistFrom((Cell) obj) < fineness / 10.0;
    }

    @Override
    public String toString() {
      return coordinates.toString();
    }

    double euclideanDistFrom(Cell targetCell) {
      return Point2D.distance(this.getCoordinates(), targetCell.getCoordinates());
    }

    Point2D getCoordinates() {
      return coordinates;
    }

    void setDistToCell(Cell targetCell, double distance) {
      distMap.put(targetCell, distance);
//      System.out.println("Set distance of " + distance + " from cell at " + this.getCoordinates() + " to cell at " + targetCell.getCoordinates());
    }

    Vector2D getGradientToCell(Cell targetCell) {
      Vector2D gradient = new Vector2D(0.0, 0.0);

      // Compute all distances to this cell, if we haven't already done so
      if (Double.isInfinite(getDistToCell(targetCell))) { computeDistancesToCell(targetCell); }

      Collection<Cell> neighbors = Graphs.neighborListOf(roomGraph, this);
      // Note that, since the BFS starts from the target cell and traverses the whole
      // graph, at most one BFS call can occur per node; this could still be horribly slow, however
      for (Cell neighbor : neighbors) {
        // diffDistance is positive if the neighbor is closer to the exit than the current cell, and negative otherwise
        double diffDistance = getDistToCell(targetCell) - neighbor.getDistToCell(targetCell);
        gradient = gradient.plus((new Vector2D(coordinates, neighbor.getCoordinates())).times(diffDistance));
      }
      return gradient.times(1.0/neighbors.size()); // Divide by number of neighbors, to average
    }

    double getDistToCell(Cell targetCell) {
      return (distMap.containsKey(targetCell)) ? distMap.get(targetCell) : Double.POSITIVE_INFINITY;
    }

    double getDistToExit() {
      return distToExit;
    }

    void setDistToExit(double distToExit) {
      this.distToExit = distToExit;
    }

    Vector2D getGradient() {
      if (exitGradient == null) { setGradient(); }
      return exitGradient;
    }

    void setGradient() {
      if (Double.isInfinite(coordinates.x())) { // Gradient outside graph is zero
        this.exitGradient = new Vector2D(0.0, 0.0);
        return;
      }

      exitGradient = new Vector2D(0.0, 0.0);

      Collection<Cell> neighbors = Graphs.neighborListOf(roomGraph, this);
      for (Cell neighbor : neighbors) {
        // diffDistance is positive if the neighbor is closer to the exit than the current cell, and negative otherwise
        double diffDistance = distToExit - neighbor.getDistToExit();
        exitGradient = exitGradient.plus((new Vector2D(coordinates, neighbor.getCoordinates())).times(diffDistance));
      }
      exitGradient = exitGradient.times(1.0/neighbors.size()); // Divide by number of neighbors, to average
    }

    void resetGradient() {
      exitGradient = null;
    }
  }

  private class CellEdge {

    private final LineSegment2D lineSegment; // We store this to check intersections with any walls that we add
    private final Cell c1, c2;

    CellEdge(Cell c1, Cell c2) {
      this.c1 = c1;
      this.c2 = c2;
      this.lineSegment = new LineSegment2D(c1.getCoordinates(), c2.getCoordinates());
    }

    LineSegment2D asLineSegment() {
      return lineSegment;
    }

    /**
     * @return length-4 array with coordinates of c1 and c2
     */
    double[] getAsArray() {
      return new double[]{  c1.getCoordinates().x(),
                            c1.getCoordinates().y(),
                            c2.getCoordinates().x(),
                            c2.getCoordinates().y() };
    }

  }

  private class NodeSearchListener extends TraversalListenerAdapter<Cell, CellEdge> {

    private Cell targetCell;

    NodeSearchListener(Cell targetCell) {
      super();
      if (targetCell == null) {
        throw new IllegalArgumentException("Trying to search for null cell!");
      }
      this.targetCell = targetCell;
    }

    @Override
    public void vertexTraversed(VertexTraversalEvent<Cell> e) {
      Cell cell = e.getVertex();
      if (targetCell.equals(cell)) { // Base Case: all cells are distance 0.0 from themselves.
        cell.setDistToCell(targetCell, 0.0);
      } else {
        // Otherwise, distance is based on the neighboring cell that is closest to target cell.
        // Note that this works because we started the BFS at the target, so any strictly closer
        // cells have already been visited
        double minDist = Double.MAX_VALUE;
        for (Cell neighbor : Graphs.neighborListOf(roomGraph, cell)) {
          double distanceThroughNeighbor = neighbor.getDistToCell(targetCell) + cell.euclideanDistFrom(neighbor);
          minDist = Math.min(minDist, distanceThroughNeighbor);
        }
        cell.setDistToCell(targetCell, minDist);
      }

    }

  }

  private class ExitSearchListener extends TraversalListenerAdapter<Cell, CellEdge> {

    @Override
    public void vertexTraversed(VertexTraversalEvent<Cell> e) {
      Cell cell = e.getVertex();
      if (exits.contains(cell)) { // base case: all exists are distance 0.0 from exit
        cell.setDistToExit(0.0);
      } else { // otherwise, distance is based on the neighboring cell that is closest to an exit
        for (Cell neighbor : Graphs.neighborListOf(roomGraph, cell)) {
          double distanceThroughNeighbor = neighbor.getDistToExit() + cell.euclideanDistFrom(neighbor);
          cell.setDistToExit(Math.min(cell.getDistToExit(), distanceThroughNeighbor));
          cell.resetGradient(); // Erases any cached gradient, forcing it to update next time getGradient() is called
        }
      }
    }

  }

}