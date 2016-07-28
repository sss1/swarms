package swarms;

import math.geom2d.Point2D;
import math.geom2d.line.LineSegment2D;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.HashSet;
import java.util.Set;

/**
 * A room is modeled as a grid-shaped graph. Walls can be simulated by removing edges.
 * Created by sss1 on 7/28/16.
 */
public class Room {

  private Graph<Cell, CellEdge> roomGraph;
  private int nCellsX, nCellsY;
  private Point2D min, max; // Coordinates of the bottom-left and top-right corners of the room

  public Room(Point2D min, Point2D max, double fineness) {

    nCellsX = 1 + ((int) ((max.x() - min.x()) / fineness));
    nCellsY = 1 + ((int) ((max.y() - min.y()) / fineness));

    roomGraph = new SimpleGraph<>(CellEdge.class);

    Cell[][] grid = new Cell[nCellsX][nCellsY]; // Temporary organization for easily adding grid edges

    for (int i = 0; i < nCellsX; i++) {
      for (int j = 0; j < nCellsY; j++) {

        double x = min.x() + i * fineness;
        double y = min.y() + j * fineness;
        grid[i][j] = new Cell(x, y);
        roomGraph.addVertex(grid[i][j]);
        if (i > 0 && j > 0) {
          roomGraph.addEdge(grid[i][j], grid[i - 1][j - 1]); // add edge to top-left
        }
        if (i > 0) {
          roomGraph.addEdge(grid[i][j], grid[i - 1][j]); // add edge to left
        }
        if (i > 0 && j < nCellsY - 2) {
          roomGraph.addEdge(grid[i][j], grid[i - 1][j + 1]); // add edge to bottom-left
        }
        if (i > 0) {
          roomGraph.addEdge(grid[i][j], grid[i][j - 1]); // add edge to top
        }
      }

    }

  }

  /**
   * Simulates a wall by removing any edges that cross the line segment between the two input points
   */
  public void addWall(LineSegment2D wall) {

    Set<CellEdge> toRemove = new HashSet<>();

    for (CellEdge e : roomGraph.edgeSet()) {
      if (LineSegment2D.intersects(wall, e.asLineSegment())) {
        toRemove.add(e);
      }
    }

    roomGraph.removeAllEdges(toRemove);

  }

  /**
   * A single ``grid cell'' of the room
   */
  private class Cell {

    private final Point2D coordinates; // location of the cell
    private Set<Agent> agents; // all agents inside this cell

    Cell(double x, double y) {
      coordinates = new Point2D(x, y);
      agents = new HashSet<>();
    }

    Point2D getCoordinates() {
      return coordinates;
    }

    Set<Agent> getAgents() {
      return agents;
    }

  }

  private class CellEdge {

    private final LineSegment2D lineSegment;

    public CellEdge(Cell c1, Cell c2) {
      this.lineSegment = new LineSegment2D(c1.getCoordinates(), c2.getCoordinates());
    }

    LineSegment2D asLineSegment() {
      return lineSegment;
    }

  }

}