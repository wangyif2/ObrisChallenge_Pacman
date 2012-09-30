import com.orbischallenge.pacman.api.common.MoveDir;
import com.orbischallenge.pacman.api.java.JUtil;
import com.orbischallenge.pacman.api.java.Maze;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A graph representation of the maze. Important: the methods are not optimized
 * for best performance. If your AI is likely to do heavy calculation, consider
 * improving some of the methods below. You are welcome to supply your own
 * methods or classes.
 */
public class MazeGraph {

	private static Maze maze;

	private Map<Point, Map<Point, List<Point>>> graph;

	public MazeGraph(Maze maze) {
		this.maze = maze;
		makeGraph();
	}

	public Map<Point, Map<Point, List<Point>>> getGraph() {
		return graph;
	}

	/**
	 * Construct a graph representation of the maze.
	 * 
	 * @return
	 */
	private void makeGraph() {
		this.graph = new HashMap<Point, Map<Point, List<Point>>>();
		for (int j = 0; j < maze.getHeight(); j++) {
			for (int i = 0; i < maze.getWidth(); i++) {
				Point node = new Point(i, j);
				// It's a node if the tile is an intersection or a dead end
				if (maze.isIntersection(node) || maze.isDeadEnd(node)) {
					Map<Point, List<Point>> connected = new HashMap<Point, List<Point>>();
					for (MoveDir dir : MoveDir.values()) {
						List<Point> path = getPathToNextNode(node, dir);
						if (path.size() > 0) {
							// Get the end node, which is the nearest node of
							// this one
							Point endNode = path.get(path.size() - 1);
							// Put in the end node and the path leading to it
							connected.put(endNode, path);
						}
					}
					// Put in this node and its connected node->path maps
					graph.put(node, connected);
				}
			}
		}
	}

	/**
	 * Get path to the next intersection/dead end, which is a node
	 * 
	 * @param tile
	 * @param dir
	 * @return List<Point> representing the path as a list of connected tiles
	 *         from the current tile (exclusive) to the nearest node tile
	 *         (inclusive) in the given direction
	 */
	public static List<Point> getPathToNextNode(Point tile, MoveDir dir) {
		ArrayList<Point> path = new ArrayList<Point>();
		Point dirVector = JUtil.getVector(dir);
		Point currTile;
		Point nextTile = JUtil.vectorAdd(tile, dirVector);
		while (maze.isAccessible(nextTile)) {
			currTile = nextTile; // move to the next tile
			path.add(currTile); // add the curr tile to path
			if (maze.isIntersection(currTile)) {
				return path;
			}
			if (maze.isCorner(currTile)) {
				// Turn corner, get the updated direction
				dirVector = turnCorner(currTile, dirVector);
			}
			// Move to the next tile to the new location
			nextTile = JUtil.vectorAdd(currTile, dirVector);
		}
		return path;
	}

	/**
	 * Turn direction at a corner tile
	 * 
	 * @param cornerTile
	 *            - This given tile must be a corner tile
	 * @param currDirVector
	 * @return Point
	 */
	private static Point turnCorner(Point cornerTile, Point currDirVector) {
		for (Point perVector : JUtil.getPerpendiculars(currDirVector)) {
			Point newTile = JUtil.vectorAdd(cornerTile,
					perVector);
			if (maze.isAccessible(newTile)) {
				return perVector;
			}
		}
		return currDirVector;
	}

	/**
	 * Find all paths from a given starting tile to a goal tile, with maximum
	 * number of nodes in each path. A path is a list of connected tiles.
	 * 
	 * @param start
	 *            - the starting tile, doesn't have to be a node
	 * @param goal
	 *            - to tile to look for
	 * @param nodeLimit
	 *            - maximum number of nodes we want to have in our path
	 * @return List<List<Point>> - List of paths
	 */
	public List<List<Point>> getPaths(Point start, Point goal, int nodeLimit) {
		List<List<Point>> paths = new ArrayList<List<Point>>();
		for (MoveDir dir : MoveDir.values()) {
			List<Point> path = getPathToNextNode(start, dir);
			if (path.size() > 0) {
				// Check if our goal is already in the nearby path
				if (path.contains(goal)) {
					paths.add(path.subList(0, path.indexOf(goal) + 1));
				} else {
					Point node = path.get(path.size() - 1);
					for (List<Point> newPath : this.graph.get(node).values()) {
						// Don't go back to the start at the first node
						if (!newPath.contains(start)) {
							List<Point> explored = Arrays.asList(new Point[] {
									start, node });
							findPathFromNode(paths, path, newPath, explored,
									goal, nodeLimit);
						}
					}
				}
			}
		}
		return paths;
	}

	/**
	 * Recursive helper method for graph search starting from a node
	 * 
	 * @param paths
	 *            - accumulating all paths to the goal tile
	 * @param currPath
	 *            - our current path so far
	 * @param newPath
	 *            - the path to be searched
	 * @param explored
	 *            - nodes we have visited so far
	 * @param goal
	 *            - to tile to look for
	 * @param nodeLimit
	 *            - maximum number of nodes we want to have in our path
	 */
	private void findPathFromNode(List<List<Point>> paths,
			List<Point> currPath, List<Point> newPath, List<Point> explored,
			Point goal, int nodeLimit) {
		// If we have found the goal in the new path, add the sublist in which
		// the goal is the
		// end tile of our current path, and save it.
		if (newPath.contains(goal)) {
			List<Point> pathToGoal = new ArrayList<Point>(currPath);
			pathToGoal.addAll(newPath.subList(0, newPath.indexOf(goal) + 1));
			paths.add(pathToGoal);
			return;
		}
		Point node = newPath.get(newPath.size() - 1);
		// Check if the current node has been visited, if so, don't go back
		// Check if the number of node visited exceeds the node limit
		if (explored.contains(node) || explored.size() > nodeLimit) {
			return;
		}
		// Add the node to our list of explored nodes
		List<Point> newExplored = new ArrayList<Point>(explored);
		newExplored.add(node);
		// Add the searched path to our current path
		List<Point> newCurrPath = new ArrayList<Point>(currPath);
		newCurrPath.addAll(newPath);
		// Check all paths connected to the current node
		for (List<Point> newNewPath : this.graph.get(node).values()) {
			findPathFromNode(paths, newCurrPath, newNewPath, newExplored, goal,
					nodeLimit);
		}
	}

	/**
	 * Get a list of MoveDir objects which Pacman can use to navigate itself
	 * through a path.
	 * 
	 * @param start
	 *            - the starting tile
	 * @param path
	 *            - the path leading out of the starting tile, a list of
	 *            connected tiles
	 * @return List<MoveDir>
	 */
	public static List<MoveDir> pathToMoveDir(Point start, List<Point> path) {
		List<MoveDir> MoveDirList = new ArrayList<MoveDir>();
		Point currTile = start;
		for (Point nextTile : path) {
			Point dirVector = JUtil.vectorSub(nextTile, currTile);
			MoveDirList.add(JUtil.getMoveDir(dirVector));
			currTile = nextTile;
		}
		return MoveDirList;
	}
}
