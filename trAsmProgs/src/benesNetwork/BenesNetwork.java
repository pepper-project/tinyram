package benesNetwork;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import tinyram.TrCommon;

public class BenesNetwork<X> {
    private final List<BenesLayer<X>> layers = new ArrayList<BenesLayer<X>>();
    private final List<BenesCell<X>> inputs = new ArrayList<BenesCell<X>>();
    private final List<BenesCell<X>> cellOutputs;
    private final List<X> outputs = new ArrayList<X>();
    private final int numCells;     // cells per layer
    private final int numLayers;    // layers in the network
    private boolean needsUpdate = true;

    public BenesNetwork ( final List<X> networkInputs ) throws BenesSizeError {
        int i;
        numCells = networkInputs.size();
        final int logSize = (int) Math.round(Math.log(numCells)/Math.log(2));
        numLayers = 2*logSize - 1;  // butterfly + reversed butterfly, removing redundant middle layer

        if (numCells != (int) Math.pow(2,logSize)) {
            throw new BenesSizeError("Cell input list must be of size 2^k, got "+Integer.toString(numCells));
        }

        // encapsulate the input data
        for (i=0;i<numCells;++i) {
            final BenesCell<X> input = new BenesCell<X>(networkInputs.get(i));
            inputs.add(input);
            outputs.add(null);
        }

        List<BenesCell<X>> inter = inputs;
        for (i=0;i<numLayers;++i) {
            final int numSets = (int) Math.pow(2,i>numLayers/2 ? numLayers-1-i : i); // 0 1 2 1 0, 0 1 2 3 2 1 0, et cetera
            final BenesLayer<X> layer;

            layer = new BenesLayer<X>(inter,numSets);

            layers.add(layer);
            inter = layer.outputs();
        }
        cellOutputs = inter;        // final layer's outputs are the network's outputs
    }

    // if we need an update, do so, otherwise just return the outputs we've cached
    public List<X> outputs() {
        if (needsUpdate) {
            // chase pointers back through the network
            for (int i=0;i<numCells;++i) { outputs.set(i,cellOutputs.get(i).get()); }
            needsUpdate = false;
        }
        return outputs;
    }

    // stupid type erasure grrr
    public void set ( List<List<Boolean>> settings ) throws BenesSizeError {
        if (settings.size() != numLayers) {
            throw new BenesSizeError("Expected "+Integer.toString(numLayers)+" switch-lists, but got "+Integer.toString(settings.size()));
        }

        final List<Boolean> tmp = new ArrayList<Boolean>();
        for (int i=0;i<numLayers;++i) {
            final int numSets = (int) Math.pow(2,i>numLayers/2 ? numLayers-1-i : i); // 0 1 2 1 0, 0 1 2 3 2 1 0, et cetera
            tmp.addAll(expandBits(settings.get(i),numSets));
        }
        setList(tmp);
    }

    private void setList ( List<Boolean> settings ) throws BenesSizeError {
        if (settings.size() != numLayers * numCells) {
            throw new BenesSizeError("Expected "+Integer.toString(numLayers*numCells)+" switch settings, but got "+Integer.toString(settings.size()));
        }
        needsUpdate = true;

        int i;
        for (i=0;i<numLayers;++i) { layers.get(i).set(settings.subList(i*numCells,(i+1)*numCells));}
    }

    public void set ( final Integer... settings ) throws BenesSizeError { setInt(Arrays.asList(settings)); }

    public void setInt ( List<Integer> settings ) throws BenesSizeError {
        if (settings.size() != numLayers) {
            throw new BenesSizeError("Expected "+Integer.toString(numLayers)+" layer settings, but got "+Integer.toString(settings.size()));
        }

        final int nBitsPerLayer = numCells/2;
        final List<List<Boolean>> tmp = new ArrayList<List<Boolean>>();
        for (int i=0;i<numLayers;++i) { tmp.add(int2bools(settings.get(i),nBitsPerLayer)); }
        set(tmp);
    }

    // doesn't check its inputs; please be careful
    public static List<Boolean> expandBits ( final List<Boolean> bits , final int numSets ) {
        final List<Boolean> expanded = new ArrayList<Boolean>();

        final int len = bits.size();
        for (int i=0;i<numSets;++i) {
            expanded.addAll(bits.subList(i*len/numSets,(i+1)*len/numSets));   // add bit pattern twice
            expanded.addAll(bits.subList(i*len/numSets,(i+1)*len/numSets));
        }

        return expanded;
    }

    public static List<Boolean> int2bools ( final int value, final int len ) {
        final List<Boolean> bools = new ArrayList<Boolean>();
        final String str = TrCommon.int2zpBin(value,len);
        for (int i=0;i<str.length();++i) {
            if (str.charAt(i) == '1')
                bools.add(true);
            else
                bools.add(false);
        }
        return bools;
    }

    public String toString() {
        return outputs().toString();
    }
}
