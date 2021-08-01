package eduardkhil;

import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Explorer extends MyUnit {

    ExplorerPathfinder pathfinder;

    String state = "INI";
    boolean returned = false;
    boolean enemyFound = false;
    boolean hasWater = false;

    Location baseLocation = null;

    Explorer(UnitController uc){
        super(uc);

        this.pathfinder = new ExplorerPathfinder(uc);
    }

    void playRound(){
        round = uc.getRound();
        lightTorch();
        senseEnemyBarracks();
        if (!hasWater) senseWater();
        tryMove();
    }

    void senseWater() {
        Location[] waterTiles = uc.senseWater(uc.getType().getVisionRange());
        if (waterTiles.length > 15 && uc.canMakeSmokeSignal()) {
            hasWater = true;
            int drawing = smoke.encode(constants.ENEMY_FOUND, uc.getLocation());
            uc.makeSmokeSignal(drawing);
        }
    }

    Location getBaseLocation(){
        UnitInfo[] units = uc.senseUnits(2, myTeam);
        for (UnitInfo unit:units){
            if (unit.getType()== UnitType.BASE){
                return unit.getLocation();
            }
        }
        return null;
    }

    private Location lookForEnemyBaseExplorer(){
        UnitInfo[] units = uc.senseUnits(myTeam.getOpponent());
        for(UnitInfo unit:units){
            if(unit.getType() == UnitType.BASE){
                pathfinder.setEnemyBase(unit.getLocation());
                return unit.getLocation();
            }
        }

        if (enemyBase == null && !enemyFound && units.length > 0) {
            if(uc.canMakeSmokeSignal()) {
                int drawing = smoke.encode(constants.ENEMY_FOUND, units[0].getLocation());
                uc.makeSmokeSignal(drawing);
                enemyFound = true;
            }
        }

        return null;
    }

    private void drawEnemyBaseLoc(){
        int drawing = smoke.encode(1, enemyBase);
        if(uc.canDraw(drawing)){
            uc.draw(drawing);
        }
    }

    void tryMove() {
        if (state == "INI"){
            baseLocation = getBaseLocation();
            state = "EXPLORE";
        }
        if (state == "EXPLORE") {
            if (enemyBase == null) enemyBase = lookForEnemyBaseExplorer();
            else if (!returned) state = "BASEFOUND";
            pathfinder.getNextLocationTarget(move.explore());
        }
        if (state == "BASEFOUND") {
            if (enemyBase != null){
                if(uc.canMakeSmokeSignal()){
                    int drawing = smoke.encode(constants.RUSH_ATTACK_ENCODING, enemyBase);
                    uc.makeSmokeSignal(drawing);
                    state = "RETURN";
                }
            }
        }
        if (state == "RETURN") {
            pathfinder.getNextLocationTarget(baseLocation);
            if (uc.getLocation().distanceSquared(baseLocation) <= 2) {
                drawEnemyBaseLoc();
                returned = true;
                state = "EXPLORE";
            }
        }
    }
}
