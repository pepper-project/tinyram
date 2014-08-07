package benesNetwork;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import tinyram.TrCommon;

public class BenesTest {
    public static void main(String[] args) throws Exception {
        // how long a list to sort?
        final int numElems;
        if (args.length > 0) {
            numElems = (int) Math.pow(2,TrCommon.bitLength(Integer.parseInt(args[0])));
        } else {
            numElems = 16384;
        }

        final int numRuns;
        if (args.length > 1) {
            final int tmp = Integer.parseInt(args[1]);
            if (tmp == 0) {
                numRuns = 100;
            } else {
                numRuns = tmp;
            }
        } else {
            numRuns = 100;
        }

        System.out.println(Integer.toString(numRuns) + " runs, " + Integer.toString(numElems) + " elements.");

        // benesRoute a bunch of lists
        final List<Long> slowRunTimes = new ArrayList<Long>(numRuns);
        final List<Long> runTimes = new ArrayList<Long>(numRuns);
        final Random rn = new Random((long) (Long.MAX_VALUE * Math.random()));
        // add a few extras to get the JIT going
        for (int i=0; i<numRuns; i++) {
            final List<Integer> inList = new ArrayList<Integer>(numElems);
            for (int j=0; j<numElems; j++) {
                inList.add(rn.nextInt());
            }

            final List<Integer> outList = new ArrayList<Integer>(inList);
            Collections.reverse(outList);

            final long startTime = System.currentTimeMillis();

            BenesRoute.benesRouteUnsafe(inList,outList);

            final long midTime = System.currentTimeMillis();

            BenesRoute.benesRoute(inList,outList);

            final long endTime = System.currentTimeMillis();
            
            runTimes.add(midTime - startTime);
            slowRunTimes.add(endTime - midTime);

            /*
            final BenesNetwork net = new BenesNetwork(inList);
            net.set(settings);

            final List<Integer> routed = net.outputs();

            for (int j=0; j<routed.size(); j++) {
                if (! routed.get(j).equals(outList.get(j))) {
                    System.out.println("Routing error at element " + Integer.toString(j) + " in run " + Integer.toString(i));
                    break;
                }
            }
            */
        }

        long tot = 0;
        long sTot = 0;
        long min = Long.MAX_VALUE;
        long sMin = Long.MAX_VALUE;
        long max = 0;
        long sMax = 0;
        for (int i=0; i<runTimes.size(); i++) {
            final Long l = runTimes.get(i);
            final Long s = slowRunTimes.get(i);
            System.out.print(l.toString() + " (" + s.toString() + ") ");
            tot += l;
            sTot += s;
            // first run will always trigger l > max, but because of JIT we expect this to be true
            if (l > max)
                max = l;
            else if (l < min)
                min = l; 

            if (s > sMax)
                sMax = s;
            else if (s < sMin)
                sMin = s;
        }
        System.out.println();
        System.out.println("Average: " + Double.toString(((double) tot) / ((double) numRuns)) + " " + Double.toString(((double) sTot) / ((double) numRuns)));
        System.out.println("Max: " + Long.toString(max) + " " + Long.toString(sMax));
        System.out.println("Min: " + Long.toString(min) + " " + Long.toString(sMin));

        return;
    }
}
