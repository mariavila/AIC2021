package viperplayer;

import aic2021.user.UnitController;

public class Injection {

    public final UnitController uc;
    public final Move move;

    Injection (UnitController uc) {
        this.uc = uc;
        this.move = new Move(this);
    }
}