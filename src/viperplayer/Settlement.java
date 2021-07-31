package viperplayer;

import aic2021.user.*;

public class Settlement extends MyUnit {

    Location myLoc = null;
    int initialFood = 0;
    int initialWood = 0;
    int initialStone = 0;
    int workers = 0;
    boolean ecoMap = false;

    Settlement(UnitController uc){
        super(uc);
    }

    void playRound(){
        myLoc = uc.getLocation();
        if (justSpawned) {
            justSpawned = false;
            senseInitialResources();
            broadCast();
        }
        trySpawn();
    }

    void broadCast() {
        if (ecoMap) {
            if (round % 41 == 0 && uc.canMakeSmokeSignal()) {
                uc.makeSmokeSignal(smoke.encode(constants.ECO_MAP, myLoc));
            }
        }
    }

    private void trySpawn(){
        if (ecoMap && workers < 3) {
            spawnEmpty(UnitType.WORKER);
            workers++;
        }
    }

    private void senseInitialResources() {
        ResourceInfo[] initialResources = uc.senseResources();
        for(int i=0; i<initialResources.length; i++) {
            if (initialResources[i].resourceType == Resource.FOOD) {
                initialFood += initialResources[i].amount;
            } else if (initialResources[i].resourceType == Resource.WOOD) {
                initialWood += initialResources[i].amount;
            } else {
                initialStone += initialResources[i].amount;
            }
        }
        if (initialFood + initialWood + initialStone > 1200) {
            ecoMap = true;
        }
    }

}
