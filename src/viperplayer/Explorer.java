package viperplayer;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
    }

    String state = "INI";
    boolean returned = false;
    boolean enemyFound = false;

    private Boolean microResult;
    private Direction microDir;

    Location baseLocation = null;

    void playRound(){
        round = uc.getRound();
        lightTorch();

        microResult = doMicro();

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
                int drawing = encodeEnemyBaseLoc(constants.ENEMY_FOUND, units[0].getLocation(), baseLocation);
                uc.makeSmokeSignal(drawing);
                enemyFound = true;
            }
        }

        return null;
    }

    private void drawEnemyBaseLoc(){
        int drawing = encodeEnemyBaseLoc(1, enemyBase, baseLocation);
        if(uc.canDraw(drawing)){
            uc.draw(drawing);
        }
    }

    void tryMove() {
        if (!microResult) {
            if (state == "INI"){
                baseLocation = getBaseLocation();
                state = "EXPLORE";
            }
            if (state == "EXPLORE") {
                if (enemyBase == null) enemyBase = lookForEnemyBaseExplorer();
                else if (!returned) state = "BASEFOUND";
                move.explore();
            }
            if (state == "BASEFOUND") {
                if (enemyBase != null){
                    if(uc.canMakeSmokeSignal()){
                        int drawing = encodeEnemyBaseLoc(constants.RUSH_ATTACK_ENCODING, enemyBase, baseLocation);
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
        } else {
            if (!uc.canMove()) return;
            uc.move(microDir);
        }
    }

    public boolean doMicro() {
        Location myLoc = uc.getLocation();
        Direction[] dirs = Direction.values();
        MicroInfo[] microInfo = new MicroInfo[9];

        for (int i = 0; i < 9; i++) {
            Location target = myLoc.add(dirs[i]);
            microInfo[i] = new MicroInfo(target);
        }

        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());

        for (UnitInfo enemy : enemies) {
            if (!uc.isObstructed(enemy.getLocation(), myLoc)) {
                for (int i = 0; i < 9; i++) {
                    microInfo[i].update(enemy);
                }
            }
        }

        if (enemies.length == 0) return false;
        if (enemies.length == 1 && enemyBase != null && enemyBase.distanceSquared(myLoc) <= uc.getType().getVisionRange()) return false;

        int bestIndex = -1;

        for (int i = 0; i <= 8; i++) {
            if (!uc.canMove(dirs[i])) continue;
            if (bestIndex < 0) {
                bestIndex = i;
            }
            if (!microInfo[bestIndex].isBetter(microInfo[i])) {
                bestIndex = i;
            }
        }

        if (bestIndex == 0 && microInfo[8].numEnemies == 0) return false;

        if (bestIndex != -1) {
            microDir = (dirs[bestIndex]);
            return true;
        }

        return false;
    }

    class MicroInfo {
        int numEnemies;
        int minDistToEnemy;
        Location loc;

        public MicroInfo(Location loc) {
            this.loc = loc;
            numEnemies = 0;
            minDistToEnemy =  100000;
        }

        void update(UnitInfo unit) {
            Location target = unit.getLocation();
            int xdiff = target.x - loc.x;
            int ydiff = target.y - loc.y;
            if (xdiff < 0) xdiff++;
            else if (xdiff > 0) xdiff--;
            else xdiff = 1;
            if (ydiff < 0) ydiff++;
            else if (ydiff > 0) ydiff--;
            else ydiff = 1;

            int distance = xdiff*xdiff + ydiff*ydiff;
            int attackRange = unit.getType().attackRange;
            if (attackRange != 0 && distance <= attackRange) {
                ++numEnemies;
            }
            if (distance < minDistToEnemy) minDistToEnemy = distance;
        }

        boolean isBetter(MicroInfo m) {
            if (numEnemies > m.numEnemies) return false;
            if (numEnemies < m.numEnemies) return true;
            return true;
        }
    }
}
