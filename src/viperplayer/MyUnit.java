package viperplayer;

import aic2021.user.*;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();

    UnitController uc;
    Move move;
    Attack attack;

    int torchTurn = 0;
    int round = 0;
    int rushAttackEncoding = 17;

    MyUnit(UnitController uc){
        this.uc = uc;
        this.move = new Move(uc);
        this.attack = new Attack(uc);
    }

    abstract void playRound();

    Location decodeSignal(boolean encoded, int signal){
        if(signal%rushAttackEncoding == 0){
            signal = signal /rushAttackEncoding;
            int negative = signal % 10;
            signal = signal/10;
            int offsetY = signal%50;
            int offsetX = signal/50;
            if (negative == 1 || negative == 2) offsetX = -offsetX;
            if(negative == 1 || negative == 3) offsetY = -offsetY;
            return new Location(offsetX, offsetY);
        }
        return null;
    }

    int encodeEnemyBaseLoc(boolean encoded, Location enemyBase, Location baseLocation){
        Location offset = new Location(baseLocation.x-enemyBase.x, baseLocation.y-enemyBase.y);
        int negatives = 0;
        if(offset.x<0){
            offset.x = -offset.x;
            if(offset.y<0){
                offset.y = -offset.y;
                negatives = 1;
            }
            else negatives = 2;
        }
        else{
            if(offset.y<0){
                offset.y = -offset.y;
                negatives = 3;
            }
        }
        int drawing = (offset.x*50+offset.y)*10 + negatives;
        if(encoded){
            drawing = drawing * rushAttackEncoding;
        }
        return drawing;
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

    boolean randomThrow(){
        Location[] locs = uc.getVisibleLocations(uc.getType().getTorchThrowRange(), false);
        int index = (int)(uc.getRandomDouble()*locs.length);
        if (uc.canThrowTorch(locs[index])){
            uc.throwTorch(locs[index]);
            return true;
        }
        return false;
    }

    void lightTorch() {
        if (torchTurn -1 == round) randomThrow();

        if ((torchTurn == 0 || torchTurn -1 == round) && uc.canLightTorch()) {
            torchTurn = round + GameConstants.TORCH_DURATION;
            uc.lightTorch();
        }
    }
}
