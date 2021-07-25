package viperplayer;

import aic2021.user.*;

public class Wolf extends MyUnit {

    Wolf(UnitController uc){
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
        if (enemyBase == null) {
            enemyBase = lookForEnemyBase();
            move.setEnemyBase(enemyBase);
        }

        microResult = doMicro();
        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        if (!microResult || round > 1600) {
            if (round > 1600) tryMove(true);
            else tryMove(false);
        } else {
            if (!uc.canMove()) return;
            uc.move(microDir);
        }
        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
    }

    void tryMove(boolean reckless) {
        if (enemyBase == null) move.explore();
        else move.moveTo(enemyBase, reckless);
    }

    public boolean doMicro() {
        Location myLoc = uc.getLocation();
        Direction[] dirs = Direction.values();
        MicroInfo[] microInfo = new MicroInfo[9];
        for (int i = 0; i < 9; i++) {
            Location target = myLoc.add(dirs[i]);
            microInfo[i] = new MicroInfo(target);
        }

        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());

        for (UnitInfo enemy : enemies) {
            for (int i = 0; i < 9; i++) {
                microInfo[i].update(enemy);
            }
        }

        if (enemies.length == 0) return false;

        int bestIndex = -1;

        for (int i = 8; i >= 0; i--) {
            if (!uc.canMove(dirs[i])) continue;
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
            UnitType type = unit.getType();
            if (distance <= type.attackRange && (type == UnitType.AXEMAN || type == UnitType.WOLF || type == UnitType.BASE)) {
                numEnemies++;
            }
            if (distance < minDistToEnemy) minDistToEnemy = distance;
        }

        boolean canAttack() {
            return uc.getType().getAttackRange() >= minDistToEnemy && minDistToEnemy >= uc.getType().getMinAttackRange();
        }

        boolean isBetter(MicroInfo m) {
            if (canAttack()) {
                if (!m.canAttack()) return true;
                return minDistToEnemy > m.minDistToEnemy;
            }
            if (m.canAttack()) return false;
            return minDistToEnemy <= m.minDistToEnemy;
        }
    }
}
