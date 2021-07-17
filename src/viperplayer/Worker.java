package viperplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team myTeam = uc.getTeam();
    Team[] teams = Team.values();

    Location baseLocation;
    Location resourceLocation = null;

    boolean followingDeer = false;
    boolean boxesResearched = false;

    ResourceInfo[] resources;
    UnitInfo[] deer;

    int torchTurn = 0;
    int round = 0;

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
        ResourceInfo[] resources = uc.senseResources();
        UnitInfo[] deer = uc.senseUnits(teams[2]);

        if(resources.length > 0){
            resourceLocation = resources[0].getLocation();
            state = "GOTORESOURCE";
        } else if(deer.length > 0){
            followingDeer = true;
            state = "GOTORESOURCE";
        } else{
            move.explore(false);
        }
    }

    private void goToResource(){
        ResourceInfo[] resources = uc.senseResources();
        UnitInfo[] deer = uc.senseUnits(teams[2]);

        if (followingDeer){
            if(deer.length > 0){
                resourceLocation = deer[0].getLocation();
            }
            else{
                state = "EXPLORE";
                followingDeer = false;
            }
        }
        move.moveTo(resourceLocation, false);
        if(!followingDeer && resourceLocation.distanceSquared(uc.getLocation())<=2){
            state = "GATHER";
        }
    }

    void gather(){
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
    }

    void playRound(){
        round = uc.getRound();

        if (torchTurn < round && uc.canLightTorch()) {
            torchTurn = round + GameConstants.TORCH_DURATION;
            uc.lightTorch();
        }

        // Get base location
        if (state == "INI"){
            baseLocation = getBaseLocation();
            state = "EXPLORE";
        }
        // Explore for resources
        if (state == "EXPLORE"){
            explore();
        }
        //Go to resource
        if (state == "GOTORESOURCE"){
            goToResource();
        }
        // Gather
        if (state == "GATHER"){
            gather();
        }
        // Return base and deposit resources
        if (state == "DEPOSIT"){
            state = "EXPLORE";
        }

        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        trySpawn();
    }

    private void trySpawn(){
        if (uc.getRound() < 430) {
            spawnRandom(UnitType.FARM);
            spawnRandom(UnitType.SAWMILL);
            spawnRandom(UnitType.QUARRY);
        }
    }
}
