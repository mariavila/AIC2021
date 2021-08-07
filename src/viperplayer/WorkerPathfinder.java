package viperplayer;

import aic2021.user.*;

public class WorkerPathfinder {

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

    WorkerPathfinder(UnitController uc){
        this.myDirs = Direction.values();
        this.uc = uc;
        this.myTeam = uc.getTeam();
        this.baseRange = UnitType.BASE.getAttackRange();
    }

    void setEnemyBase(Location target) {
        enemyBase = target;
    }

    Boolean getNextLocationTarget(Location target){
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
        if (uc.canMove(dir) && !uc.hasTrap(lastObstacleFound)) resetPathfinding();

        //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
        //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
        doMicro();

        for (int i = 0; i < 16; ++i){
            for (int j = 0; j < myDirs.length; j++) {
                if (myDirs[j] == dir) {
                    Location loc = myLoc.add(dir);
                    if (uc.canMove(dir) && !uc.hasTrap(loc) && (!isEnemies && (enemyBase == null || (loc.distanceSquared(enemyBase) > baseRange) || (uc.canSenseLocation(enemyBase) && uc.isObstructed(loc, enemyBase))))) {
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
                Location loc = myLoc.add(dir);
                if (uc.canMove(dir) && !uc.hasTrap(loc) && (!isEnemies && (enemyBase == null || (loc.distanceSquared(enemyBase) > baseRange) || (uc.canSenseLocation(enemyBase) && uc.isObstructed(loc, enemyBase))))) {
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

    public void doMicro() {
        enemies = uc.senseUnits(myTeam.getOpponent());
        traps = uc.senseTraps();
        isEnemies = false;
        int length = enemies.length;
        for (int i = 0; i < 9; i++) {
            if (!uc.canMove(myDirs[i])) continue;
            Location target = myLoc.add(myDirs[i]);
            microInfo[i] = new MicroInfo(myLoc.add(myDirs[i]));

            if (enemyBase != null && target.distanceSquared(enemyBase) <= baseRange) {
                if (uc.canSenseLocation(enemyBase) && !uc.isObstructed(target, enemyBase) || !uc.canSenseLocation(enemyBase)) microInfo[i].damage += 160;
            }

            for(Location trap: traps) {
                if(trap.isEqual(target)) {
                    microInfo[i].damage = 1000;
                    break;
                }
            }

            for (int j = 0; j < length; j++) {
                Location enemyLoc = enemies[j].getLocation();
                if (uc.canSenseLocation(enemyLoc) && uc.canSenseLocation(target) && (uc.isObstructed(enemyLoc, target) || !uc.isAccessible(target))) continue;
                UnitType type = enemies[j].getType();
                if (type != UnitType.EXPLORER && type != UnitType.BASE) isEnemies = true;
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
        int damage;
        int softdamage;
        int minDistToEnemy;
        Location loc;

        public MicroInfo(Location loc) {
            this.loc = loc;
            damage = 0;
            softdamage = 0;
            minDistToEnemy = 100000;
        }

        void updateSafe(int distance, UnitType enemyType) {
            if (enemyType == UnitType.WOLF) {
                if (distance <= UnitType.WOLF.attackRange) damage += 8;
                else if (distance < 9) softdamage += 8;
            } else if (enemyType == UnitType.SPEARMAN) {
                if (distance <= UnitType.SPEARMAN.attackRange && distance >= UnitType.SPEARMAN.minAttackRange) damage += 10;
                else if (distance < 33 && distance >= 5) softdamage += 10;
            } else if (enemyType == UnitType.AXEMAN) {
                if (distance <= UnitType.AXEMAN.attackRange) damage += 15;
                else if (distance < 14) softdamage += 15;
            } else if (enemyType == UnitType.BASE) {
                if (distance <= UnitType.BASE.attackRange) damage += 160;
            } else if (enemyType == UnitType.WORKER) {
                if (distance <= UnitType.WORKER.attackRange) damage += 8;
                else if (distance < 14) softdamage += 8;
            }

            if (distance < minDistToEnemy) minDistToEnemy = distance;
        }

        boolean canAttack() {
            return UnitType.WORKER.attackRange >= minDistToEnemy;
        }

        boolean isBetter(MicroInfo m) {
            if (uc.canAttack()) {
                if (canAttack() && damage <= 8) {
                    if (!m.canAttack()) return true;
                    return minDistToEnemy >= m.minDistToEnemy;
                }
                if (m.canAttack() && m.damage <= 8) return false;
            }
            if (damage > m.damage) return false;
            if (damage < m.damage) return true;
            return softdamage <= m.softdamage;
        }
    }
}
