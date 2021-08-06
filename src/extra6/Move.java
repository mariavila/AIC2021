package extra6;

import aic2021.user.*;

import java.util.function.Function;

public class Move {
    UnitController uc;
    Tactics tactics;

    Direction[] dirs = Direction.values();

    public Move(UnitController uc) {
        this.uc = uc;
        this.tactics = new Tactics(uc);
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
    int x2 = 1051;
    int y1 = -1;
    int y2 = 1051;
    boolean x1found = false;
    boolean x2found = false;
    boolean y1found = false;
    boolean y2found = false;
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

    void moveAvoidingEnemies(Location target) {
        Location myLoc = uc.getLocation();
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
        UnitType[] excluded = {UnitType.EXPLORER, UnitType.WORKER, UnitType.TRAPPER};
        int randomNumber = (int)(uc.getRandomDouble()*2);
        if (enemies.length > 0) {
            UnitInfo enemy = tactics.getClosestDangerousEnemy(enemies, excluded);
            if(enemy != null) {
                Direction enemyDir = myLoc.directionTo(enemy.getLocation());
                Direction myDir = myLoc.directionTo(target);
                if (myDir == enemyDir) {
                    if (randomNumber == 1) {
                        moveTo(myLoc.add(myDir.opposite().rotateLeft()), false);
                    } else {
                        moveTo(myLoc.add(myDir.opposite().rotateRight()), false);
                    }
                } else if (myDir.rotateLeft() == enemyDir) {
                    moveTo(myLoc.add(myDir.rotateRight().rotateRight()), false);
                } else if (myDir.rotateRight() == enemyDir) {
                    moveTo(myLoc.add(myDir.rotateLeft().rotateLeft()), false);
                } else {
                    moveTo(target, false);
                }
            }
        }
        moveTo(target, false);
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

    Location explore(){
        Location myLoc = uc.getLocation();
        findMapEdges();
        if (edgeTarget != null && myLoc.distanceSquared(edgeTarget) <= 8 || counter > 50) {
            edgeTarget = null;
            counter = 0;
        } else counter++;

        if (!edgesFound) {
            if (x1found && x2found && y1found && y2found) edgesFound = true;
        }

        while (edgeTarget == null || (enemyBase != null && edgeTarget.distanceSquared(enemyBase) <= 50)) {
            edgeTarget = new Location(getRandomNumber(x1, x2), getRandomNumber(y1, y2));
        }

        return edgeTarget;
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
                        y1found = true;
                    }
                    if (myLoc.y < loc.y && loc.y < y2) {
                        y2 = loc.y - 1;
                        edgeTarget = null;
                        y2found = true;
                    }
                }
            }
            if (loc.y == myLoc.y) {
                if (uc.isOutOfMap(loc)) {
                    if (myLoc.x > loc.x && loc.x > x1) {
                        x1 = loc.x + 1;
                        edgeTarget = null;
                        x1found = true;
                    }
                    if (myLoc.x < loc.x && loc.x < x2) {
                        x2 = loc.x - 1;
                        edgeTarget = null;
                        x2found = true;
                    }
                }
            }
        }
    }

    void init() {
        Location myLoc = uc.getLocation();

        x1 = myLoc.x - 50;
        x2 = myLoc.x + 50;
        y1 = myLoc.y - 50;
        y2 = myLoc.y + 50;
    }

    Direction rotateClosest(Direction dirEscape, Direction dirTarget, int times) {
        Direction finalDir = dirEscape;
        Boolean rotateLeft = false;
        for(int i = 0; i < 4; i++) {
            finalDir = finalDir.rotateLeft();
            if (finalDir.isEqual(dirTarget)) {
                rotateLeft = true;
                break;
            }
        }
        for(int j = 0; j < times; j++) {
            if (rotateLeft) finalDir = dirEscape.rotateLeft();
            else finalDir = dirEscape.rotateRight();
        }
        return finalDir;
    }
}