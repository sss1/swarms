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
  private final double fineness;
  private final Point2D min;
  private final Cell[][] grid;
  private final Cell outside;


  Room(Point2D min, Point2D max, double fineness) {

    this.min = min;
    this.fineness = fineness;

    int nCellsX = 1 + ((int) ((max.x() - min.x()) / fineness));
    int nCellsY = 1 + ((int) ((max.y() - min.y()) / fineness));

//    System.out.println("Total number of nodes: " + (nCellsX * nCellsY));

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

    outside = new Cell(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    // TODO: Remove cell at infinity, now that agents stop once they leave the room

    exits = new ArrayList<>();
    walls = new ArrayList<>();

  }

  void addExit(Point2D exitLocation) {
    Cell cell = getCellFromPosition(exitLocation);
    if (Double.isInfinite(cell.getCoordinates().x())) { // If exit is outside graph, throw exception
      throw new IllegalArgumentException("Cannot place an exit outside of the graph.");
    }
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
    // For each exit, do a BFS. Upon reaching a vertex, set its distance to the minimum distance through each of its
    // neighboring vertices.
    for (Cell exit : exits) {
      BreadthFirstIterator<Cell, CellEdge>  iterator = new BreadthFirstIterator<>(roomGraph, exit);
      iterator.addTraversalListener(new labelingTraversalListener());
      while (iterator.hasNext()) { iterator.next(); }
    }
    logarithmizeDistance();
  }

  private void logarithmizeDistance() {
    for (Cell cell : roomGraph.vertexSet()) {
      cell.setDistToExit((cell.getDistToExit() > Double.MIN_VALUE) ? 20.0 * Math.log(cell.getDistToExit()) : 0.0);
    }
  }

  private Cell getCellFromPosition(Point2D position) {
    int idxX = (int) ((position.x() - min.x()) / fineness);
    if (idxX < 0 || idxX >= grid.length) {
      return outside;
    }
    int idxY = (int) ((position.y() - min.y()) / fineness);
    if (idxY < 0 || idxY >= grid[0].length) {
      return outside;
    }
    return grid[idxX][idxY];
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
    private Vector2D gradient;
    private HashMap<Cell, Double> distMap; // Distance to each other cell

    Cell(double x, double y) {
      coordinates = new Point2D(x, y);
      distToExit = Double.POSITIVE_INFINITY;
      gradient = null;
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
          Point2D.distance(this.getCoordinates(), ((Cell) obj).getCoordinates()) < fineness / 10.0;
    }

    Point2D getCoordinates() {
      return coordinates;
    }

    Vector2D getGradientToCell(Cell cell) {
      gradient = new Vector2D(0.0, 0.0);

      Collection<Cell> neighbors = Graphs.neighborListOf(roomGraph, this);
      for (Cell neighbor : neighbors) {
        // diffDistance is positive if the neighbor is closer to the exit than the current cell, and negative otherwise
        double diffDistance = getDistToCell(cell) - neighbor.getDistToCell(cell);
        gradient = gradient.plus((new Vector2D(coordinates, neighbor.getCoordinates())).times(diffDistance));
      }
      return gradient.times(1.0/neighbors.size()); // Divide by number of neighbors, to average
    }

    double getDistToCell(Cell cell) {
      if (this.equals(cell)) { return 0.0; }
      if (!distMap.containsKey(cell)) {
        double minDist = Double.MAX_VALUE;
        for (Cell neighbor : Graphs.neighborListOf(roomGraph, this)) {
          double distToNeighbor = Point2D.distance(coordinates, neighbor.getCoordinates());
          double distThroughNeighbor = neighbor.getDistToCell(cell) + distToNeighbor;
          minDist = Math.min(minDist, distThroughNeighbor);
        }
        distMap.put(cell, minDist);
      }
      return distMap.get(cell);

    }

    double getDistToExit() {
      return distToExit;
    }

    void setDistToExit(double distToExit) {
      this.distToExit = distToExit;
    }

    Vector2D getGradient() {
      if (gradient == null) { setGradient(); }
      return gradient;
    }

    void setGradient() {
      if (Double.isInfinite(coordinates.x())) { // Gradient outside graph is zero
        this.gradient = new Vector2D(0.0, 0.0);
        return;
      }

      gradient = new Vector2D(0.0, 0.0);

      Collection<Cell> neighbors = Graphs.neighborListOf(roomGraph, this);
      for (Cell neighbor : neighbors) {
        // diffDistance is positive if the neighbor is closer to the exit than the current cell, and negative otherwise
        double diffDistance = distToExit - neighbor.getDistToExit();
        gradient = gradient.plus((new Vector2D(coordinates, neighbor.getCoordinates())).times(diffDistance));
      }
      gradient = gradient.times(1.0/neighbors.size()); // Divide by number of neighbors, to average
    }

    void resetGradient() {
      gradient = null;
    }
  }

  private class CellEdge {

    private final LineSegment2D lineSegment;
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

  private class labelingTraversalListener extends TraversalListenerAdapter<Cell, CellEdge> {

    @Override
    public void vertexTraversed(VertexTraversalEvent<Cell> e) {
      Cell cell = e.getVertex();
      if (exits.contains(cell)) { // base case: all exists are distance 0.0 from exit
        cell.setDistToExit(0.0);
      } else { // otherwise, distance is based on the neighboring cell that is closest to an exit
        for (Cell neighbor : Graphs.neighborListOf(roomGraph, cell)) {
          double distanceThroughNeighbor = neighbor.getDistToExit() +
              Point2D.distance(cell.getCoordinates(), neighbor.getCoordinates());
          cell.setDistToExit(Math.min(cell.getDistToExit(), distanceThroughNeighbor));
          cell.resetGradient();
        }
      }
    }

  }

}