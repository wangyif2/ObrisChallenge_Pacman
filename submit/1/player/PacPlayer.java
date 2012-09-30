import com.orbischallenge.pacman.api.common.GhostState;
import com.orbischallenge.pacman.api.common.MazeItem;
import com.orbischallenge.pacman.api.common.MoveDir;
import com.orbischallenge.pacman.api.java.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The Player class is the parent class of your AI player. It is just like a
 * template. Your AI player class (called PacPlayer) must implement the
 * following methods.
 */
public class PacPlayer implements Player {

    private int lives = 3;
    boolean isFirstStep = true;
    private MazeGraph graph;
    private static final Integer SAFE_DIST = 2;
//    private Queue<Point> forwardTravelPath, backwardTravelPath;

    /**
     * This is method decides Pacmans moving direction in the next frame (See
     * Frame Concept). The parameters represent the maze, ghosts, Pacman, and
     * score after the execution of last frame. In the next frame, the game will
     * call this method to set Pacmans direction and let him move (see Pacmans
     * Move).
     *
     * @param maze   A Maze object representing the current maze.
     * @param ghosts An array of Ghost objects representing the four ghosts.
     * @param pac    A Pac object representing Pacman
     * @param score  The current score
     * @return MoveDir
     */
    public MoveDir calculateDirection(Maze maze, Ghost[] ghosts, Pac pac,
                                      int score) {

        // Get the current tile of Pacman
        Point pacTile = pac.getTile();
        MazeItem curItem = maze.getTileItem(pacTile);

        if (canProceed(maze, pacTile, isFirstStep)) {
            Point nextTile = JUtil.vectorAdd(pac.getTile(), JUtil.getVector(pac.getDir()));
            if (maze.isIntersection(nextTile)) {
                return checkForGhostInNIntersection(maze, nextTile, ghosts) ? JUtil.getOppositeDir(pac.getDir()) : pac.getDir();
            } else {
                GhostState curGhostState = checkForGhostInNBlock(pac, ghosts, pac.getDir());
                return isGhostExistOrHarmful(curGhostState) ? JUtil.getOppositeDir(pac.getDir()) : pac.getDir();
            }
        } else if (isCorner(maze, pacTile) && !isFirstStep)
            return getCornerDir(maze, pac, ghosts);
        else if (isDeadEnd(maze, pacTile))
            return getDeadEndDir(pac.getDir());
        else if (maze.isIntersection(pacTile) && !isFirstStep)
            return getIntersectionDir(maze, pac, ghosts, pacTile);
        else {
            isFirstStep = false;
            return MoveDir.RIGHT;
        }
    }

    private boolean checkForGhostInNIntersection(Maze maze, Point nextTile, Ghost[] ghosts) {
        List<Point> neighbours = maze.getAccessibleNeighbours(nextTile);
        List<Point> ghostPos = getGhostPos(ghosts);
        for (Point n : neighbours) {
            if (ghostPos.contains(n))
                return true;
        }
        return false;
    }

    private Boolean isGhostExistOrHarmful(GhostState ghostState) {
        return ghostState != null && (ghostState != GhostState.FLEE && ghostState != GhostState.FRIGHTEN);
    }

    private GhostState checkForGhostInNBlock(Pac pac, Ghost[] ghosts, MoveDir dir) {
        List<Point> ghostPos = getGhostPos(ghosts);
        Point curTile = pac.getTile();
        List<Point> curPath = MazeGraph.getPathToNextNode(curTile, dir);
        for (int i = 0; i < Math.min(SAFE_DIST, curPath.size()); i++) {
            if (ghostPos.contains(curPath.get(i)))
                return ghosts[ghostPos.indexOf(curPath.get(i))].getState();
        }

        return null;
    }

    private boolean isCorner(Maze maze, Point pacTile) {
        if (maze.isAccessible(pacTile)) {
            List<Point> neighbours = maze.getAccessibleNeighbours(pacTile);
            if ((neighbours.size() == 2) &&
                    (neighbours.get(0).getX() != neighbours.get(1).getX()) &&
                    (neighbours.get(0).getY() != neighbours.get(1).getY())) {
                return true;
            }
        }
        return false;
    }

    private MoveDir getIntersectionDir(Maze maze, Pac pac, Ghost[] ghosts, Point pacTile) {
//        Point lastPoint = JUtil.vectorAdd(pacTile, JUtil.getVector(JUtil.getOppositeDir(pac.getDir())));
        Map<Point, List<Point>> listMap = graph.getGraph().get(pacTile);
        Collection<List<Point>> curChoice = listMap.values();
        List<Point> ghostPos = getGhostPos(ghosts);

        MoveDir dir = pac.getDir();
        Integer maxScore = -205, nextMaxScore = -205;

        for (Point endP : listMap.keySet()) {
            Integer score = 0;
            score = getScore(maze, ghosts, ghostPos, score, endP);
            for (Point point : listMap.get(endP))
                score = getScore(maze, ghosts, ghostPos, score, point);

            if (score > maxScore) {
                maxScore = score;
                dir = getDirFromPoint(listMap.get(endP).get(0), pacTile);
            }
        }

        if (maxScore <= 0) {
            Collection<Point> nextChoice = graph.getGraph().get(pacTile).keySet();

            for (Point point : nextChoice) {
                Collection<List<Point>> endPointChoices = graph.getGraph().get(point).values();
                Integer score = 0;

                for (List<Point> endPointList : endPointChoices) {
                    for (Point endPointPoint : endPointList) {
                        MazeItem item = maze.getTileItem(endPointPoint);
                        switch (item) {
                            case BLANK:
                                break;
                            case DOT:
                                score += 1;
                                break;
                            case POWER_DOT:
                                score += 40;
                                break;
                            case TELEPORT:
                                break;
                        }
                        if (ghostPos.contains(point)) {
                            GhostState ghostStates = ghosts[ghostPos.indexOf(point)].getState();
                            score = getScoreGhost(score, ghostStates);
                        }
                    }
                }

                if (score > nextMaxScore) {
                    nextMaxScore = score;
                    dir = getDirFromPoint(listMap.get(point).get(0), pacTile);
                }
            }
        }

        if (maxScore <= 0 && nextMaxScore <= 0) {
            while (true) {
                Integer rand = (int) (Math.random() * 5);
                switch (rand) {
                    case 0:
                        Point nextTileL = JUtil.vectorAdd(pac.getTile(), JUtil.getVector(MoveDir.LEFT));
                        GhostState curGhostStateLeft = checkForGhostInNBlock(pac, ghosts, MoveDir.LEFT);
                        if (maze.isAccessible(nextTileL) && !isGhostExistOrHarmful(curGhostStateLeft)) {
                            return MoveDir.LEFT;
                        }
                        break;
                    case 1:
                        Point nextTileR = JUtil.vectorAdd(pac.getTile(), JUtil.getVector(MoveDir.RIGHT));
                        GhostState curGhostStateRight = checkForGhostInNBlock(pac, ghosts, MoveDir.RIGHT);
                        if (maze.isAccessible(nextTileR) && !isGhostExistOrHarmful(curGhostStateRight)) {
                            return MoveDir.RIGHT;
                        }
                        break;
                    case 2:
                        Point nextTileD = JUtil.vectorAdd(pac.getTile(), JUtil.getVector(MoveDir.DOWN));
                        GhostState curGhostStateDown = checkForGhostInNBlock(pac, ghosts, MoveDir.DOWN);
                        if (maze.isAccessible(nextTileD) && !isGhostExistOrHarmful(curGhostStateDown)) {
                            return MoveDir.DOWN;
                        }
                        break;
                    case 3:
                        Point nextTileU = JUtil.vectorAdd(pac.getTile(), JUtil.getVector(MoveDir.UP));
                        GhostState curGhostStateUP = checkForGhostInNBlock(pac, ghosts, MoveDir.UP);
                        if (maze.isAccessible(nextTileU) && !isGhostExistOrHarmful(curGhostStateUP))
                            return MoveDir.UP;
                        break;
                    case 4:
                        Point nextTileUu = JUtil.vectorAdd(pac.getTile(), JUtil.getVector(MoveDir.UP));
                        GhostState curGhostStateUPup = checkForGhostInNBlock(pac, ghosts, MoveDir.UP);
                        if (maze.isAccessible(nextTileUu) && !isGhostExistOrHarmful(curGhostStateUPup))
                            return MoveDir.UP;
                        break;
                }
            }
        } else
            return dir;
    }

    private Integer getScore(Maze maze, Ghost[] ghost, List<Point> ghostPos, Integer score, Point point) {
        MazeItem item = maze.getTileItem(point);
        switch (item) {
            case BLANK:
                break;
            case DOT:
                score += 1;
                break;
            case POWER_DOT:
                score += 40;
                break;
            case TELEPORT:
                break;
        }
        if (ghostPos.contains(point)) {
            GhostState ghostStates = ghost[ghostPos.indexOf(point)].getState();
            score = getScoreGhost(score, ghostStates);
        }
        return score;
    }

    private Integer getScoreGhost(Integer score, GhostState ghostStates) {
        switch (ghostStates) {
            case IN_HOUSE:
                break;
            case CHASER:
                score = -50;
                break;
            case SCATTER:
                score = -50;
                break;
            case FRIGHTEN:
                score = +50;
                break;
            case FLEE:
                break;
        }
        return score;
    }

    private List<Point> getGhostPos(Ghost[] ghosts) {
        List<Point> points = new ArrayList<Point>();
        for (Ghost g : ghosts) {
            Point tile = g.getTile();
            points.add(tile);
        }
        return points;
    }

    private MoveDir getDirFromPoint(Point curPoint, Point nextPoint) {
        Point movingPoint = JUtil.vectorSub(curPoint, nextPoint);
        return JUtil.getMoveDir(movingPoint);
    }

    private MoveDir getDeadEndDir(MoveDir dir) {
        return JUtil.getOppositeDir(dir);
    }

    private MoveDir getCornerDir(Maze maze, Pac pac, Ghost[] ghosts) {
        MoveDir[] directions = JUtil.getPerpendicularDirs(pac.getDir());
        for (MoveDir dir : directions) {
            Point nextTile = JUtil.vectorAdd(pac.getTile(), JUtil.getVector(dir));
            if (maze.isAccessible(nextTile)) {
                GhostState curGhostState = checkForGhostInNBlock(pac, ghosts, pac.getDir());
                if (isGhostExistOrHarmful(curGhostState)) {
                    return JUtil.getOppositeDir(pac.getDir());
                } else
                    return dir;
            }
        }
        return pac.getDir();
    }

    private boolean canProceed(Maze maze, Point pacTile, Boolean isFirstStep) {
        return !(maze.isIntersection(pacTile) || isDeadEnd(maze, pacTile) || isCorner(maze, pacTile) || isFirstStep);
    }

    private boolean isDeadEnd(Maze maze, Point pacTile) {
        return maze.isDeadEnd(pacTile) && maze.getTileItem(pacTile) != MazeItem.TELEPORT;
    }

    /**
     * This method will be called by the game whenever a new level starts. The
     * parameters represent the game objects at their initial states. This
     * method will always be called before calculateDirection.
     *
     * @param maze   A Maze object representing the current maze.
     * @param ghosts An array of Ghost objects representing the four ghosts.
     * @param pac    A Pac object representing Pacman
     * @param score  The current score
     */
    public void onLevelStart(Maze maze, Ghost[] ghosts, Pac pac, int score) {
        isFirstStep = true;
        System.out.println("Java player start new level!");
        graph = new MazeGraph(maze);
    }

    /**
     * This method will be called by the game whenever Pacman receives a new
     * life, including the first life. The parameters represent the
     * repositioned game objects. This method will always be called before
     * calculateDirection and after onLevelStart.
     *
     * @param maze   A Maze object representing the current maze.
     * @param ghosts An array of Ghost objects representing the four ghosts.
     * @param pac    A Pac object representing Pacman
     * @param score  The current score
     */
    public void onNewLife(Maze maze, Ghost[] ghosts, Pac pac, int score) {
        isFirstStep = true;
        System.out.println("Hi, I still have " + lives + " lives left.");
        lives--;

    }

    ;
}