package viperplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team myTeam = uc.getTeam();
    Location baseLocation;

    String state = "INI";

    Location getBaseLocation(){
        UnitInfo[] units = uc.senseUnits(2, myTeam);
        for (UnitInfo unit:units){
            if (unit.getType()==UnitType.BASE){
                return unit.getLocation();
            }
        }
        return new Location(-1, -1);
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
            state = "GATHER";

        }
        // Gather
        if (state == "GATHER"){
            state = "DEPOSIT";
        }
        // Return base and deposit resources
        if (state == "DEPOSIT"){
            state = "EXPLORE";
        }
    }
}
