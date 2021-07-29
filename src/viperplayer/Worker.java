package viperplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    WorkerPathfinder pathfinder;

    Team myTeam = uc.getTeam();

    Location resourceLocation = null;
    ResourceInfo resourcesLeft = null;

    boolean followingDeer = false;
    Location lastDeer = null;
    boolean boxesResearched = false;
    Location barracksBuilt = null;
    boolean rushAttack = false;
    boolean hasToSendSmokeBarracks = false;

    ResourceInfo[] resources;
    UnitInfo[] deers;

    String state = "INI";

    Worker(UnitController uc){
        super(uc);

        this.pathfinder = new WorkerPathfinder(uc);
    }

    void playRound(){
        if(justSpawned){
            tryReadArt();
            justSpawned = false;
        }

        round = uc.getRound();
        lightTorch();

        smokeSignals = tryReadSmoke();

        if (round != 10 || smokeSignals.length > 0) {
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
        deers = uc.senseUnits(Team.NEUTRAL);
        int baseRange = UnitType.BASE.getAttackRange();

        for (ResourceInfo resource: resources) {
            Location resLoc = resource.getLocation();
            if (enemyBase != null && resLoc.distanceSquared(enemyBase) <= baseRange) continue;
            if (uc.isObstructed(resLoc, uc.getLocation())) continue;

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

        for (UnitInfo deer: deers) {
            Location deerLoc = deer.getLocation();
            if (enemyBase != null && deerLoc.distanceSquared(enemyBase) <= baseRange) continue;
            if (uc.isObstructed(deerLoc, uc.getLocation())) continue;

            followingDeer = true;
            lastDeer = deerLoc;
            state = "GOTORESOURCE";
            return;
        }

        pathfinder.getNextLocationTarget(move.explore());
    }

    private void goToResource(){
        int baseRange = UnitType.BASE.getAttackRange();

        if (followingDeer){
            if (uc.canMove()) pathfinder.getNextLocationTarget(lastDeer);
            else return;

            deers = uc.senseUnits(Team.NEUTRAL);

            for (UnitInfo deer: deers) {
                Location deerLoc = deer.getLocation();
                if (enemyBase != null && deerLoc.distanceSquared(enemyBase) <= baseRange) continue;
                if (uc.isObstructed(deerLoc, uc.getLocation())) continue;

                lastDeer = deerLoc;
                return;
            }

            state = "EXPLORE";
            followingDeer = false;
            lastDeer = null;
        }

        resources = uc.senseResources();
        if (enemyBase != null && resourceLocation != null && resourceLocation.distanceSquared(enemyBase) <= baseRange) resourceLocation = null;

        if (resourceLocation == null) {
            state = "EXPLORE";
        } else {
            UnitInfo unit;
            if (uc.canSenseLocation(resourceLocation)) {
                unit = uc.senseUnitAtLocation(resourceLocation);
                if (unit != null && unit.getType() == UnitType.WORKER) {
                    resourceLocation = null;
                    followingDeer = false;
                    state = "EXPLORE";
                    return;
                }
            }
            pathfinder.getNextLocationTarget(resourceLocation);
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
        pathfinder.getNextLocationTarget(baseLocation);
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
        if (hasToSendSmokeBarracks) {
            if(uc.canMakeSmokeSignal()) {
                int drawing = smoke.encodeEnemyBaseLoc(constants.BARRACKS_BUILT, barracksBuilt, baseLocation);
                uc.makeSmokeSignal(drawing);
                hasToSendSmokeBarracks = false;
            }
        }
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
                            pathfinder.setEnemyBase(enemyBase);
                            rushAttack = true;
                        }
                    } else if (type == constants.ENEMY_BASE) {
                        enemyBase = baseLocation.add(-loc.x, -loc.y);
                        if (enemyBase != null) {
                            pathfinder.setEnemyBase(enemyBase);
                        }
                    } else if (type == constants.ENEMY_FOUND) {
                        rushAttack = true;
                    } else if (type == constants.BARRACKS_BUILT) {
                        barracksBuilt = baseLocation.add(-loc.x, -loc.y);
                    }
                }
            }
        }
        if (rushAttack && barracksBuilt == null) {
            barracksBuilt = spawnSafe(UnitType.BARRACKS);
            if (barracksBuilt != null) {
                if(uc.canMakeSmokeSignal()) {
                    int drawing = smoke.encodeEnemyBaseLoc(constants.BARRACKS_BUILT, barracksBuilt, baseLocation);
                    uc.makeSmokeSignal(drawing);
                } else hasToSendSmokeBarracks = true;
            }
            if (barracksBuilt != null && enemyBase != null) {
                int drawing = smoke.encodeEnemyBaseLoc(1, enemyBase, barracksBuilt);
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
}
