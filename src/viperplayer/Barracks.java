package viperplayer;

import aic2021.user.*;

public class Barracks extends MyUnit {

    Location myLoc = null;
    boolean broadCast = true;
    int spearmen = 0;
    int axemen = 0;

    Barracks(UnitController uc){
        super(uc);
    }

    void playRound(){
        myLoc = uc.getLocation();
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

        for (Smoke.smokeSignal signal: smokeSignals) {
            if (signal == null) continue;
            loc = signal.getLoc();
            type = signal.getType();
            if (type == constants.ENEMY_BASE && enemyBase == null) {
                enemyBase = loc;
                if (enemyBase != null) {
                    move.setEnemyBase(enemyBase);
                }
            }
        }

        if (broadCast && enemyBase != null) {
            if (round % 47 == 0 && uc.canMakeSmokeSignal()) {
                uc.makeSmokeSignal(smoke.encode(constants.ENEMY_BASE, enemyBase));
            }
        } else {
            if (round % 47 == 0 && uc.canMakeSmokeSignal()) {
                uc.makeSmokeSignal(smoke.encode(constants.BARRACKS_ALIVE, myLoc));
            }
        }
    }

    private void trySpawn(){
        if (round < constants.ROUND_CHECK_ATTACK) {
            spawnSafe(UnitType.SPEARMAN);
            if (2*wood < stone && stone >= 200) spawnSafe(UnitType.AXEMAN);
        }
        if (round >= constants.ROUND_CHECK_ATTACK && round < constants.ROUND_STOP_SOLDIERS) {
            if(uc.hasResearched(Technology.ROCK_ART, myTeam)) {
                if (wood > stone) {
                    spawnSafe(UnitType.SPEARMAN);
                } else {
                    spawnSafe(UnitType.AXEMAN);
                }
            }
        }
    }

    void spawnSafe(UnitType t) {
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

        int signal;
        for (Direction dir: myDirs) {
            Location target = myLoc.add(dir);
            if (uc.canRead(target)) {
                signal = uc.read(target);
                if (signal != 0) {
                    Location loc = smoke.decode(signal);
                    return loc;
                }
            }
        }
        return null;
    }
}
