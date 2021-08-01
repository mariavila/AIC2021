package eduardkhil;

import aic2021.user.Location;
import aic2021.user.UnitController;

public class Axeman extends MyUnit {

    AxemanPathfinder pathfinder;
    int roundAttack = 1600;

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
        senseEnemyBarracks();
        smokeSignals = tryReadSmoke();
        doSmokeStuff();

        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        if (round > roundAttack) tryMove(true);
        else tryMove(false);
        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
    }

    void tryMove(boolean reckless) {
        if (enemyBarracks != null) pathfinder.getNextLocationTarget(enemyBarracks, reckless);
        else if (enemyBase != null) pathfinder.getNextLocationTarget(enemyBase, reckless);
        else pathfinder.getNextLocationTarget(move.explore(), reckless);
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
            } else if (type == constants.ATTACK_BASE) {
                enemyBase = loc;
                roundAttack = round;
            } else if (type == constants.ENEMY_BARRACKS) {
                enemyBarracks = loc;
            }
        }
    }
}
