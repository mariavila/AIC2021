package viperplayer;

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
    smokeSignal[] smokeSignals = null;
    Team myTeam;
    boolean rushAttack = false;
    boolean justSpawned = true;
    boolean enemyBaseSend = false;
    boolean needsToSend = false;
    Location barracks = null;

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

    class smokeSignal {
        Location loc;
        int type;

        smokeSignal(Location loc, int type) {
            this.loc = loc;
            this.type = type;
        }

        Location getLoc() {
            return loc;
        }

        int getType() {
            return type;
        }
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
                            Location offset = smoke.decode(signal);
                            return new Location(barracks.x - offset.x, barracks.y - offset.y);
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

    MyUnit.smokeSignal[] tryReadSmoke() {
        int[] smokeSignals = uc.readSmokeSignals();
        MyUnit.smokeSignal[] decodedSignals = new MyUnit.smokeSignal[smokeSignals.length];
        int index = 0;

        if(smokeSignals.length > 0) {
            for (int smokeSignal : smokeSignals) {
                if (smokeSignal <= 0) continue;
                MyUnit.smokeSignal mySignal = decodeSignal(true, smokeSignal);
                if (mySignal != null) {
                    decodedSignals[index] = mySignal;
                    index++;
                }
            }
        }

        return decodedSignals;
    }

    void doSmokeStuffProducer() {
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
                } else if (type == constants.ENEMY_BASE) {
                    enemyBase = baseLocation.add(-loc.x, -loc.y);
                    barracksSmokeTurn = round;
                    if (enemyBase != null) {
                        move.setEnemyBase(enemyBase);
                    }
                } else if (type == constants.ENEMY_FOUND) {
                    rushAttack = true;
                } else if (type == constants.BARRACKS_BUILT) {
                    barracksBuilt = baseLocation.add(-loc.x, -loc.y);
                    barracksSmokeTurn = round;
                } else if (type == constants.BARRACKS_ALIVE) {
                    barracksSmokeTurn = round;
                }
            }
        }
        if (barracksBuilt != null && barracksSmokeTurn + 55 < round) {
            barracksBuilt = null;
        }
    }

    smokeSignal decodeSignal(boolean encoded, int signal){
        int encoding;
        if(!encoded) smoke.decode(signal);
        else if(signal % constants.RUSH_ATTACK_ENCODING == 0){
            encoding = constants.RUSH_ATTACK_ENCODING;
            signal = signal / encoding;
            Location smokeLoc = smoke.decode(signal);
            if (smokeLoc != null) return new MyUnit.smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.ENEMY_FOUND == 0){
            encoding = constants.ENEMY_FOUND;
            signal = signal / encoding;
            Location smokeLoc = smoke.decode(signal);
            if (smokeLoc != null) return new MyUnit.smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.BARRACKS_BUILT == 0){
            encoding = constants.BARRACKS_BUILT;
            signal = signal / encoding;
            Location smokeLoc = smoke.decode(signal);
            if (smokeLoc != null) return new MyUnit.smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.ENEMY_BASE == 0){
            encoding = constants.ENEMY_BASE;
            signal = signal / encoding;
            Location smokeLoc = smoke.decode(signal);
            if (smokeLoc != null) return new MyUnit.smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.BARRACKS_ALIVE == 0){
            encoding = constants.BARRACKS_ALIVE;
            signal = signal / encoding;
            Location smokeLoc = smoke.decode(signal);
            if (smokeLoc != null) return new MyUnit.smokeSignal(smokeLoc, encoding);
        }
        return null;
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

    void lightTorch(){
        if (torchTurn -1 == round) randomThrow();

        if ((torchTurn == 0 || torchTurn -1 <= round) && uc.canLightTorch()) {
            torchTurn = round + GameConstants.TORCH_DURATION;
            uc.lightTorch();
        }
    }
}
