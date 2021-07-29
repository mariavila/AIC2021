package viperplayer;

import aic2021.user.*;

public class Axeman extends MyUnit {

    Axeman(UnitController uc){
        super(uc);
    }

    private Boolean microResult;
    private Direction microDir;

    void playRound(){
        if(justSpawned){
            barracks = senseBarracks();
            enemyBase = tryReadArt();
            move.setEnemyBase(enemyBase);
            justSpawned = false;
        }

        round = uc.getRound();
        if (enemyBase == null || uc.getLocation().distanceSquared(enemyBase) > 40) lightTorch();

        smokeSignals = tryReadSmoke();
        doSmokeStuffSoldier();

        microResult = doMicro();
        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        if (!microResult || round > 1600) {
            if (round > 1600) tryMove(true);
            else tryMove(false);
        } else {
            if (!uc.canMove()) return;
            if (move.isSafe(microDir)) uc.move(microDir);
            else move.moveTo(move.explore(), false);
        }
        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
    }

    void tryMove(boolean reckless) {
        if (enemyBase == null) move.moveTo(move.explore(), false);
        else move.moveTo(enemyBase, reckless);
    }

    public boolean doMicro() {
        Location myLoc = uc.getLocation();
        Direction[] dirs = Direction.values();
        MicroInfo[] microInfo = new MicroInfo[9];
        for (int i = 0; i < 9; i++) {
            Location target = myLoc.add(dirs[i]);
            microInfo[i] = new MicroInfo(target);
            if (enemyBase != null && enemyBase.distanceSquared(target) <= UnitType.BASE.getAttackRange()) {
                microInfo[i].numEnemies = 10;
            }
        }

        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());

        for (UnitInfo enemy : enemies) {
            if (!uc.isObstructed(enemy.getLocation(), myLoc)) {
                for (int i = 0; i < 9; i++) {
                    microInfo[i].update(enemy);
                }
            }
        }

        if (enemies.length == 0) return false;

        int bestIndex = -1;
        int baseRange = UnitType.BASE.getAttackRange();

        for (int i = 8; i >= 0; i--) {
            if (!uc.canMove(dirs[i])) continue;
            if (enemyBase != null && enemyBase.distanceSquared(myLoc.add(dirs[i])) <= baseRange) {
                microInfo[i].numEnemies += 10;
            }
            if (bestIndex < 0 || !microInfo[bestIndex].isBetter(microInfo[i])) bestIndex = i;
        }

        if (bestIndex != -1) {
            microDir = (dirs[bestIndex]);
            return true;
        }

        return false;
    }

    class MicroInfo {
        int numEnemies;
        int minDistToEnemy;
        Location loc;

        public MicroInfo(Location loc) {
            this.loc = loc;
            numEnemies = 0;
            minDistToEnemy = 100000;
        }

        void update(UnitInfo unit) {
            int distance = unit.getLocation().distanceSquared(loc);
            if (distance <= unit.getType().attackRange) {
                ++numEnemies;
            }
            if (distance < minDistToEnemy) minDistToEnemy = distance;
        }

        boolean canAttack() {
            return uc.getType().getAttackRange() >= minDistToEnemy && minDistToEnemy >= uc.getType().getMinAttackRange();
        }

        boolean isBetter(MicroInfo m) {
            if (numEnemies > 9 || m.numEnemies > 9) return numEnemies < m.numEnemies;
            if (canAttack()) {
                if (!m.canAttack()) return true;
                if (numEnemies < m.numEnemies) return true;
                return minDistToEnemy <= m.minDistToEnemy;
            }
            if (m.canAttack()) return false;
            return minDistToEnemy <= m.minDistToEnemy;
        }
    }
}
