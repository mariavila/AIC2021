package extra4;

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
    int spawnSpaces = 0;
    int idealWorkers = 3;
    int techLevel = 0;
    int enemyTechLevel = 0;

    int enemySpearmen = 0;
    int enemyAxemen = 0;
    int enemyTrappers = 0;
    int enemyWolves = 0;

    int allySpearmen = 0;
    int allyAxemen = 0;
    int allyTrappers = 0;
    int allyWolves = 0;
    int allyWorkers = 0;

    int initialFood = 0;
    int initialWood = 0;
    int initialStone = 0;

    boolean isBaseClose = false;
    boolean enemyExplorer = false;
    Direction[] safeSpawn = new Direction[8];

    boolean normalAttack = false;
    boolean hasWater = false;
    boolean waterReady = true;
    boolean ecoMap = false;
    boolean bigResources = false;
    boolean baseCorner = false;
    boolean smokeAttack = false;

    void playRound(){
        round = uc.getRound();
        if(round == 0) init();
        //if(round % 20 == 0) printTechsResearched();

        if(uc.hasResearched(Technology.ROCK_ART, myTeam) && !smokeAttack) {
            enemyTechLevel = uc.getTechLevel(myTeam.getOpponent());
            if(enemyTechLevel >= 2 && enemyBase != null && uc.canMakeSmokeSignal()) {
                int drawing = smoke.encode(constants.ATTACK_BASE, enemyBase);
                uc.makeSmokeSignal(drawing);
                smokeAttack = true;
            }
        } else if(smokeAttack && round % 350 == 0) {
            smokeAttack = false;
        }
        if (isBaseClose && !rushAttack && uc.isAccessible(enemyBase)) {
            rushAttack = true;
        }

        broadCast();

        senseEnemyUnits();
        senseAllyUnits();
        calcIdealWorkers();
        getResources();
        smokeSignals = tryReadSmoke();

        checkAttackRush();
        if (round > 9) attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        if(!enemyExplorer) senseExplorers();
        trySpawn();
        tryResearch();
    }

    void init() {
        baseLocation = uc.getLocation();
        senseBase();
        senseInitialConditions();
    }

    void broadCast() {
        if (enemyBase != null) {
            if (round % 41 == 0 && round != 0 && uc.canMakeSmokeSignal()) {
                if (rushAttack) uc.makeSmokeSignal(smoke.encode(constants.RUSH_ATTACK_ENCODING, enemyBase));
                else uc.makeSmokeSignal(smoke.encode(constants.ENEMY_BASE, enemyBase));
            }
        }
    }

    void senseBase(){
        UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
        for (UnitInfo unit: units) {
            if (unit.getType() == UnitType.BASE) {
                enemyBase = unit.getLocation();
                isBaseClose = true;
            }
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

    void printTechsResearched() {
        if(uc.hasResearched(Technology.DOMESTICATION, myTeam)) uc.println("DOMESTICATION");
        if(uc.hasResearched(Technology.MILITARY_TRAINING, myTeam)) uc.println("MILITARY_TRAINING");
        if(uc.hasResearched(Technology.BOXES, myTeam)) uc.println("BOXES");
        if(uc.hasResearched(Technology.ROCK_ART, myTeam)) uc.println("ROCK_ART");
        if(uc.hasResearched(Technology.UTENSILS, myTeam)) uc.println("UTENSILS");
        if(uc.hasResearched(Technology.RAFTS, myTeam)) uc.println("RAFTS");
        if(uc.hasResearched(Technology.COIN, myTeam)) uc.println("COIN");
        if(uc.hasResearched(Technology.SHARPENERS, myTeam)) uc.println("SHARPENERS");
        if(uc.hasResearched(Technology.COOKING, myTeam)) uc.println("COOKING");
        if(uc.hasResearched(Technology.EUGENICS, myTeam)) uc.println("EUGENICS");
        if(uc.hasResearched(Technology.NAVIGATION, myTeam)) uc.println("NAVIGATION");
        if(uc.hasResearched(Technology.OIL, myTeam)) uc.println("OIL");
        if(uc.hasResearched(Technology.VOCABULARY, myTeam)) uc.println("VOCABULARY");
        if(uc.hasResearched(Technology.HUTS, myTeam)) uc.println("HUTS");
        if(uc.hasResearched(Technology.TACTICS, myTeam)) uc.println("TACTICS");
        if(uc.hasResearched(Technology.JOBS, myTeam)) uc.println("JOBS");
    }

    private void checkAttackRush(){
        if (isBaseClose && round == 9 && uc.canMakeSmokeSignal()) {
            int drawing = smoke.encode(constants.RUSH_ATTACK_ENCODING, enemyBase);
            uc.makeSmokeSignal(drawing);
        }
        doSmokeStuffProducer();
    }

    private void trySpawn(){
        if (explorers < 1 && !rushAttack){
            if(spawnSafe(UnitType.EXPLORER)) ++explorers;
        }

        if (uc.hasResearched(Technology.JOBS, myTeam) && allyWorkers == 0 && round < 1700) spawnSafe(UnitType.WORKER);

        if (enemyAxemen+enemySpearmen+enemyWolves == 0) {
            if (workers < idealWorkers) {
                if (spawnSafe(UnitType.WORKER)) {
                    workers++;
                    trySpawn();
                }
            }

            if (rushAttack) {
                if (barracksBuilt == null && round % 100 == 0 && round > 300) {
                    spawnSafe(UnitType.WORKER);
                    workers++;
                }
            }
        }
        if ((enemyAxemen+enemySpearmen+enemyWolves > allyWolves)) {
            spawnSafe(UnitType.WOLF);
            wolves++;
        }
    }

    private void tryResearch() {
        techLevel = uc.getTechLevel(myTeam);
        if(round >= 1998) researchAll();
        if(spawnSpaces <= 1) {
            researchWheelPath();
        } else {
            researchAdaptivePath();
        }
    }

    private void researchAdaptivePath(){
        if(techLevel >= 2) {
            if(food > 6500 && !uc.hasResearched(Technology.POISON, myTeam)) {
                uc.researchTechnology(Technology.POISON);
            }
            if(wood > 4500 && stone > 5250 && !uc.hasResearched(Technology.HOUSES, myTeam)) {
                uc.researchTechnology(Technology.HOUSES);
            }
            researchWheel();
        }
        if(techLevel >= 1) {
            if(!uc.hasResearched(Technology.JOBS, myTeam)) {
                if (uc.canResearchTechnology(Technology.JOBS)) uc.researchTechnology(Technology.JOBS);
                return;
            }
            if(!uc.hasResearched(Technology.TACTICS, myTeam)) {
                if (uc.canResearchTechnology(Technology.TACTICS)) uc.researchTechnology(Technology.TACTICS);
            }
            if(!uc.hasResearched(Technology.COOKING, myTeam)) {
                if (uc.canResearchTechnology(Technology.COOKING)) uc.researchTechnology(Technology.COOKING);
            }
            /*if(uc.hasResearched(Technology.DOMESTICATION, myTeam) && !uc.hasResearched(Technology.EUGENICS, myTeam)) {
                if(uc.canResearchTechnology(Technology.EUGENICS)) uc.researchTechnology(Technology.EUGENICS);
            }*/
            /*if(uc.hasResearched(Technology.COOKING, myTeam) && !uc.hasResearched(Technology.SHARPENERS, myTeam)) {
                if (uc.canResearchTechnology(Technology.SHARPENERS)) uc.researchTechnology(Technology.SHARPENERS);
            }*/
        }
        if(techLevel >= 0) {
            if(round > constants.ROUND_CHECK_ATTACK || (round > constants.ROUND_BUILD_JOBS && techLevel == 0)) {
                if(uc.canResearchTechnology(Technology.ROCK_ART)) {
                    uc.researchTechnology(Technology.ROCK_ART);
                }
            }
            if(hasWater && uc.canResearchTechnology(Technology.RAFTS)) {
                uc.researchTechnology(Technology.RAFTS);
                waterReady = true;
            }
            /*if(enemyAxemen+enemySpearmen+enemyWolves > 0) {
                if(!uc.hasResearched(Technology.DOMESTICATION, myTeam) && uc.canResearchTechnology(Technology.DOMESTICATION)) {
                    uc.researchTechnology(Technology.DOMESTICATION);
                }
            }*/
            if(rushAttack && waterReady){
                if (!uc.hasResearched(Technology.MILITARY_TRAINING, myTeam)) {
                    if (food >= 150 && wood >= 300 && stone >= 250) {
                        if(uc.canResearchTechnology(Technology.MILITARY_TRAINING)) uc.researchTechnology(Technology.MILITARY_TRAINING);
                    }
                }
                if(!uc.hasResearched(Technology.DOMESTICATION, myTeam) && uc.canResearchTechnology(Technology.DOMESTICATION)) {
                    uc.researchTechnology(Technology.DOMESTICATION);
                }
            }
            if(normalAttack) {
                if (!uc.hasResearched(Technology.MILITARY_TRAINING, myTeam)) {
                    if (food >= 150 && wood >= 300 && stone >= 250) {
                        if(uc.canResearchTechnology(Technology.MILITARY_TRAINING)) uc.researchTechnology(Technology.MILITARY_TRAINING);
                    }
                }
                if(!uc.hasResearched(Technology.DOMESTICATION, myTeam) && uc.canResearchTechnology(Technology.DOMESTICATION)) {
                    uc.researchTechnology(Technology.DOMESTICATION);
                }
            }
            if(ecoMap){
                if(round > 25) {
                    if (!uc.hasResearched(Technology.UTENSILS, myTeam)) {
                        if (uc.canResearchTechnology(Technology.UTENSILS)) uc.researchTechnology(Technology.UTENSILS);
                    }
                    if (round > 300 && !uc.hasResearched(Technology.BOXES, myTeam)) {
                        if (uc.canResearchTechnology(Technology.BOXES)) uc.researchTechnology(Technology.BOXES);
                    }
                }
            } else{
                if(!uc.hasResearched(Technology.COIN, myTeam)) {
                    if(uc.canResearchTechnology(Technology.COIN)) uc.researchTechnology(Technology.COIN);
                }
            }
        }
    }

    private void researchWheelPath(){
        if(!uc.hasResearched(Technology.COIN, myTeam) && uc.canResearchTechnology(Technology.COIN)) {
            uc.researchTechnology(Technology.COIN);
        }
        else if(uc.hasResearched(Technology.COIN, myTeam) && !uc.hasResearched(Technology.BOXES, myTeam) && uc.canResearchTechnology(Technology.BOXES)) {
            uc.researchTechnology(Technology.BOXES);
        }
        else if(!uc.hasResearched(Technology.UTENSILS, myTeam) && uc.canResearchTechnology(Technology.UTENSILS)) {
            uc.researchTechnology(Technology.UTENSILS);
        }
        else if(!uc.hasResearched(Technology.TACTICS, myTeam) && uc.canResearchTechnology(Technology.TACTICS)) {
            uc.researchTechnology(Technology.TACTICS);
        }
        else if(!uc.hasResearched(Technology.COOKING, myTeam) && uc.canResearchTechnology(Technology.COOKING)) {
            uc.researchTechnology(Technology.COOKING);
        }
        else if(!uc.hasResearched(Technology.EUGENICS, myTeam) && uc.canResearchTechnology(Technology.EUGENICS)) {
            uc.researchTechnology(Technology.EUGENICS);
        }
        if(techLevel >= 2) {
            researchWheel();
        }
    }

    private void researchAll(){
        if(techLevel == 0 && uc.canResearchTechnology(Technology.DOMESTICATION)) uc.researchTechnology(Technology.DOMESTICATION);
        if(uc.getTechLevel(myTeam) == 0 && uc.canResearchTechnology(Technology.MILITARY_TRAINING)) uc.researchTechnology(Technology.MILITARY_TRAINING);
        if(uc.getTechLevel(myTeam) == 0 && uc.canResearchTechnology(Technology.BOXES)) uc.researchTechnology(Technology.BOXES);
        if(uc.getTechLevel(myTeam) == 0 && uc.canResearchTechnology(Technology.ROCK_ART)) uc.researchTechnology(Technology.ROCK_ART);
        if(uc.getTechLevel(myTeam) == 0 && uc.canResearchTechnology(Technology.UTENSILS)) uc.researchTechnology(Technology.UTENSILS);
        if(uc.getTechLevel(myTeam) == 0 && uc.canResearchTechnology(Technology.RAFTS)) uc.researchTechnology(Technology.RAFTS);
        if(uc.getTechLevel(myTeam) == 0 && uc.canResearchTechnology(Technology.COIN)) uc.researchTechnology(Technology.COIN);
        if(uc.getTechLevel(myTeam) == 1 && uc.canResearchTechnology(Technology.SHARPENERS)) uc.researchTechnology(Technology.SHARPENERS);
        if(uc.getTechLevel(myTeam) == 1 && uc.canResearchTechnology(Technology.COOKING)) uc.researchTechnology(Technology.COOKING);
        if(uc.getTechLevel(myTeam) == 1 && uc.canResearchTechnology(Technology.EUGENICS)) uc.researchTechnology(Technology.EUGENICS);
        if(uc.getTechLevel(myTeam) == 1 && uc.canResearchTechnology(Technology.NAVIGATION)) uc.researchTechnology(Technology.NAVIGATION);
        if(uc.getTechLevel(myTeam) == 1 && uc.canResearchTechnology(Technology.OIL)) uc.researchTechnology(Technology.OIL);
        if(uc.getTechLevel(myTeam) == 1 && uc.canResearchTechnology(Technology.VOCABULARY)) uc.researchTechnology(Technology.VOCABULARY);
        if(uc.getTechLevel(myTeam) == 1 && uc.canResearchTechnology(Technology.HUTS)) uc.researchTechnology(Technology.HUTS);
        if(uc.getTechLevel(myTeam) == 1 && uc.canResearchTechnology(Technology.TACTICS)) uc.researchTechnology(Technology.TACTICS);
        if(uc.getTechLevel(myTeam) == 1 && uc.canResearchTechnology(Technology.JOBS)) uc.researchTechnology(Technology.JOBS);
        if(uc.getTechLevel(myTeam) == 2 && uc.canResearchTechnology(Technology.SCHOOLS)) uc.researchTechnology(Technology.SCHOOLS);
        if(uc.getTechLevel(myTeam) == 2 && uc.canResearchTechnology(Technology.CRYSTALS)) uc.researchTechnology(Technology.CRYSTALS);
        if(uc.getTechLevel(myTeam) == 2 && uc.canResearchTechnology(Technology.COMBUSTION)) uc.researchTechnology(Technology.COMBUSTION);
        if(uc.getTechLevel(myTeam) == 2 && uc.canResearchTechnology(Technology.POISON)) uc.researchTechnology(Technology.POISON);
        if(uc.getTechLevel(myTeam) == 2 && uc.canResearchTechnology(Technology.EXPERTISE)) uc.researchTechnology(Technology.EXPERTISE);
        if(uc.getTechLevel(myTeam) == 2 && uc.canResearchTechnology(Technology.FLINT)) uc.researchTechnology(Technology.FLINT);
        if(uc.getTechLevel(myTeam) == 2 && uc.canResearchTechnology(Technology.HOUSES)) uc.researchTechnology(Technology.HOUSES);
        if(uc.getTechLevel(myTeam) == 2 && uc.canResearchTechnology(Technology.POISON)) uc.researchTechnology(Technology.POISON);
        if(uc.canResearchTechnology(Technology.WHEEL)) uc.researchTechnology(Technology.WHEEL);
    }

    private void researchWheel() {
        if(techLevel == 2) {
            if(food >= 5000 && wood >= 1500 && stone >= 1500) {
                uc.researchTechnology(Technology.SCHOOLS);
            } else if((food >= 3000 || (food >= 1500 && uc.hasResearched(Technology.POISON, myTeam))) && wood >= 3000 && stone >= 3000) {
                uc.researchTechnology(Technology.CRYSTALS);
                uc.researchTechnology(Technology.COMBUSTION);
                uc.researchTechnology(Technology.POISON);
            } else if(food >= 2000 && wood >= 3500 && stone >= 3500) {
                uc.researchTechnology(Technology.CRYSTALS);
                uc.researchTechnology(Technology.COMBUSTION);
                uc.researchTechnology(Technology.EXPERTISE);
            } else if((food >= 2000 && (wood >= 2750 && stone >= 4250)) || ((uc.hasResearched(Technology.HOUSES, myTeam) && wood >= 2000 && stone >= 3500))) {
                uc.researchTechnology(Technology.CRYSTALS);
                uc.researchTechnology(Technology.EXPERTISE);
                uc.researchTechnology(Technology.HOUSES);
            } else if(food >= 1500) {
                if((wood > 3750 && stone > 3750) || (uc.hasResearched(Technology.HOUSES, myTeam) && wood >= 3000 && stone >= 3000)) {
                    uc.researchTechnology(Technology.CRYSTALS);
                    uc.researchTechnology(Technology.COMBUSTION);
                    uc.researchTechnology(Technology.HOUSES);
                } else if(wood > 3000 && stone > 4500) {
                    uc.researchTechnology(Technology.CRYSTALS);
                    uc.researchTechnology(Technology.COMBUSTION);
                    uc.researchTechnology(Technology.FLINT);
                }
            }
        }
        techLevel = uc.getTechLevel(myTeam);
        if(techLevel == 3 && uc.canResearchTechnology(Technology.WHEEL)) uc.researchTechnology(Technology.WHEEL);
    }

    private void calcIdealWorkers() {
        if(round > 25) {
            if (bigResources) {
                idealWorkers = 12;
            } else if (ecoMap) {
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

    private void senseEnemyUnits() {
        enemySpearmen = 0;
        enemyAxemen = 0;
        enemyTrappers = 0;
        enemyWolves = 0;
        UnitInfo[] enemies = uc.senseUnits(myTeam.getOpponent());
        for (int i = 0; i < enemies.length; i++) {
            if (!uc.isObstructed(baseLocation, enemies[i].getLocation())) {
                UnitType type = enemies[i].getType();
                if (type == UnitType.AXEMAN) enemyAxemen++;
                else if (type == UnitType.SPEARMAN) enemySpearmen++;
                else if (type == UnitType.TRAPPER) enemyTrappers++;
                else if (type == UnitType.WOLF) enemyWolves++;
            }
        }
    }

    private void senseAllyUnits() {
        allySpearmen = 0;
        allyAxemen = 0;
        allyTrappers = 0;
        allyWolves = 0;
        allyWorkers = 0;
        UnitInfo[] allies = uc.senseUnits(myTeam);
        for (int i = 0; i < allies.length; i++) {
            if (!uc.isObstructed(baseLocation, allies[i].getLocation())) {
                UnitType type = allies[i].getType();
                if (type == UnitType.AXEMAN) allyAxemen++;
                else if (type == UnitType.SPEARMAN) allySpearmen++;
                else if (type == UnitType.TRAPPER) allyTrappers++;
                else if (type == UnitType.WOLF) allyWolves++;
                else if (type == UnitType.WORKER) allyWorkers++;
            }
        }
    }

    private void senseInitialConditions() {
        senseInitialWater();
        senseInitialResources();
        senseSpaceAround();
    }

    private void senseInitialWater() {
        Location[] initialWaterTiles = uc.senseWater(uc.getType().getVisionRange());
        Location waterTile;
        for(int i=0; i<initialWaterTiles.length; i++) {
            waterTile = initialWaterTiles[i];
            /*if(uc.isAccessible(waterTile.add(waterTile.directionTo(baseLocation))))*/ waterTiles++;
        }
        if (waterTiles > 15) {
            hasWater = true;
            waterReady = false;
        }
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
        if (initialFood + initialWood + initialStone > 2000) {
            ecoMap = true;
            bigResources = true;
        } else if (initialFood + initialWood + initialStone > 1000) {
            ecoMap = true;
        }
    }

    private void senseSpaceAround() {
        for (Direction dir: safeSpawn) {
            if (uc.canSpawn(UnitType.WORKER, dir)) {
                spawnSpaces++;
            }
        }
        Direction[] cardinals = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        Location exploreLoc;

        int outOfMap = 0;
        for (int i = 0; i < 4; i++) {
            exploreLoc = baseLocation.add(cardinals[i]).add(cardinals[i]).add(cardinals[i]).add(cardinals[i]);
            if(uc.isOutOfMap(exploreLoc)) outOfMap++;
        }
        if (outOfMap >= 2) baseCorner = true;
    }

    void doSmokeStuffProducer() {
        if(smokeSignals.length > 0) {
            Location loc;
            int type;

            for (Smoke.smokeSignal smokeSignal : smokeSignals) {
                if (smokeSignal == null) continue;
                loc = smokeSignal.getLoc();
                type = smokeSignal.getType();

                if (type == constants.RUSH_ATTACK_ENCODING) {
                    enemyBase = loc;
                    if (enemyBase != null) {
                        move.setEnemyBase(enemyBase);
                        rushAttack = true;
                    }
                } else if (type == constants.ENEMY_BASE) {
                    enemyBase = loc;
                    barracksSmokeTurn = round;
                    if (enemyBase != null) {
                        move.setEnemyBase(enemyBase);
                    }
                } else if (type == constants.ENEMY_FOUND) {
                    rushAttack = true;
                } else if (type == constants.BARRACKS_BUILT) {
                    barracksBuilt = loc;
                    barracksSmokeTurn = round;
                } else if (type == constants.BARRACKS_ALIVE) {
                    barracksSmokeTurn = round;
                } else if (type == constants.WATER) {
                    hasWater = true;
                }
            }
        }
        if (barracksBuilt != null && barracksSmokeTurn + 55 < round) {
            barracksBuilt = null;
        }
    }
}
