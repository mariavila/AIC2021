package extra2;

import aic2021.user.*;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();

    UnitController uc;
    Move move;
    Attack attack;
    Tactics tactics;
    Smoke smoke;

    Location enemyBase = null;
    Location baseLocation = null;
    Location barracksBuilt = null;
    Smoke.smokeSignal[] smokeSignals = null;
    Team myTeam;
    boolean rushAttack = false;
    boolean justSpawned = true;
    boolean enemyBaseSend = false;
    boolean needsToSend = false;
    Location barracks = null;
    Location enemyBarracks = null;
    boolean hasToSendBarracks = false;

    int torchTurn = 0;
    int round = 0;
    int barracksSmokeTurn = 0;

    int food = 0;
    int wood = 0;
    int stone = 0;

    public final Constants constants = new Constants();

    MyUnit(UnitController uc){
        this.uc = uc;
        this.move = new Move(uc);
        this.attack = new Attack(uc);
        this.tactics = new Tactics(uc);
        this.smoke = new Smoke(uc);
        this.myTeam = uc.getTeam();
    }

    abstract void playRound();

    void getResources() {
        food = uc.getResource(Resource.FOOD);
        wood = uc.getResource(Resource.WOOD);
        stone = uc.getResource(Resource.STONE);
    }

    Location senseBarracks() {
        UnitInfo[] units = uc.senseUnits(2, myTeam);

        for (UnitInfo unit: units) {
            if (unit.getType() == UnitType.BARRACKS) return unit.getLocation();
        }
        return null;
    }

    Location lookForEnemyBase() {
        UnitInfo[] units = uc.senseUnits(myTeam.getOpponent());
        for(UnitInfo unit:units){
            if(unit.getType() == UnitType.BASE){
                move.setEnemyBase(unit.getLocation());
                return unit.getLocation();
            }
        }
        return null;
    }

    Location tryReadArt(){
        UnitInfo[] units = uc.senseUnits(uc.getTeam());
        Direction[] myDirs = Direction.values();
        int signal;

        for (UnitInfo unit: units) {
            UnitType myType = unit.getType();
            if (myType == UnitType.BARRACKS) {
                Location barracks = unit.getLocation();
                for (Direction dir: myDirs) {
                    Location target = barracks.add(dir);
                    if (uc.canRead(target)) {
                        signal = uc.read(target);
                        if (signal != 0) {
                            return smoke.decode(signal);
                        }
                    }
                }
            }
        }

        return null;
    }

    Location spawnEmpty(UnitType t){
        ResourceInfo[] res;
        Location myLoc = uc.getLocation();
        Location[] traps = uc.senseTraps(2);
        boolean hasResource;

        outerloop:
        for (Direction dir : dirs){
            if (dir == Direction.ZERO) continue;

            Location target = myLoc.add(dir);

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

    Smoke.smokeSignal[] tryReadSmoke() {
        int[] smokeSignals = uc.readSmokeSignals();
        Smoke.smokeSignal[] decodedSignals = new Smoke.smokeSignal[smokeSignals.length];
        int index = 0;

        if(smokeSignals.length > 0) {
            for (int smokeSignal : smokeSignals) {
                if (smokeSignal <= 0) continue;
                Smoke.smokeSignal mySignal = smoke.decodeSignal(true, smokeSignal);
                if (mySignal != null) {
                    decodedSignals[index] = mySignal;
                    index++;
                }
            }
        }

        return decodedSignals;
    }

    boolean randomThrow(){
        Location[] locs = uc.getVisibleLocations(2, true);
        for (Location loc:locs) {
            if (uc.canThrowTorch(loc)){
                uc.throwTorch(loc);
                return true;
            }
        }
        return false;
    }

    void senseEnemyBarracks() {
        if (enemyBarracks != null) {
            if (uc.canSenseLocation(enemyBarracks)) {
                UnitInfo unit = uc.senseUnitAtLocation(enemyBarracks);
                if (unit == null || unit.getType() != UnitType.BARRACKS) enemyBarracks = null;
            }
            if (uc.canMakeSmokeSignal() && hasToSendBarracks) {
                int drawing = smoke.encode(constants.ENEMY_BARRACKS, enemyBarracks);
                uc.makeSmokeSignal(drawing);
                hasToSendBarracks = false;
            }
        } else {
            UnitInfo[] enemies = uc.senseUnits(myTeam.getOpponent());
            for (UnitInfo enemy: enemies) {
                UnitType type = enemy.getType();
                if (type == UnitType.BARRACKS) {
                    enemyBarracks = enemy.getLocation();
                    hasToSendBarracks = true;
                }
            }
        }
    }

    void lightTorch(){
        if (torchTurn -1 == round) randomThrow();

        UnitInfo[] units = uc.senseUnits(myTeam);
        UnitType myType = uc.getType();

        if (UnitType.WORKER == myType || UnitType.EXPLORER == myType || units.length < 3) {
            if ((torchTurn == 0 || torchTurn -1 <= round) && uc.canLightTorch()) {
                torchTurn = round + GameConstants.TORCH_DURATION;
                uc.lightTorch();
            }
        }
    }
}
