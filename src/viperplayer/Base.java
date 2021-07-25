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
    boolean COINresearched = false;
    boolean BOXESresearched = false;
    boolean ROCK_ARTresearched = false;
    boolean JOBSresearched = false;
    boolean VOCABULARYresearched = false;
    boolean EUGENICSresearched = false;
    boolean SCHOOLSresearched = false;
    boolean isBaseClose = false;
    boolean MILITARY_TRAININGresearched = false;
    boolean enemyExplorer = false;
    Location baseLoc = null;
    Direction[] safeSpawn = new Direction[8];

    boolean rushAttack = false;

    void playRound(){
        if(uc.getRound() == 0) init();

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

    void senseBase(){
        UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
        if (units.length > 0) {
            isBaseClose = true;
            rushAttack = true;
            baseLoc = units[0].getLocation();
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
                if (target.distanceSquared(baseLoc) > range || uc.isObstructed(target, baseLoc)) {
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
                }
            }
        }
    }

    private void trySpawn(){
        /*if (trappers < 1) {
            if (spawnRandom(UnitType.TRAPPER)) ++trappers;
        }*/
        if(explorers < 1 && !rushAttack){
            if(spawnSafe(UnitType.EXPLORER)) ++explorers;
        }
        if (workers < 1){
            if (spawnSafe(UnitType.WORKER)) ++workers;
        }
    }

    private void tryResearch(){
        if(rushAttack && !MILITARY_TRAININGresearched){
            if(uc.canResearchTechnology(Technology.MILITARY_TRAINING)) {
                uc.researchTechnology(Technology.MILITARY_TRAINING);
                MILITARY_TRAININGresearched = true;
            }
        }
        else{
            researchWheel();
        }
    }

    private void researchWheel(){
        if(uc.canResearchTechnology(Technology.COIN)) {
            uc.researchTechnology(Technology.COIN);
            COINresearched = true;
        }
        else if(COINresearched && uc.canResearchTechnology(Technology.BOXES)) {
            uc.researchTechnology(Technology.BOXES);
            BOXESresearched = true;
        }
        else if(BOXESresearched && uc.canResearchTechnology(Technology.ROCK_ART)) {
            uc.researchTechnology(Technology.ROCK_ART);
            ROCK_ARTresearched = true;
        }
        else if(ROCK_ARTresearched && uc.canResearchTechnology(Technology.JOBS)) {
            uc.researchTechnology(Technology.JOBS);
            JOBSresearched = true;
        }
        else if(JOBSresearched && uc.canResearchTechnology(Technology.VOCABULARY)) {
            uc.researchTechnology(Technology.VOCABULARY);
            VOCABULARYresearched = true;
        }
        else if(VOCABULARYresearched && uc.canResearchTechnology(Technology.EUGENICS)) {
            uc.researchTechnology(Technology.EUGENICS);
            EUGENICSresearched = true;
        }
        else if(EUGENICSresearched && uc.canResearchTechnology(Technology.SCHOOLS)) {
            uc.researchTechnology(Technology.SCHOOLS);
            SCHOOLSresearched = true;
        }
        else if(uc.canResearchTechnology(Technology.WHEEL)) uc.researchTechnology(Technology.WHEEL);
    }

    private void senseInitialWater() {
        Location[] initialWaterTiles = uc.senseWater(uc.getType().getVisionRange());
        for(int i=0; i<initialWaterTiles.length; i++) {
            this.waterTiles++;
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
