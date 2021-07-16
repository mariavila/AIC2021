package viperplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team myTeam = uc.getTeam();
    Location baseLocation;
    Location resourceLocation = null;
    boolean followingDeer = false;
    boolean boxesResearched = false;

    String state = "INI";

    Location getBaseLocation(){
        UnitInfo[] units = uc.senseUnits(2, myTeam);
        for (UnitInfo unit:units){
            if (unit.getType()==UnitType.BASE || unit.getType()==UnitType.SETTLEMENT){
                return unit.getLocation();
            }
        }
        return null;
    }

    void explore(){
        ResourceInfo[] resources = uc.senseResources();

        if(resources.length > 0){
            if(resources[0].resourceType == Resource.FOOD){
                followingDeer = true;
            }
            resourceLocation = resources[0].location;
            state = "GOTORESOURCE";
        }
        else{
            move.explore(false);
        }
    }

    void goToResource(){
        if (followingDeer){
            ResourceInfo[] resources = uc.senseResources();
            if(resources.length > 0){
                if(resources[0].resourceType != Resource.FOOD){
                    followingDeer = false;
                }
                resourceLocation = resources[0].location;
                state = "GOTORESOURCE";
            }
            else{
                state = "EXPLORE";
                explore();
            }
        }
        move.moveTo(resourceLocation, false);
        if(resourceLocation.distanceSquared(uc.getLocation())<=1){ //check que no sigui food
            state = "GATHER";
        }
    }
/*
    void gather(){
        if (uc.canGatherResources()){
            uc.gatherResources();
        }
        if (boxesResearched){
            int gatheredResource = uc.getResourcesCarried(); // max of list
            if (gatheredResource == GameConstants.MAX_RESOURCE_CAPACITY_BOXES){
                state = "DEPOSIT";
            }
        }
        else{
            if (uc.getResourcesCarried() >= GameConstants.MAX_RESOURCE_CAPACITY) //TODO
                state = "DEPOSIT";
        }

    }
    */

    void playRound(){
        UnitInfo myInfo = uc.getInfo();

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
            //gather();
        }
        // Return base and deposit resources
        if (state == "DEPOSIT"){
            state = "EXPLORE";
        }

        attack.genericTryAttack();
    }
}
