package extra4;

import aic2021.user.*;

public class WolfPathfinder {

    UnitController uc;

    final int INF = 1000000000;
    boolean rotateRight = true; //if I should rotate right or left
    boolean rotate = false;
    Location lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    Location prevTarget = null; //previous target
    Location myLoc;
    Location enemyBase = null;
    Team myTeam;
    Location[] traps;
    UnitInfo[] enemies;
    Direction microDir;
    Direction[] myDirs;
    MicroInfo[] microInfo = new MicroInfo[9];
    int baseRange;
    boolean isEnemies;

    WolfPathfinder(UnitController uc){
        this.myDirs = Direction.values();
        this.uc = uc;
        this.myTeam = uc.getTeam();
        this.baseRange = UnitType.BASE.getAttackRange();
    }

    void setEnemyBase(Location target) {
        enemyBase = target;
    }

    Boolean getNextLocationTarget(Location target, UnitInfo[] enemies, boolean reckless){
        if (!uc.canMove()) return false;
        if (target == null) return false;
        isEnemies = false;
        //different target? ==> previous data does not help!
        if (prevTarget == null || !target.isEqual(prevTarget)) resetPathfinding();

        //If I'm at a minimum distance to the target, I'm free!
        myLoc = uc.getLocation();
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
        doMicro(enemies);

        for (int i = 0; i < 16; ++i){
            for (int j = 0; j < myDirs.length; j++) {
                if (myDirs[j] == dir) {
                    if (uc.canMove(dir) && ((!isEnemies && (enemyBase == null || myLoc.add(dir).distanceSquared(enemyBase) > baseRange)) || reckless)) {
                        uc.move(dir);
                        return true;
                    }
                    break;
                }
            }
            if (!rotate && myLoc.add(dir.rotateLeft()).distanceSquared(target) > myLoc.add(dir.rotateRight()).distanceSquared(target)) {
                rotateRight = true;
                rotate = true;
            }
            Location newLoc = myLoc.add(dir);
            if (uc.isOutOfMap(newLoc)) rotateRight = !rotateRight;
                //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
            else lastObstacleFound = myLoc.add(dir);
            if (rotateRight) dir = dir.rotateRight();
            else dir = dir.rotateLeft();
        }

        for (int j = 0; j < myDirs.length; j++) {
            if (myDirs[j] == dir) {
                if (uc.canMove(dir) && ((!isEnemies && (enemyBase == null || myLoc.add(dir).distanceSquared(enemyBase) > baseRange)) || reckless)) {
                    uc.move(dir);
                    return true;
                }
                break;
            }
        }

        if (microDir != Direction.ZERO) {
            uc.move(microDir);
            return true;
        }

        return false;
    }

    void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
    }

    public void doMicro(UnitInfo[] enemies) {
        traps = uc.senseTraps();
        int length = Math.min(enemies.length, 10);
        for (int i = 0; i < 9; i++) {
            Location target = myLoc.add(myDirs[i]);
            microInfo[i] = new MicroInfo(myLoc.add(myDirs[i]));

            if (enemyBase != null && target.distanceSquared(enemyBase) <= baseRange) {
                if (uc.canSenseLocation(enemyBase) && !uc.isObstructed(target, enemyBase)) microInfo[i].numEnemies += 10;
            }

            for(Location trap: traps) {
                if(trap.isEqual(target)) {
                    microInfo[i].numEnemies = 100;
                    break;
                }
            }

            for (int j = 0; j < length; j++) {
                Location enemyLoc = enemies[j].getLocation();
                if (uc.canSenseLocation(enemyLoc) && uc.canSenseLocation(target) && (uc.isObstructed(enemyLoc, target) || !uc.isAccessible(target))) continue;
                isEnemies = true;
                UnitInfo enemy = enemies[j];
                UnitType enemyType = enemy.getType();
                int distance = microInfo[i].loc.distanceSquared(enemy.getLocation());
                microInfo[i].updateSafe(distance, enemyType);
            }
        }

        int bestIndex = 8;

        for (int i = 8; i >= 0; i--) {
            if (!uc.canMove(myDirs[i])) continue;
            if (!microInfo[bestIndex].isBetter(microInfo[i])) bestIndex = i;
        }

        if (isEnemies) {
            microDir = myDirs[bestIndex];
        }
    }

    class MicroInfo {
        int numEnemies;
        int minDistToUnit;
        int minDistToBuilding;
        int illumination;
        Location loc;

        public MicroInfo(Location loc) {
            this.loc = loc;
            this.illumination = uc.senseIllumination(loc);
            numEnemies = 0;
            minDistToUnit = 100000;
            minDistToBuilding = 100000;
        }

        void updateSafe(int distance, UnitType enemyType) {
            if (enemyType == UnitType.WORKER) {
                if (distance < 14) numEnemies++;
                if (distance < minDistToUnit) minDistToUnit = distance;
            } else if (enemyType == UnitType.WOLF) {
                if (distance < 9) numEnemies++;
                if (distance < minDistToUnit) minDistToUnit = distance;
            } else if (enemyType == UnitType.SPEARMAN) {
                if (distance < 33)
                if (distance < minDistToUnit) minDistToUnit = distance;
            } else if (enemyType == UnitType.AXEMAN) {
                if (distance < 14) numEnemies++;
                if (distance < minDistToUnit) minDistToUnit = distance;
            } else if (enemyType == UnitType.TRAPPER) {
                if (distance < minDistToUnit) minDistToUnit = distance;
            } else if (enemyType == UnitType.EXPLORER) {
                if (distance < minDistToUnit) minDistToUnit = distance;
            } else if (enemyType == UnitType.BASE) {
                if (distance < 19) numEnemies += 10;
            } else {
                if (distance < minDistToBuilding) minDistToBuilding = distance;
            }
        }

        boolean canAttackUnit() {
            return UnitType.WOLF.attackRange >= minDistToUnit;
        }

        boolean canAttackBuilding() {
            return UnitType.WOLF.attackRange >= minDistToBuilding;
        }

        boolean isBetter(MicroInfo m) {
            if (numEnemies > 9 && m.numEnemies <= 9) return false;
            if (numEnemies <= 9 && m.numEnemies > 9) return true;
            if (canAttackUnit()) {
                if (!m.canAttackUnit()) return true;
                if (numEnemies < m.numEnemies) return true;
                if (numEnemies > m.numEnemies) return false;
                return illumination < m.illumination;
            }
            if (m.canAttackUnit()) return false;
            if (minDistToUnit == 100000 && m.minDistToUnit == 100000) {
                if (canAttackBuilding()) {
                    if (!m.canAttackBuilding()) return true;
                    return minDistToBuilding > m.minDistToBuilding;
                }
                if (m.canAttackBuilding()) return false;
                return minDistToBuilding < m.minDistToBuilding;
            } else return minDistToUnit < m.minDistToUnit;
        }
    }
}
