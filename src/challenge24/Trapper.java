package challenge24;

import aic2021.user.*;

public class Trapper extends MyUnit {

    Trapper(UnitController uc){
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
            state = "PERIMETER";
        }
        // Explore for resources
        if (state == "PERIMETER") {
            move.moveTo(baseLocation.add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST), false, this::moveCircle);
            Location myNewLoc = uc.getLocation();
            if (uc.getLocation().distanceSquared(baseLocation) > 18) {
                Location trapPos = myNewLoc.add(baseLocation.directionTo(myNewLoc));
                if(uc.canAttack(trapPos)) uc.attack(trapPos);
            }
        }
    }

    boolean moveCircle(Direction dir) {
        return uc.canMove(dir) && uc.getLocation().add(dir).distanceSquared(baseLocation) <= (10 + uc.getRound() / 10);
    }
}
