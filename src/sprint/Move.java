package sprint;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitType;

import java.util.function.Function;

public class Move {
    UnitController uc;
    public Move(UnitController uc) {
        this.uc = uc;
    }

    final int INF = 1000000000;

    boolean rotateRight = true; //if I should rotate right or left
    Location lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    Location prevTarget = null; //previous target

    Location enemyBase = null;
    boolean edgesFound = false;
    Location edgeTarget = null;
    int x1 = -1;
    int x2 = 1100;
    int y1 = -1;
    int y2 = 1100;
    int counter = 0;

    void moveTo(Location target, boolean reckless) {
        moveTo(target, reckless, (Direction dir)->uc.canMove(dir));
    }

    void moveTo(Location target, boolean reckless, Function<Direction, Boolean> conditions){
        Location[] trapLocs = uc.senseTraps();
        Location[] dangerLocs = dangerousLocations();

        //No target? ==> bye!
        if (target == null || !uc.canMove()) return;

        //different target? ==> previous data does not help!
        if (prevTarget == null || !target.isEqual(prevTarget)) resetPathfinding();

        //If I'm at a minimum distance to the target, I'm free!
        Location myLoc = uc.getLocation();
        int d = myLoc.distanceSquared(target);
        if (d <= minDistToEnemy) resetPathfinding();

        //Update data
        prevTarget = target;
        minDistToEnemy = Math.min(d, minDistToEnemy);

        //If there's an obstacle I try to go around it [until I'm free] instead of going to the target directly
        Direction dir = myLoc.directionTo(target);
        if (lastObstacleFound != null) dir = myLoc.directionTo(lastObstacleFound);

        //This should not happen for a single unit, but whatever
        if (conditions.apply(dir)) resetPathfinding();

        //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
        //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
        for (int i = 0; i < 16; ++i){
            if (conditions.apply(dir) && safeLocation(myLoc.add(dir), dangerLocs, trapLocs, reckless)){
                uc.move(dir);
                return;
            }
            Location newLoc = myLoc.add(dir);
            if (uc.isOutOfMap(newLoc)) rotateRight = !rotateRight;
                //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
            else lastObstacleFound = myLoc.add(dir);
            if (rotateRight) dir = dir.rotateRight();
            else dir = dir.rotateLeft();
        }

        if (conditions.apply(dir) && safeLocation(myLoc.add(dir), dangerLocs, trapLocs, reckless)) uc.move(dir);
    }

    public boolean isSafe(Direction dir) {
        Location[] trapLocs = uc.senseTraps();
        Location[] dangerLocs = dangerousLocations();
        return safeLocation(uc.getLocation().add(dir), dangerLocs, trapLocs, false);
    }

    //clear some of the previous data
    private void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
    }

    private boolean safeLocation(Location loc, Location[] dangerLocs, Location[] trapLocs, boolean reckless) {
        if (reckless) return true;

        boolean isSafe = true;

        for(Location danger: trapLocs) {
            if (loc.isEqual(danger)) {
                isSafe = false;
                break;
            }
        }

        for(Location danger: dangerLocs) {
            if (loc.isEqual(danger)) {
                isSafe = false;
                break;
            }
        }

        return isSafe;
    }

    private Location[] dangerousLocations(){
        Direction[] dirs = Direction.values();
        Location myLoc = uc.getLocation();
        Location[] dangerLocs = new Location[9];
        int index = 0;

        for (Direction dir: dirs) {
            Location target = myLoc.add(dir);

            if (enemyBase != null) {
                int dist = enemyBase.distanceSquared(target);
                if (dist <= UnitType.BASE.getAttackRange()) {
                    dangerLocs[index] = target;
                    index++;
                    continue;
                }
            }

            index++;
        }
        return dangerLocs;
    }

    void setEnemyBase(Location target) {
        enemyBase = target;
    }

    void explore(){
        findMapEdges();
        if (edgeTarget != null && uc.getLocation().distanceSquared(edgeTarget) <= 8 || counter > 20) {
            edgeTarget = null;
            counter = 0;
        } else counter++;

        if (edgeTarget == null) {
            if (y1 == -1 && x1 == -1) edgeTarget = new Location(x1, y1);
            else if (y2 == -1 && x2 == -1) edgeTarget = new Location(x2, y2);
            else if (x1 == -1 && y2 == 1100) edgeTarget = new Location(x1, y2);
            else if (x2 == 1100 && y1 == -1) edgeTarget = new Location(x2, y1);
            else {
                if (x1 != -1 && y1 != -1 && x2 != 1100 && y2 != 1100) edgesFound = true;
                edgeTarget = new Location(getRandomNumber(x1, x2), getRandomNumber(y1, y2));
            }
        }
        moveTo(edgeTarget, false);
    }

    public int getRandomNumber(int min, int max) {
        return (int) ((uc.getRandomDouble() * (max - min)) + min);
    }

    void findMapEdges() {
        if (edgesFound) return;
        Location[] myLocs = uc.getVisibleLocations();
        Location myLoc = uc.getLocation();
        for (Location loc: myLocs) {
            if (loc.x == myLoc.x) {
                if (uc.isOutOfMap(loc)) {
                    if (myLoc.y > loc.y && loc.y > y1) {
                        y1 = loc.y + 1;
                        edgeTarget = null;
                    }
                    if (myLoc.y < loc.y && loc.y < y2) {
                        y2 = loc.y - 1;
                        edgeTarget = null;
                    }
                }
            }
            if (loc.y == myLoc.y) {
                if (uc.isOutOfMap(loc)) {
                    if (myLoc.x > loc.x && loc.x > x1) {
                        x1 = loc.x + 1;
                        edgeTarget = null;
                    }
                    if (myLoc.x < loc.x && loc.x < x2) {
                        x2 = loc.x - 1;
                        edgeTarget = null;
                    }
                }
            }
        }
    }
}