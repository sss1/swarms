package swarms;

import math.geom2d.Point2D;
import math.geom2d.line.LineSegment2D;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A room is modeled as a grid-shaped graph. Walls can be simulated by removing edges.
 * Created by sss1 on 7/28/16.
 */
class Room {

  private Graph<Cell, CellEdge> roomGraph;
  private ArrayList<LineSegment2D> walls;

  Room(Point2D min, Point2D max, double fineness) {

    int nCellsX = 1 + ((int) ((max.x() - min.x()) / fineness));
    int nCellsY = 1 + ((int) ((max.y() - min.y()) / fineness));

    roomGraph = new SimpleGraph<>(CellEdge.class);

    Cell[][] grid = new Cell[nCellsX][nCellsY]; // Temporary organization for easily adding grid edges

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

      walls = new ArrayList<>();

    }

  }

  /**
   * Simulates a wall by removing any edges that cross the line segment between the two input points
   */
  void addWall(LineSegment2D wall) {

    walls.add(wall);

    Set<CellEdge> toRemove = new HashSet<>();

    for (CellEdge e : roomGraph.edgeSet()) {
      if (LineSegment2D.intersects(wall, e.asLineSegment())) {
        toRemove.add(e);
      }
    }

    roomGraph.removeAllEdges(toRemove);

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

    Cell(double x, double y) {
      coordinates = new Point2D(x, y);
    }

    Point2D getCoordinates() {
      return coordinates;
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

}