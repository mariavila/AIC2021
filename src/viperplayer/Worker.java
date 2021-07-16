package viperplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team myTeam = uc.getTeam();
    Location baseLocation;
    Location resourceLocation = null;

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
            resourceLocation = resources[0].location;
            state = "GOTORESOURCE";
        }
        else{
            move.explore(false);
        }
    }

    void goToResource(){
        move.moveTo(resourceLocation, false);
        if(resourceLocation.distanceSquared(uc.getLocation())<=1){
            state = "GATHER";
        }
        else{
            move.moveTo(resourceLocation, false);
        }
    }

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
            state = "DEPOSIT";
        }
        // Return base and deposit resources
        if (state == "DEPOSIT"){
            state = "EXPLORE";
        }

        attack.genericTryAttack();
    }
}
