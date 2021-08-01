package viperplayer;

import aic2021.user.Location;
import aic2021.user.UnitController;

public class Smoke {

    UnitController uc;
    public final Constants constants = new Constants();

    public Smoke(UnitController uc) {
        this.uc = uc;
    }

    class smokeSignal {
        Location loc;
        int type;

        smokeSignal(Location loc, int type) {
            this.loc = loc;
            this.type = type;
        }

        Location getLoc() {
            return loc;
        }

        int getType() {
            return type;
        }
    }

    smokeSignal decodeSignal(boolean encoded, int signal){
        int encoding;
        if(!encoded) decode(signal);
        else if(signal % constants.RUSH_ATTACK_ENCODING == 0){
            encoding = constants.RUSH_ATTACK_ENCODING;
            signal = signal / encoding;
            Location smokeLoc = decode(signal);
            if (smokeLoc != null) return new smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.ENEMY_FOUND == 0){
            encoding = constants.ENEMY_FOUND;
            signal = signal / encoding;
            Location smokeLoc = decode(signal);
            if (smokeLoc != null) return new smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.BARRACKS_BUILT == 0){
            encoding = constants.BARRACKS_BUILT;
            signal = signal / encoding;
            Location smokeLoc = decode(signal);
            if (smokeLoc != null) return new smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.ENEMY_BASE == 0){
            encoding = constants.ENEMY_BASE;
            signal = signal / encoding;
            Location smokeLoc = decode(signal);
            if (smokeLoc != null) return new smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.BARRACKS_ALIVE == 0){
            encoding = constants.BARRACKS_ALIVE;
            signal = signal / encoding;
            Location smokeLoc = decode(signal);
            if (smokeLoc != null) return new smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.ECO_MAP == 0){
            encoding = constants.ECO_MAP;
            signal = signal / encoding;
            Location smokeLoc = decode(signal);
            if (smokeLoc != null) return new smokeSignal(smokeLoc, encoding);
        } else if(signal % constants.SETTLEMENT == 0){
            encoding = constants.SETTLEMENT;
            signal = signal / encoding;
            Location smokeLoc = decode(signal);
            if (smokeLoc != null) return new smokeSignal(smokeLoc, encoding);
        }
        return null;
    }

    Location decode(int signal) {
        int posY = signal%1051;
        int posX = signal/1051;
        return new Location(posX, posY);
    }

    int encode(int encoding, Location loc){
        int drawing = loc.x*1051+loc.y;
        return drawing * encoding;
    }
}
