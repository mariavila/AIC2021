package viperplayer;

import aic2021.user.*;

public class Base extends MyUnit {

    Base(UnitController uc){
        super(uc);
    }

    int workers = 0;
    int explorers = 0;
    int waterTiles = 0;
    Boolean COINresearched = false;
    Boolean BOXESresearched = false;
    Boolean ROCK_ARTresearched = false;
    Boolean JOBSresearched = false;
    Boolean VOCABULARYresearched = false;
    Boolean EUGENICSresearched = false;
    Boolean SCHOOLSresearched = false;

    void playRound(){
        if(uc.getRound() == 1) senseInitialWater();

        attack.genericTryAttack(uc.senseUnits(uc.getTeam().getOpponent()));
        trySpawn();
        tryResearch();
    }

    private void trySpawn(){
        if (workers < 1 && COINresearched){
            if (spawnRandom(UnitType.WORKER)) ++workers;
        }
    }

    private void tryResearch(){
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

}
