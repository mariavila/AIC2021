package viperplayer;

import aic2021.user.*;

public class Base extends MyUnit {

    int workers = 0;
    int explorers = 0;
    int waterTiles = 0;

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        research();

        if(uc.getRound() == 1) senseInitialWater();

        attack.genericTryAttack();

        if (workers < 5){
            //if (spawnRandom(UnitType.WORKER)) ++workers;
        }
        if (explorers < 1 && workers > 2) {
            //if (spawnRandom(UnitType.EXPLORER)) ++explorers;
        }
    }

    private void research(){
        if(uc.canResearchTechnology(Technology.COIN)) uc.researchTechnology(Technology.COIN);
        else if(uc.canResearchTechnology(Technology.BOXES)) uc.researchTechnology(Technology.BOXES);
        else if(uc.canResearchTechnology(Technology.UTENSILS)) uc.researchTechnology(Technology.UTENSILS);
        else if(uc.canResearchTechnology(Technology.JOBS)) uc.researchTechnology(Technology.JOBS);
        else if(uc.canResearchTechnology(Technology.VOCABULARY)) uc.researchTechnology(Technology.VOCABULARY);
        else if(uc.canResearchTechnology(Technology.EUGENICS)) uc.researchTechnology(Technology.EUGENICS);
        else if(uc.canResearchTechnology(Technology.SCHOOLS)) uc.researchTechnology(Technology.SCHOOLS);
        else if(uc.canResearchTechnology(Technology.WHEEL)) uc.researchTechnology(Technology.WHEEL);
    }

    private void senseInitialWater() {
        Location[] initialWaterTiles = uc.senseWater(uc.getType().getVisionRange());
        for(int i=0; i<initialWaterTiles.length; i++) {
            this.waterTiles++;
        }
    }

}
