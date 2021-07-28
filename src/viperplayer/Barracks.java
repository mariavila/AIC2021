package viperplayer;

import aic2021.user.*;

public class Barracks extends MyUnit {

    boolean broadCast = true;
    int spearmen = 0;
    int axemen = 0;

    Barracks(UnitController uc){
        super(uc);
    }

    void playRound(){
        round = uc.getRound();
        getResources();
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
            if (type == constants.ENEMY_BASE && enemyBase == null) {
                enemyBase = uc.getLocation().add(-loc.x, -loc.y);
                if (enemyBase != null) {
                    move.setEnemyBase(enemyBase);
                }
            }
        }

        if (broadCast && enemyBase != null) {
            if (round % 47 == 0 && uc.canMakeSmokeSignal()) {
                uc.makeSmokeSignal(smoke.encodeEnemyBaseLoc(constants.ENEMY_BASE, enemyBase, uc.getLocation()));
            }
        }
    }

    private void trySpawn(){
        if (spearmen + axemen < 5 || uc.hasResearched(Technology.TACTICS, myTeam)) {
            if (wood > stone) {
                spawnSafe(UnitType.SPEARMAN);
            } else {
                spawnSafe(UnitType.AXEMAN);
            }
        }
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

            if (enemyBase == null || target.distanceSquared(enemyBase) > UnitType.BASE.getAttackRange()) {
                uc.spawn(t, dir);
                if (t == UnitType.AXEMAN) axemen++;
                else if (t == UnitType.SPEARMAN) spearmen++;
            }
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
                    Location offset = smoke.decode(signal);
                    return new Location(myLoc.x - offset.x, myLoc.y - offset.y);
                }
            }
        }
        return null;
    }
}
