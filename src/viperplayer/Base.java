package viperplayer;

import aic2021.user.*;

public class Base extends MyUnit {

    Base(UnitController uc){
        super(uc);
    }

    int workers = 0;
    int explorers = 0;
    int trappers = 0;
    int waterTiles = 0;

    int food = 0;
    int wood = 0;
    int stone = 0;

    boolean isBaseClose = false;
    boolean enemyExplorer = false;
    Direction[] safeSpawn = new Direction[8];

    boolean rushAttack = false;
    boolean hasWater = false;
    boolean ecoMap = false;

    void playRound(){
        round = uc.getRound();
        if(round == 0) init();

        getResources();
        smokeSignals = tryReadSmoke();

        checkAttackRush();
        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        if(!enemyExplorer) senseExplorers();
        trySpawn();
        tryResearch();
    }

    void init() {
        baseLocation = uc.getLocation();
        senseBase();
        senseInitialWater();
    }

    void getResources() {
        food = uc.getResource(Resource.FOOD);
        wood = uc.getResource(Resource.WOOD);
        stone = uc.getResource(Resource.STONE);
    }

    void senseBase(){
        UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
        if (units.length > 0) {
            isBaseClose = true;
            rushAttack = true;
            enemyBase = units[0].getLocation();
        }

        if (!isBaseClose) {
            safeSpawn = Direction.values();
        } else {
            int index = 0;
            int range = UnitType.BASE.getAttackRange();
            Direction[] myDirs = Direction.values();
            Direction[] tempDirs = new Direction[9];
            for (Direction dir: myDirs) {
                Location target = baseLocation.add(dir);
                if (target.distanceSquared(enemyBase) > range || uc.isObstructed(target, enemyBase)) {
                    tempDirs[index] = dir;
                    index++;
                }
            }

            safeSpawn = new Direction[index];

            index = 0;
            for (Direction dir: tempDirs) {
                if (dir == null) break;
                safeSpawn[index] = dir;
                index++;
            }
        }
    }

    private void checkAttackRush(){
        if (isBaseClose && round == 10) {
            int drawing = encodeEnemyBaseLoc(constants.RUSH_ATTACK_ENCODING, enemyBase, baseLocation);
            uc.makeSmokeSignal(drawing);
        }
        if (smokeSignals.length > 0) {
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
                } else if (type == constants.ENEMY_FOUND) {
                    rushAttack = true;
                }
            }
        }
    }

    private void trySpawn(){
        if (explorers < 1 && !rushAttack){
            if(spawnSafe(UnitType.EXPLORER)) ++explorers;
        }
        if (workers < 3){
            if (spawnSafe(UnitType.WORKER)) {
                workers++;
                trySpawn();
            }
        }

        if (rushAttack) spawnSafe(UnitType.WOLF);
    }

    private void tryResearch() {
        int level = uc.getTechLevel(myTeam);
        researchWheel(level);
        if(rushAttack && !uc.hasResearched(Technology.MILITARY_TRAINING, myTeam)){
            if(uc.canResearchTechnology(Technology.MILITARY_TRAINING)) uc.researchTechnology(Technology.MILITARY_TRAINING);
        }
        else {
            if(uc.canResearchTechnology(Technology.DOMESTICATION)) {
                uc.researchTechnology(Technology.DOMESTICATION);
            }
            if(hasWater && uc.canResearchTechnology(Technology.RAFTS)) {
                uc.researchTechnology(Technology.RAFTS);
            }
        }
        if(!ecoMap && !uc.hasResearched(Technology.COIN, myTeam)){
            if(uc.canResearchTechnology(Technology.COIN)) uc.researchTechnology(Technology.COIN);
        }
    }

    private void researchWheel(int level) {
        if(level == 2 && food >= 5000 && wood >= 1500 && stone >= 1500) uc.researchTechnology(Technology.SCHOOLS);
        if(level == 3 && uc.canResearchTechnology(Technology.WHEEL)) uc.researchTechnology(Technology.WHEEL);
    }

    private void senseInitialWater() {
        Location[] initialWaterTiles = uc.senseWater(uc.getType().getVisionRange());
        for(int i=0; i<initialWaterTiles.length; i++) {
            this.waterTiles++;
        }
        if (waterTiles > 9) hasWater = true;
    }

    private void senseExplorers() {
        UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
        if (units.length > 0) {
            for (int i=0; i<units.length; i++) {
                if (units[i].getType() == UnitType.EXPLORER) enemyExplorer = true;
            }
        }
    }

    private boolean spawnSafe(UnitType t){
        Direction[] myDirs = new Direction[8];
        int index = 0;

        for (Direction dir: safeSpawn) {
            if (uc.canSpawn(t, dir)) {
                myDirs[index] = dir;
                index++;
            }
        }

        if (myDirs[0] == null) return false;

        int random = (int)(uc.getRandomDouble()*index);

        uc.spawn(t, myDirs[random]);
        return true;
    }
}
