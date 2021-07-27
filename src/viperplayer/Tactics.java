package viperplayer;

import aic2021.user.*;

public class Tactics {
    UnitController uc;

    public Tactics(UnitController uc) {
        this.uc = uc;
    }

    // Must receive an array of length > 0
    public UnitInfo getClosestUnobstructedEnemy(UnitInfo[] enemies) {
        UnitInfo closestEnemy = enemies[0];
        UnitInfo closestUnobstructed = enemies[0];

        int dist0 = 100000;
        boolean unobstructed = false;
        boolean allEnemiesObstructed = true;

        for (int i = 0; i < enemies.length; i++) {

            int dist = enemies[i].getLocation().distanceSquared(uc.getLocation());
            unobstructed = !uc.isObstructed(uc.getLocation(),enemies[i].getLocation());

            if (dist < dist0) {
                dist0 = dist;
                closestEnemy = enemies[i];
                if (unobstructed) {
                    closestUnobstructed = closestEnemy;
                    allEnemiesObstructed = false;
                }
            }
        }

        if (allEnemiesObstructed) {
            return closestEnemy;
        } else {
            return closestUnobstructed;
        }
    }
}
