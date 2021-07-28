package viperplayer;

import aic2021.user.*;

public class Explorer extends MyUnit {

    ExplorerPathfinder pathfinder;

    String state = "INI";
    boolean returned = false;
    boolean enemyFound = false;

    private Boolean microResult;
    private Direction microDir;

    Location baseLocation = null;

    Explorer(UnitController uc){
        super(uc);

        this.pathfinder = new ExplorerPathfinder(uc);
    }

    void playRound(){
        round = uc.getRound();
        lightTorch();

        tryMove();
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

    private Location lookForEnemyBaseExplorer(){
        UnitInfo[] units = uc.senseUnits(myTeam.getOpponent());
        for(UnitInfo unit:units){
            if(unit.getType() == UnitType.BASE){
                move.setEnemyBase(unit.getLocation());
                return unit.getLocation();
            }
        }

        if (enemyBase == null && !enemyFound && units.length > 0) {
            if(uc.canMakeSmokeSignal()) {
                int drawing = smoke.encodeEnemyBaseLoc(constants.ENEMY_FOUND, units[0].getLocation(), baseLocation);
                uc.makeSmokeSignal(drawing);
                enemyFound = true;
            }
        }

        return null;
    }

    private void drawEnemyBaseLoc(){
        int drawing = smoke.encodeEnemyBaseLoc(1, enemyBase, baseLocation);
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
            if(uc.canMove()) pathfinder.getNextLocationTarget(move.explore());
        }
        if (state == "BASEFOUND") {
            if (enemyBase != null){
                if(uc.canMakeSmokeSignal()){
                    int drawing = smoke.encodeEnemyBaseLoc(constants.RUSH_ATTACK_ENCODING, enemyBase, baseLocation);
                    uc.makeSmokeSignal(drawing);
                    state = "RETURN";
                }
            }
        }
        if (state == "RETURN") {
            if(uc.canMove()) pathfinder.getNextLocationTarget(baseLocation);
            if (uc.getLocation().distanceSquared(baseLocation) <= 2) {
                drawEnemyBaseLoc();
                returned = true;
                state = "EXPLORE";
            }
        }
    }
}
