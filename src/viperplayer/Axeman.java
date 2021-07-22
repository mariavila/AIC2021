package viperplayer;

import aic2021.user.UnitController;

public class Axeman extends MyUnit {

    Axeman(UnitController uc){
        super(uc);
    }

    void playRound(){
        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        smokeSignals = tryReadSmoke();
    }
}
