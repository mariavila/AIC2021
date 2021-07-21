package viperplayer;

import aic2021.user.UnitController;

public class Axeman extends MyUnit {

    Axeman(UnitController uc){
        super(uc);
    }

    boolean rushAttack=false;

    private void readSmokeSignal(){
        return;
    }

    void playRound(){
        readSmokeSignal();
    }

}
