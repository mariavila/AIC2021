package extra5;

import aic2021.user.*;


public class SpearmanPathfinder {
    final int NO_RESET = 0;
    final int SOFT_RESET = 1;
    final int HARD_RESET = 2;

    final double maxCos = 0.5;

    Location obstacle = null;
    Location target = null;
    boolean left = true;
    UnitController uc;
    boolean dodging = false;

    Location minLocation;
    int minDist = 0;

    Location enemyBase = null;

    int[] cont;
    int[] mindist;
    int[] dmg;

    int bestIndex;

    Location closestRanger = null;

    double myDPS;
    int DPS;
    UnitType type;

    Direction[] directions = {
            Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST,
            Direction.SOUTH, Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST, Direction.ZERO
    };

    public SpearmanPathfinder(UnitController _uc){
        uc = _uc;
        type = uc.getType();
        DPS = dps(type);
        if (uc.getRandomDouble() > 0.5) left = false;
    }

    void reset(){
        obstacle = null;
        if (target != null){
            minDist = uc.getLocation().distanceSquared(target);
            minLocation = uc.getLocation();
        }
        dodging = false;
    }

    public void setEnemyBase(Location target) {
        enemyBase = target;
    }

    void soft_reset(){
        if (target != null){
            if (minLocation != null)  minDist = minLocation.distanceSquared(target);
            else minDist = uc.getLocation().distanceSquared(target);
        }
    }

    double cosSquared(Location loc1, Location loc2, Location loc3){
        int x1 = loc2.x - loc1.x;
        int y1 = loc2.y - loc1.y;
        int x2 = loc3.x - loc1.x;
        int y2 = loc3.y - loc1.y;

        int prod = (x1*x2 + y1*y2);

        if (prod < 0) return -1;
        if (prod == 0) return 0;

        return ((double)prod*prod)/((x1*x1 + y1*y1)*(x2*x2 + y2*y2));
    }

    int resetType(Location newTarget){
        if (target == null) return HARD_RESET;
        if (target.isEqual(newTarget)) return NO_RESET;
        if (target.distanceSquared(newTarget) <= 8) return SOFT_RESET;
        if (cosSquared(uc.getLocation(), target, newTarget) < maxCos*maxCos) return SOFT_RESET;
        return HARD_RESET;
    }

    boolean moveTo(Location _target){
        if (_target == null) return false;
        int a = resetType(_target);
        if (a == SOFT_RESET){
            target = _target;
            soft_reset();
        } else if (a == HARD_RESET){
            target = _target;
            reset();
        }

        if (target != null && uc.getLocation().distanceSquared(target) < minDist){
            reset();
        }
        return bugPath();
    }

    int getIndex(Direction dir){
        return dir.ordinal();
    }

    boolean isSafe(Direction dir){
        int a = getIndex(dir);
        return !isBetter(a, bestIndex);
    }

    boolean bugPath(){
        Location myLoc = uc.getLocation();
        if (target == null) return false;

        if (enemyBase != null && (myLoc.add(myLoc.directionTo(target)).distanceSquared(enemyBase) <= UnitType.BASE.attackRange && (!uc.canSenseLocation(enemyBase) || !uc.isObstructed(enemyBase, myLoc.add(myLoc.directionTo(target)))))) return false;

        if (obstacle == null) {
            if (greedyMove()) return true;
        }

        Direction dir;
        if (obstacle == null || myLoc.distanceSquared(obstacle) == 0) dir = myLoc.directionTo(target);
        else dir = myLoc.directionTo(obstacle);
        if (!uc.canMove(dir) || !uc.isAccessible(myLoc.add(dir))) {
            dodging = true;
            int c = 0;
            if (obstacle != null && myLoc.distanceSquared(obstacle) > 2){
                int d = myLoc.distanceSquared(obstacle);
                Direction bestDir = Direction.ZERO;
                for (int i = 0; i < 8; ++i){
                    Location newLoc = myLoc.add(dir);
                    int d2 = newLoc.distanceSquared(obstacle);
                    if (uc.canMove(dir) && d2 < d){
                        d = d2;
                        bestDir = dir;
                    }
                    if (left) dir = dir.rotateLeft();
                    else dir = dir.rotateRight();
                }
                if (bestDir != Direction.ZERO) {
                    dir = bestDir;
                    c = 20;
                }
            }
            boolean unitFound = false;
            while (!uc.canMove(dir) && c++ < 20) {
                if (uc.isOutOfMap(myLoc.add(dir))) left = !left;
                Location newLoc = myLoc.add(dir);
                if (uc.senseUnitAtLocation(newLoc) != null) unitFound = true;
                if (!unitFound) obstacle = newLoc;
                if (left) dir = dir.rotateLeft();
                else dir = dir.rotateRight();
            }
        }
        if (dir != Direction.ZERO && uc.canMove(dir) && isSafe(dir)){
            uc.move(dir);
            return true;
        }
        return false;
    }

    boolean greedyMove(){
        Location myLoc = uc.getLocation();
        Direction dir = myLoc.directionTo(target);

        if (uc.canMove(dir) && isSafe(dir)){
            uc.move(dir);
            return true;
        }

        int dist = uc.getLocation().distanceSquared(target);
        Direction dirR = dir.rotateRight(), dirL = dir.rotateLeft();
        Location newLocR = myLoc.add(dirR);
        Location newLocL = myLoc.add(dirL);
        int distR = newLocR.distanceSquared(target), distL = newLocL.distanceSquared(target);
        if (distR < distL){
            if (distR < dist && uc.canMove(dirR) && isSafe(dirR)){
                uc.move(dirR);
                return true;
            }
            if (distL < dist && uc.canMove(dirL) && isSafe(dirL)){
                uc.move(dirL);
                return true;
            }
        }
        if (distL < dist && uc.canMove(dirL) && isSafe(dirL)){
            uc.move(dirL);
            return true;
        }
        if (distR < dist && uc.canMove(dirR) && isSafe(dirR)){
            uc.move(dirR);
            return true;
        }
        return false;
    }

    void safeMove(){
        if (bestIndex != 8){
            uc.move(directions[bestIndex]);
            reset();
        }
    }

    int dps (UnitType type){
        if (type == UnitType.AXEMAN) return 15;
        if (type == UnitType.WOLF) return 8;
        if (type == UnitType.SPEARMAN) return 10;
        if (type == UnitType.WORKER) return 4;
        if (type == UnitType.BASE) return 160;
        return 0;
    }

    boolean fightMove(){
        Location loc = uc.getLocation();

        closestRanger = null;
        UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
        boolean closecombat = false;
        boolean bee = false;
        cont = new int[9];
        mindist = new int[9];
        dmg = new int[9];
        for (int i = 0; i < 9; ++i) mindist[i] = 1000;

        for (int i = 0; i < units.length && uc.getEnergyUsed() < 12000; ++i) {
            UnitType type = units[i].getType();
            Location enemyLoc = units[i].getLocation();
            if (type.minAttackRange > 0 && (closestRanger == null || loc.distanceSquared(closestRanger) > loc.distanceSquared(enemyLoc))) closestRanger = enemyLoc;
            int dps = dps(type);
            if (dps == 0) continue;
            if (closecombat && (type == UnitType.SPEARMAN)) continue;
            if (closecombat && type != UnitType.SPEARMAN && enemyLoc.distanceSquared(uc.getLocation()) > 13) continue;
            boolean ignoreDist = false;
            if (closecombat && enemyLoc.distanceSquared(uc.getLocation()) > 20) ignoreDist = true;
            int ars = type.attackRange;
            int arsmin = type.getMinAttackRange();
            boolean ignoreDmg = (bee && units[i].getAttackCooldown() >= 2);
            for (int j = 0; j < 9; ++j){
                Location newLoc = loc.add(directions[j]);
                dmg[j] = Math.max(dmg[j], damage(loc, enemyLoc));
                if (uc.isObstructed(newLoc, enemyLoc)) continue;
                int d = newLoc.distanceSquared(enemyLoc);
                if (!ignoreDist && mindist[j] > d) mindist[j] = d;
                if (d <= ars && d < arsmin && !ignoreDmg){
                    cont[j]+= dps;
                }
            }
        }

        bestIndex = 8;
        for (int i = 7; i >= 0; --i){
            if (enemyBase != null && loc.add(directions[i]).distanceSquared(enemyBase) <= UnitType.BASE.attackRange) continue;
            if (uc.canMove(directions[i]) && isBetter(bestIndex, i)){
                bestIndex = i;
            }
        }

        return true;
    }

    int damage(Location myLoc, Location enemyLoc){
        if(!uc.canAttack()) return 0;
        int d = myLoc.distanceSquared(enemyLoc);
        if (d > type.attackRange || d < type.minAttackRange) return 0;
        if (uc.isObstructed(myLoc, enemyLoc)) return 0;
        return type.attack;
    }

    boolean isBetter(int j, int i){
        int prevDPS = cont[j], dps = cont[i];
        int prevDist = mindist[j], dist = mindist[i];
        double myPrevDPS = dmg[j], myDPS = dmg[i];
        if (prevDPS <= DPS && dps > DPS) return false;
        if (prevDPS > DPS && dps <= DPS) return true;

        int ars = uc.getType().attackRange;
        if (!uc.canAttack()) ars = 100;
        if (dist <= ars){
            if (prevDist > ars) return true;
            if (dps-myDPS < prevDPS-myPrevDPS) return true;
            if (dps-myDPS > prevDPS-myPrevDPS) return false;
            return (dist > prevDist);
        }
        if (prevDist <= ars) return false;
        if (dps-myDPS < prevDPS-myPrevDPS) return true;
        if (dps-myDPS > prevDPS-myPrevDPS) return false;
        return (dist < prevDist);
    }
}
