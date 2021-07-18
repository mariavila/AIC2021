package challenge24;

import aic2021.user.*;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();

    UnitController uc;
    Move move;
    Attack attack;

    MyUnit(UnitController uc){
        this.uc = uc;
        this.move = new Move(uc);
        this.attack = new Attack(uc);
    }

    abstract void playRound();

    boolean spawnRandom(UnitType t){
        for (Direction dir : dirs){
            if (uc.canSpawn(t, dir)){
                uc.spawn(t, dir);
                return true;
            }
        }
        return false;
    }

    boolean spawnEmpty(UnitType t){
        ResourceInfo[] res;
        Location myLoc = uc.getLocation();
        boolean hasResource;

        for (Direction dir : dirs){
            if (dir == Direction.ZERO) continue;

            Location target = myLoc.add(dir);

            if (uc.canSenseLocation(target)) {
                res = uc.senseResourceInfo(target);

                hasResource = res[0] != null;
                if (res[1] != null) hasResource = true;
                if (res[2] != null) hasResource = true;

                if (!hasResource && uc.canSpawn(t, dir)){
                    uc.spawn(t, dir);
                    return true;
                }
            }
        }
        return false;
    }

    boolean moveRandom(){
        int tries = 10;
        while (uc.canMove() && tries-- > 0){
            int random = (int)(uc.getRandomDouble()*8);
            if (uc.canMove(dirs[random])){
                uc.move(dirs[random]);
                return true;
            }
        }
        return false;
    }

    boolean lightTorch(){
        if (uc.canLightTorch()){
            uc.lightTorch();
            return true;
        }
        return false;
    }

    boolean randomThrow(){
        Location[] locs = uc.getVisibleLocations(uc.getType().getTorchThrowRange(), false);
        int index = (int)(uc.getRandomDouble()*locs.length);
        if (uc.canThrowTorch(locs[index])){
            uc.throwTorch(locs[index]);
            return true;
        }
        return false;
    }
}
