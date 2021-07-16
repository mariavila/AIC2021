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

        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
        Location baseLoc = uc.getLocation();

        baseAttack(enemies, baseLoc);

        if (workers < 5){
            if (spawnRandom(UnitType.WORKER)) ++workers;
        }
        if (explorers < 1 && workers > 2) {
            if (spawnRandom(UnitType.EXPLORER)) ++explorers;
        }
    }

    private void baseAttack(UnitInfo[] enemies, Location baseLoc){

        for (int i = 0; i < enemies.length; i++) {
            Location locEnemy = enemies[i].getLocation();

            if (baseLoc.distanceSquared(locEnemy) <= UnitType.BASE.attackRange) {
                if (uc.canAttack(locEnemy)) uc.attack(locEnemy);
            }
        }
    }

    private void research(){
        if(uc.canResearchTechnology(Technology.COIN)) uc.researchTechnology(Technology.COIN);
        else if(uc.canResearchTechnology(Technology.RAFTS) && this.waterTiles > 4) uc.researchTechnology(Technology.RAFTS);
        else if(uc.canResearchTechnology(Technology.BOXES)) uc.researchTechnology(Technology.BOXES);
        else if(uc.canResearchTechnology(Technology.UTENSILS)) uc.researchTechnology(Technology.UTENSILS);
        else if(uc.canResearchTechnology(Technology.EUGENICS)) uc.researchTechnology(Technology.EUGENICS);
        else if(uc.canResearchTechnology(Technology.VOCABULARY)) uc.researchTechnology(Technology.VOCABULARY);
        else if(uc.canResearchTechnology(Technology.JOBS)) uc.researchTechnology(Technology.JOBS);
        else if(uc.canResearchTechnology(Technology.POISON)) uc.researchTechnology(Technology.POISON);
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
