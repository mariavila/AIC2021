package viperplayer;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
    }

    String state = "INI";
    Location enemyBase = null;
    boolean returned = false;

    private Boolean microResult;
    private Direction microDir;

    Team myTeam = uc.getTeam();
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

    private Location lookForEnemyBase(){
        UnitInfo[] units = uc.senseUnits(myTeam.getOpponent());
        for(UnitInfo unit:units){
            if(unit.getType() == UnitType.BASE){
                move.setEnemyBase(unit.getLocation());
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

    void tryMove() {
        if (!microResult) {
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
        } else {
            if (!uc.canMove()) return;
            uc.move(microDir);
        }
    }

    boolean moveCircle(Direction dir) {
        return uc.canMove(dir) && uc.getLocation().add(dir).distanceSquared(baseLocation) <= (10 + uc.getRound() / 10);
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
        if (enemies.length == 1 && enemyBase.distanceSquared(myLoc) <= uc.getType().getVisionRange()) return false;

        int bestIndex = -1;

        for (int i = 8; i >= 0; i--) {
            if (!uc.canMove(dirs[i])) continue;
            if (bestIndex < 0 || !microInfo[bestIndex].isBetter(microInfo[i])) bestIndex = i;
        }

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
            int distance = unit.getLocation().distanceSquared(loc);
            if (distance <= unit.getType().attackRange) {
                ++numEnemies;
            }
            if (distance < minDistToEnemy) minDistToEnemy = distance;
        }

        boolean canAttack() {
            return uc.getType().getAttackRange()+10 >= minDistToEnemy && minDistToEnemy >= uc.getType().getMinAttackRange();
        }

        boolean isBetter(MicroInfo m) {
            if (numEnemies < m.numEnemies) return true;
            if (numEnemies > m.numEnemies) return false;
            if (canAttack()) {
                if (!m.canAttack()) return true;
                return minDistToEnemy >= m.minDistToEnemy;
            }
            if (m.canAttack()) return false;
            return minDistToEnemy >= m.minDistToEnemy;
        }
    }
}
