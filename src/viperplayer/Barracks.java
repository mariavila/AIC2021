package viperplayer;

import aic2021.engine.Unit;
import aic2021.user.*;

public class Barracks extends MyUnit {

    Barracks(UnitController uc){
        super(uc);
    }

    void playRound(){
        if(justSpawned){
            enemyBase = barracksRead();
            move.setEnemyBase(enemyBase);
            justSpawned = false;
        }
        trySpawn();
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

            if (target.distanceSquared(enemyBase) > UnitType.BASE.getAttackRange()) uc.spawn(t, dir);
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
