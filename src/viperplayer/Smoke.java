package viperplayer;

import aic2021.user.Location;
import aic2021.user.UnitController;

public class Smoke {

    UnitController uc;
    public final Constants constants = new Constants();

    public Smoke(UnitController uc) {
        this.uc = uc;
    }

    Location decode(int signal) {
        int negative = signal % 10;
        if (negative != 0 && negative != 1 && negative != 2 && negative != 3) return null;
        signal = signal/10;
        int offsetY = signal%50;
        int offsetX = signal/50;
        if (negative == 1 || negative == 2) offsetX = -offsetX;
        if(negative == 1 || negative == 3) offsetY = -offsetY;
        return new Location(offsetX, offsetY);
    }

    int encodeLoc(int encoding, Location loc){
        Location offset = new Location(loc.x, loc.y);
        int negatives = 0;
        if(offset.x<0){
            offset.x = -offset.x;
            if(offset.y<0){
                offset.y = -offset.y;
                negatives = 1;
            }
            else negatives = 2;
        }
        else{
            if(offset.y<0){
                offset.y = -offset.y;
                negatives = 3;
            }
        }
        int drawing = (offset.x*50+offset.y)*10 + negatives;
        return drawing * encoding;
    }

    int encodeEnemyBaseLoc(int encoding, Location enemyBase, Location baseLocation){
        Location offset = new Location(baseLocation.x-enemyBase.x, baseLocation.y-enemyBase.y);
        int negatives = 0;
        if(offset.x<0){
            offset.x = -offset.x;
            if(offset.y<0){
                offset.y = -offset.y;
                negatives = 1;
            }
            else negatives = 2;
        }
        else{
            if(offset.y<0){
                offset.y = -offset.y;
                negatives = 3;
            }
        }
        int drawing = (offset.x*50+offset.y)*10 + negatives;
        return drawing * encoding;
    }

}
