package challenge24;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.UnitController;

import java.util.function.Function;

public class Move {
    UnitController uc;
    public Move(UnitController uc) {
        this.uc = uc;
    }

    final int INF = 1000000;

    boolean rotateRight = true; //if I should rotate right or left
    Location lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    Location prevTarget = null; //previous target

    Direction exploringDir = null;

    void moveTo(Location target, boolean reckless) {
        moveTo(target, reckless, (Direction dir)->uc.canMove(dir));
    }

    void moveTo(Location target, boolean reckless, Function<Direction, Boolean> conditions){
        Location[] traps = uc.senseTraps();

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
            if (conditions.apply(dir) && safeLocation(myLoc.add(dir),traps,reckless)){
                uc.move(dir);
                return;
            }
            Location newLoc = myLoc.add(dir);
            if (uc.isOutOfMap(newLoc)) rotateRight = !rotateRight;
                //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
            else lastObstacleFound = myLoc.add(dir);
            if (rotateRight) {
                dir = dir.rotateRight();
            }
        }

        if (conditions.apply(dir) && safeLocation(myLoc.add(dir),traps,reckless)) uc.move(dir);
    }

    //clear some of the previous data
    private void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
    }

    private boolean safeLocation(Location loc, Location[] traps, boolean reckless) {
        boolean isSafe = true;

        if (reckless) return true;

        for(int i=0; i<traps.length; i++) {
            if (loc.isEqual(traps[i])) isSafe = false;
            break;
        }

        return isSafe;
    }

    void moveToLimited(Location target, boolean reckless){
        Location[] traps = uc.senseTraps();

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
        if (uc.canMove(dir)) resetPathfinding();

        //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
        //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
        int dirchange = 0;
        for (int i = 0; i < 16; ++i){
            if (uc.canMove(dir) && safeLocation(myLoc.add(dir),traps,reckless)){
                uc.move(dir);
                return;
            }
            Location newLoc = myLoc.add(dir);
            if (uc.isOutOfMap(newLoc)) rotateRight = !rotateRight;
                //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
            else lastObstacleFound = myLoc.add(dir);
            if (rotateRight) {
                dir = dir.rotateRight();
                dirchange += 1;
            }
            else {
                dir = dir.rotateLeft();
                dirchange -= 1;
            }
            if (dirchange >= 3 || dirchange <= -3) return;
        }

        if (uc.canMove(dir) && safeLocation(myLoc.add(dir),traps,reckless)) uc.move(dir);
    }

    void explore(){
        Direction[] dirs = Direction.values();
        int moved = 0;

        if (exploringDir != null) {
            if (uc.canMove(exploringDir)) {
                uc.move(exploringDir);
                return;
            }
        }

        while (moved < 10) {
            int index = (int)(uc.getRandomDouble()*(dirs.length - 1));

            if (uc.canMove(dirs[index])) {
                exploringDir = dirs[index];
                uc.move(dirs[index]);
            }
            moved++;
        }
    }
}