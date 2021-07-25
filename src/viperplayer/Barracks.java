package viperplayer;

import aic2021.user.*;

public class Barracks extends MyUnit {

    boolean broadCast = true;

    Barracks(UnitController uc){
        super(uc);
    }

    void playRound(){
        round = uc.getRound();
        if (justSpawned) {
            justSpawned = false;
            enemyBase = barracksRead();
            if (enemyBase != null) {
                move.setEnemyBase(enemyBase);
            } else {
                broadCast = true;
            }
        }
        smokeSignals = tryReadSmoke();
        broadCast();
        trySpawn();
    }

    void broadCast() {
        Location loc;
        int type;

        for (smokeSignal smoke: smokeSignals) {
            if (smoke == null) continue;
            loc = smoke.getLoc();
            type = smoke.getType();

            if (type == constants.ENEMY_BASE) {
                enemyBase = uc.getLocation().add(-loc.x, -loc.y);
                if (enemyBase != null) {
                    move.setEnemyBase(enemyBase);
                }
            }
        }

        if (broadCast && enemyBase != null) {
            if (round % 47 == 0 && uc.canMakeSmokeSignal()) {
                uc.makeSmokeSignal(encodeEnemyBaseLoc(constants.ENEMY_BASE, enemyBase, uc.getLocation()));
            }
        }
    }

    private void trySpawn(){
        spawnSafe(UnitType.SPEARMAN);
        spawnSafe(UnitType.AXEMAN);
    }

    void spawnSafe(UnitType t) {
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

            if (enemyBase == null || target.distanceSquared(enemyBase) > UnitType.BASE.getAttackRange()) uc.spawn(t, dir);
        }
    }

    Location barracksRead() {
        Direction[] myDirs = Direction.values();
        Location myLoc = uc.getLocation();
        int signal = 0;
        for (Direction dir: myDirs) {
            Location target = myLoc.add(dir);
            if (uc.canRead(target)) {
                signal = uc.read(target);
                if (signal != 0) {
                    Location offset = decode(signal);
                    return new Location(myLoc.x - offset.x, myLoc.y - offset.y);
                }
            }
        }
        return null;
    }
}
