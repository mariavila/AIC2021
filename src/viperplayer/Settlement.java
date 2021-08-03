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
        round = uc.getRound();
        broadCast(justSpawned);
        if (justSpawned) {
            senseInitialResources();
            justSpawned = false;
        }
        trySpawn();
    }

    void broadCast(boolean justSpawned) {
        if (ecoMap) {
            if ((round % 41 == 0 || justSpawned) && uc.canMakeSmokeSignal()) {
                uc.makeSmokeSignal(smoke.encode(constants.ECO_MAP, myLoc));
            }
        } else {
            if ((round % 41 == 0 || justSpawned) && uc.canMakeSmokeSignal()) {
                uc.makeSmokeSignal(smoke.encode(constants.SETTLEMENT, myLoc));
            }
        }
    }

    private void trySpawn(){
        Location workerSpawn;
        if (workers < 1 || ecoMap && workers < 3) {
            workerSpawn = spawnEmpty(UnitType.WORKER);
            if(workerSpawn!=null) workers++;
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
