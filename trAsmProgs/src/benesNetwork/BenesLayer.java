package benesNetwork;

import java.util.List;
import java.util.ArrayList;

class BenesLayer<X> {
    private final List<BenesSet<X>> sets = new ArrayList<BenesSet<X>>();
    private final List<BenesCell<X>> layerOutputs = new ArrayList<BenesCell<X>>();
    private final int numCells; // total number of cells in all sets
    private final int numSets;  // number of sets

    BenesLayer ( final List<BenesCell<X>> layerInputs, final int numSets ) throws BenesSizeError {
        int i;
        numCells = layerInputs.size();
        this.numSets = numSets;
        final int logSize = (int) Math.round(Math.log(numCells)/Math.log(2));

        if (numCells != (int) Math.pow(2,logSize)) {
            throw new BenesSizeError("Cell input list must be of size 2^k, got "+Integer.toString(numCells));
        }
        if (numCells / numSets < 2) {
            throw new BenesSizeError("Refusing to create useless BenesSets with <2 nodes");
        }

        for (i=0;i<numSets;++i) {
            final List<BenesCell<X>> setInputs = layerInputs.subList(i*numCells/numSets,(i+1)*numCells/numSets);
            final BenesSet<X> set;

            set = new BenesSet<X>(setInputs);

            sets.add(set);
            layerOutputs.addAll(set.outputs());
        }
    }

    List<BenesCell<X>> outputs() { return layerOutputs; }

    void set ( final List<Boolean> settings ) throws BenesSizeError {
        if (settings.size() != numCells) {
            throw new BenesSizeError("Expected "+Integer.toString(numCells)+" switch settings, but got "+Integer.toString(settings.size()));
        }

        int i;
        for (i=0;i<numSets;++i) { sets.get(i).set(settings.subList(i*numCells/numSets,(i+1)*numCells/numSets)); }
    }
}
