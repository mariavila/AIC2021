package viperplayer;

import aic2021.user.*;

public class Axeman extends MyUnit {

    AxemanPathfinder pathfinder;

    Axeman(UnitController uc){
        super(uc);

        this.pathfinder = new AxemanPathfinder(uc);
    }

    void playRound(){
        if(justSpawned){
            barracks = senseBarracks();
            pathfinder.setEnemyBase(enemyBase);
            justSpawned = false;
        }

        round = uc.getRound();
        if (enemyBase == null || uc.getLocation().distanceSquared(enemyBase) > 65) lightTorch();

        smokeSignals = tryReadSmoke();
        doSmokeStuff();

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
                uc.makeSmokeSignal(smoke.encode(constants.ENEMY_BASE, enemyBase));
                enemyBaseSend = true;
            }
        }

        Location loc;
        int type;

        for (Smoke.smokeSignal signal: smokeSignals) {
            if (signal == null) continue;
            loc = signal.getLoc();
            type = signal.getType();

            if (type == constants.ENEMY_BASE) {
                enemyBase = loc;
                if (enemyBase != null) {
                    needsToSend = false;
                    pathfinder.setEnemyBase(enemyBase);
                }
            }
        }
    }
}
