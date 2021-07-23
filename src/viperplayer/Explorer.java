package viperplayer;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
    }

    String state = "INI";
    Location enemyBase = null;
    boolean returned = false;

    Team myTeam = uc.getTeam();
    Location baseLocation = null;

    void playRound(){
        round = uc.getRound();
        lightTorch();

        tryJob();
    }

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

    void tryJob() {
        if (state == "INI"){
            baseLocation = getBaseLocation();
            state = "EXPLORE";
        }
        if (state == "EXPLORE") {
            move.explore();
            if (enemyBase == null) enemyBase = lookForEnemyBase();
            else if (!returned) state = "BASEFOUND";
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
                returned = true;
                state = "EXPLORE";
            }
        }
    }

    boolean moveCircle(Direction dir) {
        return uc.canMove(dir) && uc.getLocation().add(dir).distanceSquared(baseLocation) <= (10 + uc.getRound() / 10);
    }

}
