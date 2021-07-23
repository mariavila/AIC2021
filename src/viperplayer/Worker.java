package viperplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team myTeam = uc.getTeam();

    Location resourceLocation = null;
    int resourcesLeft = 0;

    boolean followingDeer = false;
    boolean boxesResearched = false;
    boolean barracksBuilt = false;
    boolean rushAttack = false;
    boolean justSpawned = true;

    ResourceInfo[] resources;
    UnitInfo[] deer;

    String state = "INI";

    void playRound(){
        round = uc.getRound();
        lightTorch();

        if(justSpawned){
            tryReadArt();
            justSpawned = false;
        }

        smokeSignals = tryReadSmoke();

        tryBarracks();
        tryJob();
        trySpawn();
        uc.println(state);
        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        if (state.equals("EXPLORE") || (state.equals("GOTORESOURCE") && followingDeer)) attack.genericTryAttack(uc.senseUnits(Team.NEUTRAL));
    }

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
            followingDeer = false;
            state = "GOTORESOURCE";
        } else if(deer.length > 0){
            followingDeer = true;
            state = "GOTORESOURCE";
        } else{
            move.explore();
        }
    }

    private void goToResource(){
        resources = uc.senseResources();
        if (resources.length > 0){
            resourceLocation = resources[0].getLocation();
            followingDeer = false;
        }
        if (followingDeer){
            deer = uc.senseUnits(Team.NEUTRAL);
            if (deer.length > 0){
                resourceLocation = deer[0].getLocation();
            }
            else{
                state = "EXPLORE";
                followingDeer = false;
            }
        }
        if (resourcesLeft < 100) {
            state = "EXPLORE";
        }
        move.moveTo(resourceLocation, false);
        if (!followingDeer && resourceLocation.isEqual(uc.getLocation())){
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

            if (boxesResearched) {
                if (total_res == GameConstants.MAX_RESOURCE_CAPACITY_BOXES){
                    state = "DEPOSIT";
                    resourcesLeft = resources[0].getAmount();
                }
            }
            else if (total_res >= GameConstants.MAX_RESOURCE_CAPACITY) {
                state = "DEPOSIT";
                resourcesLeft = resources[0].getAmount();
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

    private void tryBarracks(){
        if(!barracksBuilt){
            if(smokeSignals.length > 0) {
                Location loc;
                int type;

                for (smokeSignal smokeSignal : smokeSignals) {
                    loc = smokeSignal.getLoc();
                    type = smokeSignal.getType();

                    if (type == rushAttackEncoding) {
                        enemyBase = baseLocation.add(-loc.x, -loc.y);
                        if (enemyBase != null) {
                            move.setEnemyBase(enemyBase);
                            rushAttack = true;
                        }
                    }
                }
            }
        }
        if (rushAttack && !barracksBuilt) {
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

    private void trySpawn() {
        if (rushAttack &&!barracksBuilt) {
            if (uc.getRound() < 430) {
                spawnEmpty(UnitType.FARM);
                spawnEmpty(UnitType.SAWMILL);
                spawnEmpty(UnitType.QUARRY);
            }
        }
    }
}
