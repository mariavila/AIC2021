package extra3;

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
        UnitInfo[] units = uc.senseUnits(myTeam);
        UnitInfo[] enemies = uc.senseUnits(myTeam.getOpponent());
        int soldiers = 0;
        int enemySoldiers = 0;

        for (UnitInfo unit: units) {
            UnitType myType = unit.getType();
            if (myType == UnitType.AXEMAN || myType == UnitType.SPEARMAN  || myType == UnitType.WOLF) soldiers++;
        }

        for (UnitInfo enemy: enemies) {
            UnitType myType = enemy.getType();
            if (!uc.isObstructed(myLoc, enemy.getLocation()) && (myType == UnitType.AXEMAN || myType == UnitType.SPEARMAN || myType == UnitType.WOLF || myType == UnitType.WORKER)) enemySoldiers++;
        }

        if (soldiers < enemySoldiers) {
            spawnSafe(UnitType.WOLF);
        }

        Location workerSpawn;
        if ((workers < 1 || (ecoMap && workers < 3)) && round < 1700) {
            workerSpawn = spawnEmpty(UnitType.WORKER);
            if(workerSpawn != null) workers++;
        }
    }

    Location spawnSafe(UnitType t) {
        Location[] traps = uc.senseTraps(2);

        outerloop:
        for (Direction dir : dirs){
            if (dir == Direction.ZERO) continue;
            if (!uc.canSpawn(t, dir)) continue;

            Location target = myLoc.add(dir);
            for (Location trap: traps) {
                if (target.isEqual(trap)) continue outerloop;
            }

            if (enemyBase == null || target.distanceSquared(enemyBase) > UnitType.BASE.getAttackRange()) {
                uc.spawn(t, dir);
                return myLoc.add(dir);
            }
        }
        return null;
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
