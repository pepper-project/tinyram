package tinyram;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class TrObjectReader {
    public static TrCommon.Tuple<TrProgram,List<TrTapeStore>> getInputs (final List<String> args) {
        return readStreams(args,openStreams(args));
    }

    public static List<ObjectInputStream> openStreams (final List<String> args) {
        final List<ObjectInputStream> inputs = new ArrayList<ObjectInputStream>();

        if (null != args && 0 < args.size())
            for (int i=0; i<args.size(); ++i) {
                final ObjectInputStream inp;
                try { inp = new ObjectInputStream(new FileInputStream(args.get(i))); }
                catch (IOException ex) {
                    TrCommon.err("Opening "+args.get(i)+": "+ex.toString()+"\nContinuing.");
                    continue;
                }
                inputs.add(inp);
            }
        else {
            try { inputs.add(new ObjectInputStream(System.in)); }
            catch (IOException ex) {
                TrCommon.err("Creating input stream from STDIN: "+ex.toString());
            }
        }

        return inputs;
    }

    public static TrCommon.Tuple<TrProgram,List<TrTapeStore>> readStreams (final List<String> args, final List<ObjectInputStream> inputs) {
        TrProgram prog = null;
        final List<TrTapeStore> tapeStores = new ArrayList<TrTapeStore>();

        for (int i=0; i<inputs.size(); ++i) {
            final ObjectInputStream input = inputs.get(i);
            try {
                while (true) {
                    Object inObj = input.readObject();

                    if (inObj.getClass() == tinyram.TrProgram.class) {
                        if (null == prog) {
                            prog = (TrProgram) inObj;
                            prog.filename = (null != args && i < args.size()) ? args.get(i) : "<unknown>";
                        } else {
                            TrCommon.warn("Ignoring all but the first program.");
                        }
                    } else if (inObj.getClass() == tinyram.TrTapeStore.class) {
                        ((TrTapeStore) inObj).filename = (null != args && i < args.size()) ? args.get(i) : "<unknown>";
                        tapeStores.add((TrTapeStore) inObj);
                    } else if (null != args && i < args.size()) {
                        TrCommon.warn("Unexpected object found in "+args.get(i)+" ("+inObj.getClass().toString()+"). Continuing.");
                    } else {
                        TrCommon.warn("Unexpected object found in STDIN ("+inObj.getClass().toString()+"). Continuing.");
                    }
                }} catch (EOFException ex) { // hit end of file. No need to report this, it's expected
                    try { if (null != input) input.close(); }
                    catch (IOException iex) { ; } // if we fail to close just ignore it
                }  catch (ClassNotFoundException|IOException ex) { // report other errors
                    try { if (null != input) input.close(); }
                    catch (IOException iex) { ; } // if we fail to close just ignore it
                    if (null != args && i < args.size())
                        TrCommon.warn("Exception while reading "+args.get(i)+": "+ex.toString()+"\nContinuing.");
                    else
                        TrCommon.warn("Exception while reading STDIN: "+ex.toString()+"\nContinuing.");
                }
        }
        return new TrCommon.Tuple<TrProgram,List<TrTapeStore>>(prog, tapeStores);
    }
}
