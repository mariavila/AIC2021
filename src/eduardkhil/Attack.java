package eduardkhil;

import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Attack {
    UnitController uc;
    UnitType myType;

    public Attack(UnitController uc) {
        this.uc = uc;
        this.myType = uc.getType();
    }

    public int getMyAttack() {
        return myType.getAttack();
    }

    public boolean canAttackTarget(Location target) {
        int distance = uc.getLocation().distanceSquared(target);
        if (myType.getAttackRange() < distance) return false;
        if (myType.getMinAttackRange() > distance) return false;
        return true;
    }

    public boolean genericTryAttack(UnitInfo[] units)  {
        if (!uc.canAttack()) return false;
        if (units.length == 0) return false;

        int myAttack = getMyAttack();

        UnitInfo bestTarget = null;
        int bestTargetHealth = 10000;
        Location bestLoc = null;
        UnitInfo killableTarget = null;
        int killableTargetHealth = 0;
        Location killableLoc = null;
        UnitInfo killableEnemy = null;
        int killableEnemyHealth = 0;
        Location killableEnemyLoc = null;
        UnitInfo bestEnemy = null;
        int bestTargetEnemy = 10000;
        Location bestEnemyLoc = null;

        for (UnitInfo unit : units) {
            Location target = unit.getLocation();

            if (uc.canAttack(target)) {
                int health = unit.getHealth();
                if (bestTargetHealth > health) {
                    bestTarget = unit;
                    bestTargetHealth = health;
                    bestLoc = target;
                }
                if (bestTargetEnemy < health) {
                    bestEnemy = unit;
                    bestTargetEnemy = health;
                    bestEnemyLoc = target;
                }
                if (myAttack >= health) {
                    if (killableTargetHealth < health) {
                        killableTarget = unit;
                        killableTargetHealth = health;
                        killableLoc = target;
                    }
                    if (killableEnemyHealth < health) {
                        killableEnemy = unit;
                        killableEnemyHealth = health;
                        killableEnemyLoc = target;
                    }
                }
            }
        }

        if (killableEnemy != null) {
            uc.attack(killableEnemyLoc);
            return true;
        } else if (bestEnemy != null) {
            uc.attack(bestEnemyLoc);
            return true;
        } else if (killableTarget != null) {
            uc.attack(killableLoc);
            return true;
        } else if (bestTarget != null) {
            uc.attack(bestLoc);
            return true;
        }

        return false;
    }
}
