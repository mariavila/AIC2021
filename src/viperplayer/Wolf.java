package viperplayer;

import aic2021.user.*;

public class Wolf extends MyUnit {

    WolfPathfinder pathfinder;
    int roundAttack = 1950;

    Wolf(UnitController uc){
        super(uc);

        this.pathfinder = new WolfPathfinder(uc);
    }

    void playRound(){
        if(justSpawned){
            barracks = senseBarracks();
            move.init();
            justSpawned = false;
        }

        round = uc.getRound();
        if (enemyBase == null) {
            enemyBase = lookForEnemyBase();
            pathfinder.setEnemyBase(enemyBase);
            move.setEnemyBase(enemyBase);
        }

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
}