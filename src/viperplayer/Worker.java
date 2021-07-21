package viperplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team myTeam = uc.getTeam();

    Location baseLocation = null;
    Location resourceLocation = null;

    boolean followingDeer = false;
    boolean boxesResearched = false;
    boolean barracksBuilt = false;
    boolean rushAttack = false;
    boolean justSpawned = true;

    ResourceInfo[] resources;
    UnitInfo[] deer;
    Location enemyBase;

    String state = "INI";

    private Location getBaseLocation(){
        UnitInfo[] units = uc.senseUnits(2, myTeam);
        for (UnitInfo unit:units){
            if (unit.getType()==UnitType.BASE || unit.getType()==UnitType.SETTLEMENT){
                return unit.getLocation();
            }
        }
        return null;
    }

    private void explore(){
        resources = uc.senseResources();
        deer = uc.senseUnits(Team.NEUTRAL);

        if(resources.length > 0){
            resourceLocation = resources[0].getLocation();
            state = "GOTORESOURCE";
        } else if(deer.length > 0){
            followingDeer = true;
            state = "GOTORESOURCE";
        } else{
            move.explore();
        }
    }

    private void goToResource(){
        if (followingDeer){
            deer = uc.senseUnits(Team.NEUTRAL);
            if(deer.length > 0){
                resourceLocation = deer[0].getLocation();
            }
            else{
                state = "EXPLORE";
                followingDeer = false;
            }
        }
        move.moveTo(resourceLocation, false);
        if(!followingDeer && resourceLocation.isEqual(uc.getLocation())){
            state = "GATHER";
        }
    }

    void gather(){
        Location myLoc = uc.getLocation();
        resources = uc.senseResources();

        if (resources.length > 0 && resources[0].getLocation().isEqual(myLoc)) {
            if (uc.canGatherResources()){
                uc.gatherResources();
            }

            int[] gatheredResources = uc.getResourcesCarried();
            int total_res = 0;

            for (int res: gatheredResources) {
                total_res += res;
            }

            if (boxesResearched){
                if (total_res == GameConstants.MAX_RESOURCE_CAPACITY_BOXES){
                    state = "DEPOSIT";
                }
            }
            else if (total_res >= GameConstants.MAX_RESOURCE_CAPACITY) {
                state = "DEPOSIT";
            }
        } else {
            state = "EXPLORE";
        }
    }

    void deposit(){
        move.moveTo(baseLocation, false);
        if (uc.canDeposit()) {
            uc.deposit();
            state = "GOTORESOURCE";
        }
    }

    private void readArt(){
        //if(uc.canRead())
        return;
    }



    void playRound(){
        round = uc.getRound();
        lightTorch();

        if(justSpawned){
            readArt();
            justSpawned = false;
        }
        tryBarracks();
        tryJob();
        trySpawn();

        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        attack.genericTryAttack(uc.senseUnits(Team.NEUTRAL));
    }

    private void tryBarracks(){
        if(!barracksBuilt){
            int[] smokeSignals = uc.readSmokeSignals();
            if(smokeSignals.length>0) {
                for (int smokeSignal : smokeSignals) {
                    Location offset = decodeSignal(true, smokeSignal);
                    enemyBase = baseLocation.add(-offset.x, -offset.y);
                    if (enemyBase != null) {
                        rushAttack = true;
                    }
                }
            }
        }
        if (rushAttack && !barracksBuilt){
            barracksBuilt = spawnEmpty(UnitType.BARRACKS);
            if(barracksBuilt){
                int drawing = encodeEnemyBaseLoc(false, enemyBase, baseLocation);
                if(uc.canDraw(drawing)){
                    uc.draw(drawing);
                }
            }
        }
    }

    private void tryJob() {
        if (state.equals("INI")){
            baseLocation = getBaseLocation();
            state = "EXPLORE";
        }
        if (state.equals("EXPLORE")){
            explore();
        }
        if (state.equals("GOTORESOURCE")){
            goToResource();
        }
        if (state.equals("GATHER")){
            gather();
        }
        if (state.equals("DEPOSIT")){
            deposit();
        }
    }

    private void trySpawn(){
        if (rushAttack &&!barracksBuilt){
            if (uc.getRound() < 430) {
                spawnEmpty(UnitType.FARM);
                spawnEmpty(UnitType.SAWMILL);
                spawnEmpty(UnitType.QUARRY);
            }
        }
    }
}
