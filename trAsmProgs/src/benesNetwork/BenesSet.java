package benesNetwork;

import java.util.List;
import java.util.ArrayList;

class BenesSet<X> {
    private final List<BenesCell<X>> cells = new ArrayList<BenesCell<X>>();
    private final int numCells;

    BenesSet ( final List<BenesCell<X>> cellInputs ) throws BenesSizeError {
        int i;
        numCells = cellInputs.size();

        if (numCells != (int) Math.pow(2,Math.round(Math.log(numCells)/Math.log(2)))) {
            throw new BenesSizeError("Cell input list must be of size 2^k, got "+Integer.toString(numCells));
        }

        for (i=0;i<numCells;++i) {
            final BenesCell<X> cell = new BenesCell<X>(cellInputs.get(i),cellInputs.get((i+numCells/2)%numCells));
            cells.add(cell);
        }
    }

    List<BenesCell<X>> outputs() { return cells; }

    void set ( final List<Boolean> settings ) throws BenesSizeError {
        if (settings.size() != numCells) {
            throw new BenesSizeError("Expected "+Integer.toString(numCells)+" switch settings, but got "+Integer.toString(settings.size()));
        }

        int i;
        for (i=0;i<numCells;++i) { cells.get(i).set(settings.get(i)); }
    }
}
