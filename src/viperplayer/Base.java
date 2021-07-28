package viperplayer;

import aic2021.user.*;

public class Base extends MyUnit {

    Base(UnitController uc){
        super(uc);
    }

    int workers = 0;
    int explorers = 0;
    int trappers = 0;
    int wolves = 0;
    int waterTiles = 0;

    int initialFood = 0;
    int initialWood = 0;
    int initialStone = 0;

    boolean isBaseClose = false;
    boolean enemyExplorer = false;
    Direction[] safeSpawn = new Direction[8];

    boolean rushAttack = false;
    boolean normalAttack = false;
    boolean hasWater = false;
    boolean ecoMap = false;
    int idealWorkers = 3;

    void playRound(){
        round = uc.getRound();
        if(round == 0) init();

        calcIdealWorkers();
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
        senseInitialResources();
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
            int drawing = smoke.encodeEnemyBaseLoc(constants.RUSH_ATTACK_ENCODING, enemyBase, baseLocation);
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
        if (workers < idealWorkers && !rushAttack){
            if (spawnSafe(UnitType.WORKER)) {
                workers++;
                trySpawn();
            }
        }

        if (rushAttack) {
            if (wolves < 5 || uc.hasResearched(Technology.EUGENICS, myTeam)) {
                spawnSafe(UnitType.WOLF);
                wolves++;
            }
        }
    }

    private void tryResearch() {
        int level = uc.getTechLevel(myTeam);
        if(level >= 2) {
            researchWheel(level);
        }
        if(level >= 1) {
            if(!ecoMap && !rushAttack){
                if(!uc.hasResearched(Technology.JOBS, myTeam)) {
                    if (uc.canResearchTechnology(Technology.JOBS)) uc.researchTechnology(Technology.JOBS);
                }
            }
            if(!uc.hasResearched(Technology.TACTICS, myTeam)) {
                if (uc.canResearchTechnology(Technology.TACTICS)) uc.researchTechnology(Technology.TACTICS);
            }
            if(uc.hasResearched(Technology.DOMESTICATION, myTeam) && !uc.hasResearched(Technology.EUGENICS, myTeam)) {
                if(uc.canResearchTechnology(Technology.EUGENICS)) uc.researchTechnology(Technology.EUGENICS);
            }
            if(!uc.hasResearched(Technology.COOKING, myTeam)) {
                if (uc.canResearchTechnology(Technology.COOKING)) uc.researchTechnology(Technology.COOKING);
            } else if(!uc.hasResearched(Technology.SHARPENERS, myTeam)) {
                if (uc.canResearchTechnology(Technology.SHARPENERS)) uc.researchTechnology(Technology.SHARPENERS);
            }
        }
        if(level >= 0) {
            if(hasWater && uc.canResearchTechnology(Technology.RAFTS)) {
                uc.researchTechnology(Technology.RAFTS);
            }
            if(rushAttack){
                if(!uc.hasResearched(Technology.DOMESTICATION, myTeam) && uc.canResearchTechnology(Technology.DOMESTICATION)) {
                    uc.researchTechnology(Technology.DOMESTICATION);
                }
                if (!uc.hasResearched(Technology.MILITARY_TRAINING, myTeam)) {
                    if(uc.canResearchTechnology(Technology.MILITARY_TRAINING)) uc.researchTechnology(Technology.MILITARY_TRAINING);
                }
            }
            if(normalAttack) {
                if (!uc.hasResearched(Technology.MILITARY_TRAINING, myTeam)) {
                    if(uc.canResearchTechnology(Technology.MILITARY_TRAINING)) uc.researchTechnology(Technology.MILITARY_TRAINING);
                }
            }
            if(ecoMap){
                if(!rushAttack || round > 500) {
                    if (!uc.hasResearched(Technology.BOXES, myTeam)) {
                        if (uc.canResearchTechnology(Technology.BOXES)) uc.researchTechnology(Technology.BOXES);
                    }
                    if (!uc.hasResearched(Technology.UTENSILS, myTeam)) {
                        if (uc.canResearchTechnology(Technology.UTENSILS)) uc.researchTechnology(Technology.UTENSILS);
                    }
                }
            } else{
                if(!uc.hasResearched(Technology.COIN, myTeam)) {
                    if(uc.canResearchTechnology(Technology.COIN)) uc.researchTechnology(Technology.COIN);
                }
            }
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

    private void senseInitialResources() {
        ResourceInfo[] initialResources = uc.senseResources();
        for(int i=0; i<initialResources.length; i++) {
            if (initialResources[i].resourceType == Resource.FOOD) {
                initialFood += initialResources[i].amount;
            } else if (initialResources[i].resourceType == Resource.WOOD) {
                initialWood += initialResources[i].amount;
            } else {
                initialStone += initialResources[i].amount;
            }
        }
        if (initialFood + initialWood + initialStone > 1000) {
            ecoMap = true;
        }
    }

    private void calcIdealWorkers() {
        if(round > 25) {
            if (ecoMap) {
                idealWorkers = 8;
            } else {
                idealWorkers = 5;
            }
        }
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
