package extra6;

import aic2021.user.*;

public class Trapper extends MyUnit {

    Trapper(UnitController uc){
        super(uc);
    }

    String state = "INI";

    Team myTeam = uc.getTeam();
    Location baseLocation;
    UnitInfo myInfo;

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
        round = uc.getRound();
        lightTorch();

        if (state == "INI"){
            baseLocation = getBaseLocation();
            uc.lightTorch();
            myInfo = uc.getInfo();
            state = "PERIMETER";
        }
        // Explore for resources
        if (state == "PERIMETER") {
            if (uc.getLocation().distanceSquared(baseLocation) < 13) {
                move.moveTo(baseLocation.add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST), false, this::moveCircle);
            }

            if (uc.getLocation().distanceSquared(baseLocation) >= 12) {
                Location[] traps = uc.senseTraps(2);
                setTraps(traps);
            }
        }
    }

    void setTraps(Location[] traps) {
        Location myLoc = uc.getLocation();
        Location trapPos = myLoc.add(baseLocation.directionTo(myLoc));
        if (uc.canAttack()) {
            if (canSetTrap(trapPos, traps)) uc.attack(trapPos);
            else {
                trapPos = myLoc.add(baseLocation.directionTo(myLoc).rotateLeft());
                if (canSetTrap(trapPos, traps)) uc.attack(trapPos);
                else {
                    trapPos = myLoc.add(baseLocation.directionTo(myLoc).rotateRight());
                    if (canSetTrap(trapPos, traps)) uc.attack(trapPos);
                    else {
                        move.moveTo(baseLocation.add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST).add(Direction.EAST), false, this::moveCircle);
                    }
                }
            }
        }
    }

    boolean canSetTrap(Location trapPos, Location[] traps) {
        boolean trapPlaced = false;
        for(int i=0; i<traps.length; i++) {
            if (traps[i].isEqual(trapPos)) trapPlaced = true;
        }
        return uc.canAttack(trapPos) && trapPos.distanceSquared(baseLocation) > 18 && !trapPlaced;
    }

    boolean moveCircle(Direction dir) {
        return uc.canMove(dir) && uc.getLocation().add(dir).distanceSquared(baseLocation) <= 18;
    }

}
