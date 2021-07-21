package viperplayer;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
    }

    String state = "INI";
    Location enemyBase;

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

    private Location lookForEnemyBase(){
        UnitInfo[] units = uc.senseUnits(myTeam.getOpponent());
        for(UnitInfo unit:units){
            if(unit.getType() == UnitType.BASE){
                return unit.getLocation();
            }
        }
        return null;
    }

    private void drawEnemyBaseLoc(){
        int drawing = encodeEnemyBaseLoc(false, enemyBase, baseLocation);
        if(uc.canDraw(drawing)){
            uc.draw(drawing);
        }
    }

    void playRound(){
        lightTorch();

        if (state == "INI"){
            baseLocation = getBaseLocation();
            state = "EXPLORE";
        }
        // Explore to find enemy base
        if (state == "EXPLORE") {
            move.explore();
            enemyBase = lookForEnemyBase();
            if (enemyBase != null){
                state = "BASEFOUND";
            }
        }
        if (state == "BASEFOUND") {
            if (enemyBase != null){
                if(uc.canMakeSmokeSignal()){
                    int drawing = encodeEnemyBaseLoc(true, enemyBase, baseLocation);
                    uc.makeSmokeSignal(drawing);
                    state = "RETURN";
                }
            }
        }
        if (state == "RETURN") {
            move.moveTo(baseLocation, false);
            if (uc.getLocation().distanceSquared(baseLocation) <= 2) {
                drawEnemyBaseLoc();
                state = "EXPLORE";
            }
        }
    }

    boolean moveCircle(Direction dir) {
        return uc.canMove(dir) && uc.getLocation().add(dir).distanceSquared(baseLocation) <= (10 + uc.getRound() / 10);
    }

}
