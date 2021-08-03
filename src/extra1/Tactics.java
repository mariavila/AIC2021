package extra1;

import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Tactics {
    UnitController uc;

    public Tactics(UnitController uc) {
        this.uc = uc;
    }

    // Must receive an array of length > 0
    public UnitInfo getClosestDangerousEnemy(UnitInfo[] enemies, UnitType[] excluded) {
        UnitInfo closestEnemy = null;
        UnitInfo closestUnobstructed = null;

        int dist0 = 100000;
        boolean unobstructed;
        boolean allEnemiesObstructed = true;

        outerloop:
        for (int i = 0; i < enemies.length; i++) {
            for (int j = 0; j < excluded.length; j++) {
                if (enemies[i].getType() == excluded[j]) continue outerloop;
            }

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
