package viperplayer;

import aic2021.user.*;

public class Spearman extends MyUnit {

    SpearmanPathfinder pathfinder;

    Spearman(UnitController uc){
        super(uc);

        this.pathfinder = new SpearmanPathfinder(uc);
    }

    void playRound(){
        if(justSpawned){
            barracks = senseBarracks();
            enemyBase = tryReadArt();
            pathfinder.setEnemyBase(enemyBase);
            justSpawned = false;
        }

        round = uc.getRound();
        if (enemyBase == null || uc.getLocation().distanceSquared(enemyBase) > 65) lightTorch();

        smokeSignals = tryReadSmoke();
        doSmokeStuffSoldier();

        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        if (round > 1600) tryMove(true);
        else tryMove(false);
        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
    }

    void tryMove(boolean reckless) {
        if (enemyBase == null) pathfinder.getNextLocationTarget(move.explore(), reckless);
        else pathfinder.getNextLocationTarget(enemyBase, reckless);
    }

    void doSmokeStuff() {
        if (enemyBase == null || needsToSend) {
            enemyBase = lookForEnemyBase();
            needsToSend = true;
            if (enemyBase != null && !enemyBaseSend && uc.canMakeSmokeSignal()) {
                uc.makeSmokeSignal(smoke.encodeEnemyBaseLoc(constants.ENEMY_BASE, enemyBase, barracks));
                enemyBaseSend = true;
            }
        }

        Location loc;
        int type;

        for (MyUnit.smokeSignal smoke: smokeSignals) {
            if (smoke == null) continue;
            loc = smoke.getLoc();
            type = smoke.getType();

            if (type == constants.ENEMY_BASE) {
                enemyBase = barracks.add(-loc.x, -loc.y);
                if (enemyBase != null) {
                    needsToSend = false;
                    pathfinder.setEnemyBase(enemyBase);
                }
            }
        }
    }
}
