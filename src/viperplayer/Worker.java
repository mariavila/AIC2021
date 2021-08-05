package viperplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    WorkerPathfinder pathfinder;

    Team myTeam = uc.getTeam();

    Location resourceLocation = null;
    ResourceInfo resourcesLeft = null;

    boolean followingDeer = false;
    Location lastDeer = null;
    boolean hasToSendSmokeBarracks = false;
    Location closeSettlement = null;
    ResourceInfo[] resources;
    Resource bestResType;
    UnitInfo[] deers;
    Location targetDeposit = null;

    String state = "INI";

    Worker(UnitController uc){
        super(uc);

        this.pathfinder = new WorkerPathfinder(uc);
    }

    void playRound(){
        round = uc.getRound();

        if(justSpawned){
            move.init();
            justSpawned = false;
        }

        lightTorch();
        resources = uc.senseResources();
        getResources();
        getBestResType();

        smokeSignals = tryReadSmoke();

        if ((round != 10 && round != 9) || smokeSignals.length > 0) {
            refreshSettlement();
            tryBarracks();
            doSmokeStuffProducer();
            Location readArt = tryReadArt();
            if (readArt != null) {
                enemyBase = readArt;
                move.setEnemyBase(enemyBase);
                pathfinder.setEnemyBase(enemyBase);
            }
            attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
            tryGather();
            tryMove();
            tryGather();
            attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
            attack.genericTryAttack(uc.senseUnits(Team.NEUTRAL));
            tryEcoBuilding();

            /*if (state.equals("EXPLORE") || (state.equals("GOTORESOURCE") && followingDeer))
                attack.genericTryAttack(uc.senseUnits(Team.NEUTRAL));*/
        }
    }

    private void tryGather() {
        if (uc.canGatherResources()){
            uc.gatherResources();
        }
        if (uc.canDeposit()){
            uc.deposit();
        }
    }

    private void refreshSettlement() {
        if (baseLocation != null && uc.canSenseLocation(baseLocation)) {
            UnitInfo base = uc.senseUnitAtLocation(baseLocation);
            if (base == null || (base.getType() != UnitType.BASE && base.getType() != UnitType.SETTLEMENT)) {
                baseLocation = null;
            }
        }

        if (closeSettlement != null && uc.canSenseLocation(closeSettlement)) {
            UnitInfo settlement = uc.senseUnitAtLocation(closeSettlement);
            if (settlement == null || (settlement.getType() != UnitType.BASE && settlement.getType() != UnitType.SETTLEMENT)) {
                closeSettlement = null;
            }
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
        int baseRange = UnitType.BASE.getAttackRange();
        deers = uc.senseUnits(Team.NEUTRAL);
        checkForBestResource();
        followingDeer = false;

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

    void checkForBestResource() {
        int baseRange = UnitType.BASE.getAttackRange();
        Location myLoc = uc.getLocation();
        resources = uc.senseResources();

        Location bestRes = null;
        Resource bestType = null;

        for (ResourceInfo resource: resources) {
            Location resLoc = resource.getLocation();
            Resource resType = resource.getResourceType();
            if (enemyBase != null && resLoc.distanceSquared(enemyBase) <= baseRange) continue;
            if (uc.isObstructed(resLoc, uc.getLocation())) continue;

            UnitInfo unit;
            if (uc.canSenseLocation(resLoc)) {
                unit = uc.senseUnitAtLocation(resLoc);
                if (unit == null || unit.getType() != UnitType.WORKER || myLoc.isEqual(unit.getLocation())) {
                    if (resType == Resource.FOOD && bestType != Resource.FOOD) {
                        bestRes = resLoc;
                        bestType = resType;
                    } else if (resType == Resource.WOOD && bestType != Resource.FOOD && bestType != Resource.WOOD) {
                        bestRes = resLoc;
                        bestType = resType;
                    } else if (resType == Resource.STONE && bestType == null) {
                        bestRes = resLoc;
                        bestType = resType;
                    }
                }
            }
        }

        if (bestRes != null) {
            resourceLocation = bestRes;
            followingDeer = false;
            if (uc.getLocation().isEqual(bestRes)) state = "GATHER";
            else state = "GOTORESOURCE";
        } else if (resourceLocation == null || (resourceLocation != null && uc.canSenseLocation(resourceLocation))) {
            resourceLocation = null;
            state = "EXPLORE";
        } else {
            state = "GOTORESOURCE";
        }
    }

    private void goToResource(){
        int baseRange = UnitType.BASE.getAttackRange();
        checkForBestResource();

        if (resources.length > 0) {
            if (uc.getLocation().isEqual(resourceLocation)) {
                state = "GATHER";
                return;
            } else if (uc.senseUnitAtLocation(resourceLocation) != null) {
                resourceLocation = null;
            }
        }

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
        checkForBestResource();
        resources = uc.senseResources();

        if (resources.length > 0 && resources[0].getLocation().isEqual(myLoc)) {
            if (uc.canGatherResources()){
                uc.gatherResources();
            }

            int[] gatheredResources = uc.getResourcesCarried();
            int maxRes = 0;
            for (int res: gatheredResources) {
                if(res > maxRes) maxRes = res;
            }

            if (uc.hasResearched(Technology.BOXES, myTeam)) {
                if (maxRes >= GameConstants.MAX_RESOURCE_CAPACITY_BOXES){
                    state = "DEPOSIT";
                    if (resources.length > 0) {
                        resourcesLeft = resources[0];
                    } else resourcesLeft = null;
                    targetDeposit();
                }
            }
            else if (maxRes >= GameConstants.MAX_RESOURCE_CAPACITY) {
                state = "DEPOSIT";
                if (resources.length > 0) {
                    resourcesLeft = resources[0];
                } else resourcesLeft = null;
                targetDeposit();
            }
        } else {
            state = "EXPLORE";
        }
    }

    void targetDeposit() {
        if (closeSettlement == null && baseLocation == null) targetDeposit = null;
        else if (closeSettlement != null && baseLocation == null) targetDeposit = closeSettlement;
        else if (closeSettlement == null && baseLocation != null) targetDeposit = baseLocation;
        else {
            Location myLoc = uc.getLocation();
            int baseDistance = baseLocation.distanceSquared(myLoc);
            int settlementDistance = closeSettlement.distanceSquared(myLoc);

            if (baseDistance < settlementDistance) targetDeposit = baseLocation;
            else targetDeposit = closeSettlement;
        }
    }

    void deposit(){
        Location myLoc = uc.getLocation();
        if(canSpawnSettlement(myLoc)) {
            spawnEmpty(UnitType.SETTLEMENT);
        }
        if (uc.canDeposit()) {
            uc.deposit();
            if (resourcesLeft != null) {
                if ((resourcesLeft.getAmount() < 100 && resourcesLeft.getResourceType() == Resource.FOOD) || resourcesLeft.getAmount() == 0) {
                    resourcesLeft = null;
                    state = "EXPLORE";
                } else {
                    resourceLocation = resourcesLeft.location;
                    state = "GOTORESOURCE";
                }
            }
        } else {
            pathfinder.getNextLocationTarget(targetDeposit);
        }
    }

    boolean canSpawnSettlement(Location myLoc){
        int baseDist = myLoc.distanceSquared(baseLocation);

        if (baseDist < 36) return false;
        if (closeSettlement != null && myLoc.distanceSquared(closeSettlement) < 36) return false;

        UnitInfo[] allies = uc.senseUnits(myTeam);
        for(int i = 0; i < allies.length; i++) {
            if (allies[i].getType() == UnitType.SETTLEMENT) return false;
        }

        if (baseDist > 100 && (closeSettlement == null || myLoc.distanceSquared(closeSettlement) > 100)) return true;

        int closeResources = 0;
        for(int i = 0; i < resources.length; i++) {
            closeResources += resources[i].getAmount();
            if (closeResources >= 350) break;
        }
        if(closeResources < 350) return false;

        return true;
    }

    private void tryBarracks(){
        if (uc.hasResearched(Technology.JOBS, myTeam)) return;
        if (hasToSendSmokeBarracks) {
            if(uc.canMakeSmokeSignal()) {
                int drawing = smoke.encode(constants.BARRACKS_BUILT, barracksBuilt);
                uc.makeSmokeSignal(drawing);
                hasToSendSmokeBarracks = false;
            }
        }

        if (rushAttack && barracksBuilt == null && uc.canMakeSmokeSignal()) {
            barracksBuilt = spawnEmpty(UnitType.BARRACKS);
            if (barracksBuilt != null) {
                barracksSmokeTurn = round;
                if(uc.canMakeSmokeSignal()) {
                    int drawing = smoke.encode(constants.BARRACKS_BUILT, barracksBuilt);
                    uc.makeSmokeSignal(drawing);
                } else hasToSendSmokeBarracks = true;
            }
        }
    }

    private void tryEcoBuilding() {
        if(round < 1700 && uc.hasResearched(Technology.JOBS, myTeam)) {
            if(uc.getTechLevel(myTeam) < 3) {
                if(bestResType == Resource.FOOD) spawnEmptySpaced(UnitType.FARM);
                else if(bestResType == Resource.WOOD) spawnEmptySpaced(UnitType.SAWMILL);
                else spawnEmptySpaced(UnitType.QUARRY);
                getResources();
                getBestResType();
                if(bestResType == Resource.FOOD) spawnEmptySpaced(UnitType.FARM);
                else if(bestResType == Resource.WOOD) spawnEmptySpaced(UnitType.SAWMILL);
                else spawnEmptySpaced(UnitType.QUARRY);
            }
        }
    }

    Location spawnEmptySpaced(UnitType t){
        ResourceInfo[] res;
        Location myLoc = uc.getLocation();
        Location[] traps = uc.senseTraps(2);
        boolean hasResource;

        outerloop:
        for (Direction dir : dirs){
            if (dir == Direction.ZERO) continue;

            Location target = myLoc.add(dir);
            if ((target.x + target.y) % 2 == 0) continue;

            for (Location trap: traps) {
                if (target.isEqual(trap)) continue outerloop;
            }

            if (uc.canSenseLocation(target)) {
                res = uc.senseResourceInfo(target);

                hasResource = res[0] != null;
                if (res[1] != null) hasResource = true;
                if (res[2] != null) hasResource = true;

                if (!hasResource && (enemyBase == null || target.distanceSquared(enemyBase) > UnitType.BASE.getAttackRange()+4) && uc.canSpawn(t, dir)){
                    uc.spawn(t, dir);
                    return myLoc.add(dir);
                }
            }
        }
        return null;
    }

    private void tryMove() {
        if (state.equals("INI")){
            baseLocation = getBaseLocation();
            state = "EXPLORE";
            if (baseLocation != null) closeSettlement = baseLocation;
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

    void doSmokeStuffProducer() {
        if(smokeSignals.length > 0) {
            Location loc;
            int type;

            for (Smoke.smokeSignal smokeSignal : smokeSignals) {
                if (smokeSignal == null) continue;
                loc = smokeSignal.getLoc();
                type = smokeSignal.getType();

                if (type == constants.RUSH_ATTACK_ENCODING) {
                    if (enemyBase == null) {
                        enemyBase = loc;
                        if (enemyBase != null) {
                            pathfinder.setEnemyBase(enemyBase);
                            move.setEnemyBase(enemyBase);
                            rushAttack = true;
                        }
                    }
                } else if (type == constants.ENEMY_BASE) {
                    barracksSmokeTurn = round;
                    if (enemyBase == null) {
                        enemyBase = loc;
                        if (enemyBase != null) {
                            pathfinder.setEnemyBase(enemyBase);
                            move.setEnemyBase(enemyBase);
                        }
                    }
                } else if (type == constants.ENEMY_FOUND) {
                    rushAttack = true;
                } else if (type == constants.BARRACKS_BUILT) {
                    barracksBuilt = loc;
                    barracksSmokeTurn = round;
                } else if (type == constants.BARRACKS_ALIVE) {
                    barracksSmokeTurn = round;
                } else if (type == constants.ECO_MAP || type == constants.SETTLEMENT) {
                    Location myLoc = uc.getLocation();
                    int distance = myLoc.distanceSquared(loc);
                    if (distance < myLoc.distanceSquared(closeSettlement)) closeSettlement = loc;
                }
            }
        }
        if (barracksBuilt != null && barracksSmokeTurn + 55 < round) {
            barracksBuilt = null;
        }
    }

    void getBestResType() {
        if (food <= wood && food <= stone) {
            bestResType = Resource.FOOD;
        } else if (wood <= food && wood <= stone) {
            bestResType = Resource.WOOD;
        } else if (stone <= food && stone <= wood) {
            bestResType = Resource.STONE;
        }
    }
}
