package viperplayer;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
    }

    String state = "INI";
    int turnsStopped = 0;
    Location lastLoc;

    Team myTeam = uc.getTeam();
    Location baseLocation;

    Location getBaseLocation(){
        UnitInfo[] units = uc.senseUnits(2, myTeam);
        for (UnitInfo unit:units){
            if (unit.getType()== UnitType.BASE){
                return unit.getLocation();
            }
        }
        return new Location(-1, -1);
    }

    void playRound(){
        //move.moveTo(enemyBaseLoc, false, this::moveCircle);
        if (state == "INI"){
            baseLocation = getBaseLocation();
            state = "EXPLORE";
        }
        // Explore for resources
        if (state == "EXPLORE") {
            move.explore(false);
            Location myNewLoc = uc.getLocation();
            if (myNewLoc.isEqual(lastLoc)) {
                turnsStopped++;
                if (turnsStopped > 4) state = "RETURN";
            } else {
                lastLoc = myNewLoc;
                turnsStopped = 0;
            }
        }
        // Explore for resources
        if (state == "RETURN") {
            move.moveTo(baseLocation, false);
            if (uc.getLocation().distanceSquared(baseLocation) < 2) {
                state = "EXPLORE";
            }
        }
    }

    boolean moveCircle(Direction dir) {
        return uc.canMove(dir) && uc.getLocation().add(dir).distanceSquared(baseLocation) <= (10 + uc.getRound() / 10);
    }

}
