package viperplayer;

import aic2021.user.*;

public class Wolf extends MyUnit {

    WolfPathfinder pathfinder;
    int roundAttack = 1950;
    UnitInfo[] enemies;

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

        enemies = uc.senseUnits(uc.getTeam().getOpponent());
        attack.genericTryAttack(enemies);
        if (round > roundAttack) tryMove(enemies, true);
        else tryMove(enemies, false);
        enemies = uc.senseUnits(uc.getTeam().getOpponent());
        attack.genericTryAttack(enemies);
    }

    void tryMove(UnitInfo[] enemies, boolean reckless) {
        if (enemyBarracks != null) pathfinder.getNextLocationTarget(enemyBarracks, enemies, reckless);
        else if (enemyBase != null) pathfinder.getNextLocationTarget(enemyBase, enemies, reckless);
        else pathfinder.getNextLocationTarget(move.explore(), enemies, reckless);
    }
}