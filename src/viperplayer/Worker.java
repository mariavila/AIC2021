package viperplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team myTeam = uc.getTeam();
    private Boolean microResult;
    private Direction microDir;

    Location resourceLocation = null;
    ResourceInfo resourcesLeft = null;

    boolean followingDeer = false;
    boolean boxesResearched = false;
    Location barracksBuilt = null;
    boolean rushAttack = false;

    ResourceInfo[] resources;
    UnitInfo[] deer;

    String state = "INI";

    void playRound(){
        if(justSpawned){
            tryReadArt();
            justSpawned = false;
        }

        round = uc.getRound();
        lightTorch();

        smokeSignals = tryReadSmoke();

        if (round != 10 || smokeSignals.length > 0) {
            //microResult = doMicro();

            tryBarracks();
            attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
            tryMove();
            attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
            trySpawn();

            if (state.equals("EXPLORE") || (state.equals("GOTORESOURCE") && followingDeer))
                attack.genericTryAttack(uc.senseUnits(Team.NEUTRAL));
        }
    }

    private Location getBaseLocation(){
        UnitInfo[] units = uc.senseUnits(2, myTeam);
        for (UnitInfo unit:units){
            if (unit.getType()==UnitType.BASE || unit.getType()==UnitType.SETTLEMENT){
                return unit.getLocation();
            }
        }
        return null;
    }

    private void explore(){
        resources = uc.senseResources();
        deer = uc.senseUnits(Team.NEUTRAL);
        int baseRange = UnitType.BASE.getAttackRange();

        for (ResourceInfo resource: resources) {
            Location resLoc = resource.getLocation();
            if (enemyBase != null && resLoc.distanceSquared(enemyBase) <= baseRange) continue;

            UnitInfo unit;
            if (uc.canSenseLocation(resLoc)) {
                unit = uc.senseUnitAtLocation(resLoc);
                if (unit == null) {
                    resourceLocation = resLoc;
                    followingDeer = false;
                    state = "GOTORESOURCE";
                    return;
                }
            } else {
                resourceLocation = resLoc;
                followingDeer = false;
                state = "GOTORESOURCE";
                return;
            }
        }

        if(deer.length > 0){
            followingDeer = true;
            state = "GOTORESOURCE";
        } else{
            move.explore();
        }
    }

    private void goToResource(){
        resources = uc.senseResources();
        int baseRange = UnitType.BASE.getAttackRange();
        if (enemyBase != null && resourceLocation != null && resourceLocation.distanceSquared(enemyBase) <= baseRange) resourceLocation = null;

        if (followingDeer){
            deer = uc.senseUnits(Team.NEUTRAL);
            if (deer.length > 0){
                resourceLocation = deer[0].getLocation();
            }
            else{
                state = "EXPLORE";
                followingDeer = false;
            }
        }

        if (resourceLocation == null) {
            state = "EXPLORE";
        } else {
            UnitInfo unit;
            if (uc.canSenseLocation(resourceLocation)) {
                unit = uc.senseUnitAtLocation(resourceLocation);
                if (unit != null) {
                    resourceLocation = null;
                    followingDeer = false;
                    state = "EXPLORE";
                    return;
                }
            }
            move.moveAvoidingEnemies(resourceLocation);
            if (!followingDeer && resourceLocation.isEqual(uc.getLocation())){
                state = "GATHER";
            }
        }
    }

    void gather(){
        Location myLoc = uc.getLocation();
        resources = uc.senseResources();

        if (resources.length > 0 && resources[0].getLocation().isEqual(myLoc)) {
            if (uc.canGatherResources()){
                uc.gatherResources();
            }

            resources = uc.senseResources();

            int[] gatheredResources = uc.getResourcesCarried();
            int total_res = 0;

            for (int res: gatheredResources) {
                total_res += res;
            }

            if (boxesResearched) {
                if (total_res == GameConstants.MAX_RESOURCE_CAPACITY_BOXES){
                    state = "DEPOSIT";
                    if (resources.length > 0) {
                        resourcesLeft = resources[0];
                    } else resourcesLeft = null;
                }
            }
            else if (total_res >= GameConstants.MAX_RESOURCE_CAPACITY) {
                state = "DEPOSIT";
                if (resources.length > 0) {
                    resourcesLeft = resources[0];
                } else resourcesLeft = null;
            }
        } else {
            state = "EXPLORE";
        }
    }

    void deposit(){
        move.moveAvoidingEnemies(baseLocation);
        if (uc.canDeposit()) {
            uc.deposit();
            if (resourcesLeft != null) {
                if ((resourcesLeft.getAmount() < 100 && resourcesLeft.getResourceType() == Resource.FOOD) || resourcesLeft.getAmount() == 0) {
                    resourcesLeft = null;
                    state = "EXPLORE";
                }
            }
            state = "GOTORESOURCE";
        }
    }

    private void tryBarracks(){
        if(barracksBuilt == null){
            if(smokeSignals.length > 0) {
                Location loc;
                int type;

                for (smokeSignal smokeSignal : smokeSignals) {
                    if (smokeSignal == null) continue;
                    loc = smokeSignal.getLoc();
                    type = smokeSignal.getType();

                    if (type == constants.RUSH_ATTACK_ENCODING) {
                        enemyBase = baseLocation.add(-loc.x, -loc.y);
                        if (enemyBase != null) {
                            move.setEnemyBase(enemyBase);
                            rushAttack = true;
                        }
                    } else if (type == constants.ENEMY_FOUND) {
                        rushAttack = true;
                    }
                }
            }
        }
        if (rushAttack && barracksBuilt == null) {
            barracksBuilt = spawnSafe(UnitType.BARRACKS);
            if (barracksBuilt != null) {
                if(uc.canMakeSmokeSignal()) {
                    int drawing = encodeEnemyBaseLoc(constants.BARRACKS_BUILT, barracksBuilt, baseLocation);
                    uc.makeSmokeSignal(drawing);
                }
            }
            if (barracksBuilt != null && enemyBase != null) {
                int drawing = encodeEnemyBaseLoc(1, enemyBase, barracksBuilt);
                if(uc.canDraw(drawing)){
                    uc.draw(drawing);
                }
            }
        }
    }

    private void tryMove() {
        if (state.equals("INI")){
            baseLocation = getBaseLocation();
            state = "EXPLORE";
        }
        if (state.equals("EXPLORE")){
            explore();
        }
        if (state.equals("GOTORESOURCE")){
            goToResource();
        }
        if (state.equals("GATHER")){
            gather();
        }
        if (state.equals("DEPOSIT")){
            deposit();
        }
    }

    private void trySpawn() {
        if (barracksBuilt == null) {
            if (uc.getRound() < 800) {
                spawnEmpty(UnitType.FARM);
                spawnEmpty(UnitType.SAWMILL);
                spawnEmpty(UnitType.QUARRY);
            }
        }
    }

    Location spawnSafe(UnitType t) {
        Location myLoc = uc.getLocation();
        Location[] traps = uc.senseTraps(2);

        outerloop:
        for (Direction dir : dirs){
            if (dir == Direction.ZERO) continue;
            if (!uc.canSpawn(t, dir)) continue;

            Location target = myLoc.add(dir);
            for (Location trap: traps) {
                if (target.isEqual(trap)) continue outerloop;
            }

            if (enemyBase == null || target.distanceSquared(enemyBase) > UnitType.BASE.getAttackRange()+4) {
                uc.spawn(t, dir);
                return myLoc.add(dir);
            }
        }
        return null;
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
            return uc.getType().getAttackRange() >= minDistToEnemy && minDistToEnemy >= uc.getType().getMinAttackRange();
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
