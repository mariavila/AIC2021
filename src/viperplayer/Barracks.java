package viperplayer;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Barracks extends MyUnit {

    Barracks(UnitController uc){
        super(uc);
    }


    private void trySpawn(){
        spawnEmpty(UnitType.AXEMAN);
    }


    void playRound(){
        trySpawn();
    }

}
