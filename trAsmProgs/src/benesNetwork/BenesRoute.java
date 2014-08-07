package benesNetwork;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;
import tinyram.TrCommon.Tuple;
import tinyram.TrCommon;

public class BenesRoute { 
    /*
     * Benes network routing algorithm.
     *
     * Given: `to` is a permutation of `from`; lists are of equal size, 2^k, k>0
     *
     * Base case: 2-element list. If `from` and `to` have the same order, return [[false]], else return [[true]]
     *
     * Recursive case: Remove first and last layer from Benes network.
     * These are BenesSets (see BenesSet.java) of size 2^k, with 2^(k-1) switch settings.
     * Between first and last layer are two Benes networks of size 2^(k-1), "upper" and "lower".
     *
     * (Note that any position in the `from` and `to` list can be reached from either the upper or lower Benes
     * network by selecting output position and switch setting. Thus, we can choose to route any input through
     * either the upper or lower network by choosing the spot at which it exits and setting the switches properly.)
     *
     * Choose first input; route it through the upper Benes network, choosing its output slot from the network
     * to satisfy the constraint imposed by its position in the `to` list.
     *
     * Now, choose the butterfly partner of the output (that is, the signal that shares a switch setting in
     * the output butterfly network with the signal routed above). This signal is necessarily routed through
     * the lower Benes network by the switch setting we chose when routing the previous signal.
     *
     * Continue propagating constraints in a circular fashion, routing a signal in one direction and then
     * routing its butterfly partner backward. If this cycle ends and there are no more entries in the input
     * list, you're done. Otherwise, choose another signal and route it, always choosing to route forward
     * through the top network and backward through the bottom one.
     *
     * The result of this operation is two sets of from-to pairs, each half the size of the original, which
     * must be routed on two Benes networks each of half the original size. Recursively apply routing algorithm,
     * then combine the resulting switch settings and bundle them up with the input and output network settings
     * we got from routing the outer BenesSets. This is our result.
     *
     */

    public static <X> List<List<Boolean>> benesRoute(final List<X> from, final List<X> to) throws BenesRouteError {
        // enforce equal size
        if (from.size() != to.size()) {
            throw new BenesRouteError("From and To must have the same number of elements");
        }

        // enforce permutability
        // this is *much* faster than containsAll
        final Map<X,Integer> permMap = new HashMap<X,Integer>(2*from.size());
        for (X f : from) {
            final Integer n = permMap.get(f);
            if (null == n) {
                permMap.put(f,1);
            } else {
                permMap.put(f,n+1);
            }
        }
        for (X t : to) {
            final Integer n = permMap.get(t);
            if ((null == n) || n.equals(0)) {
                throw new BenesRouteError("To must be a permutation of From");
            } else if (n.equals(1)) {
                permMap.remove(t);
            } else {
                permMap.put(t,n-1);
            }
        }
        if (! permMap.isEmpty()) {
            throw new BenesRouteError("To must be a permutation of From");
        }

        // nulls in the input lists will confuse the routing algorithm.
        if (from.contains(null) || to.contains(null)) {
            throw new BenesRouteError("Cannot route lists containing null values. Use another placeholder.");
        }

        final int logSize = TrCommon.bitLength(from.size());
        // enforce power of 2 size
        if ( (from.size() != (int) Math.pow(2,logSize)) || (logSize < 1) ) {
            throw new BenesRouteError("Must route 2^k elements, k>0");
        }

        return benesRouteUnsafe(from,to);
    }

    // skip checks; this is a huge time savings because containsAll is very costly
    public static <X> List<List<Boolean>> benesRouteUnsafe(final List<X> from, final List<X> to) {
        final int logSize = TrCommon.bitLength(from.size());
        final List<List<Boolean>> settings = new ArrayList<List<Boolean>>(2*logSize - 1);
        for (int i=0; i<2*logSize - 1; i++) {
            settings.add(Arrays.asList(new Boolean[from.size() / 2]));
        }

        route(new ArrayList<X>(from), new ArrayList<X>(to), settings);

        return settings;
    }

    // NOTE! Contents of from and to are modified by this routine
    private static <X> void route(List<X> from, List<X> to, List<List<Boolean>> settings) {
        final List<Boolean> inSettings = settings.get(0);

        // base case: route a 2-size network
        if (2 == from.size()) {
            if (from.get(0).equals(to.get(0))) {
                inSettings.set(0,false);        // don't need to criss-cross this switch
            } else {
                inSettings.set(0,true);         // need to criss-cross this switch
            }

            return;
        }

        final List<Boolean> outSettings = settings.get(settings.size() - 1);

        // output of the from-network, input of the to-network
        final List<X> fromOut = Arrays.asList((X[]) new Object[from.size()]);
        final List<X> toIn = Arrays.asList((X[]) new Object[from.size()]);

        final Map<X,Integer> fromMap = new HashMap<X,Integer>(2*from.size());
        final Map<X,Integer> toMap = new HashMap<X,Integer>(2*from.size());
        // candidate deque
        final Deque<Integer> routeQ = new ArrayDeque<Integer>(from.size());
        for (int i=0; i<from.size(); i++) {
            fromMap.put(from.get(i),i);
            toMap.put(to.get(i),i);
            routeQ.add(i);
        }

        // once we've routed every candidate, we're done
rLoop:  while (routeQ.size() > 0) {
            // take next item from routing queue
            int nextRoute = routeQ.pop();
            // if it's already been routed, find the next one that hasn't
            while (null == from.get(nextRoute)) {
                if (routeQ.size() > 0) {
                    nextRoute = routeQ.pop();
                } else {
                    break rLoop;
                }
            }

            while (-1 != nextRoute) {
                nextRoute = routeForward(from,to,fromOut,toIn,inSettings,outSettings,from.size()/2,nextRoute,fromMap,toMap);
            }
        }

        // now we have fromOut and toIn, so we recursively route them
        final int recSize = fromOut.size()/2;
        final Tuple<List<List<Boolean>>,List<List<Boolean>>> nextSets = splitSettings(settings, recSize/2);
        route(fromOut.subList(0,recSize),toIn.subList(0,recSize),nextSets.fst);
        route(fromOut.subList(recSize,2*recSize),toIn.subList(recSize,2*recSize),nextSets.snd);

        return;
    }

    private static Tuple<List<List<Boolean>>,List<List<Boolean>>> splitSettings(List<List<Boolean>> settings, int recSize) {
        final List<List<Boolean>> fst = new ArrayList<List<Boolean>>(settings.size() - 2);
        final List<List<Boolean>> snd = new ArrayList<List<Boolean>>(settings.size() - 2);
        final Tuple<List<List<Boolean>>,List<List<Boolean>>> rTup = new Tuple(fst, snd);

        for (int i=1; i<settings.size()-1; i++) {
            final List<Boolean> fTmp = settings.get(i).subList(0,recSize);
            final List<Boolean> sTmp = settings.get(i).subList(recSize,2*recSize);

            fst.add(fTmp);
            snd.add(sTmp);
        }

        return rTup;
    }

    private static <X> int routeForward(List<X> from, List<X> to, List<X> fromOut, List<X> toIn, List<Boolean> inSettings, List<Boolean> outSettings, int halfSize, int elm, Map<X,Integer> fromMap, Map<X,Integer> toMap) {
        final int inPos = elm % halfSize;

        // where is it going?
        // normally we can use a hash, but if we don't have unique
        // elements in from and to then we will get collisions, which
        // we resolve with indexOf instead
        final X targetElm = from.get(elm);
        Integer tmp = toMap.get(targetElm);
        if (null == tmp) {
            tmp = to.indexOf(targetElm);
        } else {
            toMap.remove(targetElm);
        }
        final int elmO = tmp.intValue();
        final int outPos = elmO % halfSize;

        // in switch setting: if elm is in the bottom half of the input list, criss-cross wires
        if (elm >= halfSize) { // e.g., size=4, so 0,1 are in top half, 2,3 are in bottom half
            assert ! ((inSettings.get(inPos) != null) && !inSettings.get(inPos)) :
                "Inconsistency found in-fwd-routing "+Integer.toString(elm);
            inSettings.set(inPos,true);
        } else { // otherwise, straight through
            assert ! ((inSettings.get(inPos) != null) && inSettings.get(inPos)) :
                "Inconsistency found in-fwd-routing "+Integer.toString(elm);
            inSettings.set(inPos,false);
        }

        // move the element from the from list to the fromOut list
        fromOut.set(inPos,from.get(elm));
        from.set(elm,null);

        // out switch setting: if elm is in bottom half of output list, criss-cross wires
        if (elmO >= halfSize) {
            assert ! ((outSettings.get(outPos) != null) && !outSettings.get(outPos)) :
                "Inconsistency found out-fwd-routing "+Integer.toString(elmO);
            outSettings.set(outPos,true);
        } else {
            assert ! ((outSettings.get(outPos) != null) && outSettings.get(outPos)) :
                "Inconsistency found out-fwd-routing "+Integer.toString(elmO);
            outSettings.set(outPos,false);
        }

        // move the element from the to list to the toIn list
        toIn.set(outPos,to.get(elmO));
        to.set(elmO,null);

        // check if the butterfly partner is already routed; if not, route it backward
        final int bPart = (elmO + halfSize)%(2*halfSize);
        if (null != to.get(bPart)) {
            return routeBackward(from,to,fromOut,toIn,inSettings,outSettings,halfSize,bPart,fromMap,toMap);
        } else {
            return -1;
        }
    }

    private static <X> int routeBackward(List<X> from, List<X> to, List<X> fromOut, List<X> toIn, List<Boolean> inSettings, List<Boolean> outSettings, int halfSize, int elm, Map<X,Integer> fromMap, Map<X,Integer> toMap) {
        final int outPos = elm % halfSize;

        // where is it going?
        // see above: we use a hash if possible, but we have
        // to handle collisions (e.g., duplicate input elements)
        // by falling back to linear search
        final X targetElm = to.get(elm);
        Integer tmp = fromMap.get(targetElm);
        if (null == tmp) {
            tmp = from.indexOf(targetElm);
        } else {
            fromMap.remove(targetElm);
        }
        final int elmI = tmp.intValue();
        final int inPos = elmI % halfSize;

        // out switch setting: if elm is in the bottom half of the input list, straight through
        if (elm >= halfSize) {
            assert ! ((outSettings.get(outPos) != null) && outSettings.get(outPos)) :
                "Inconsistency found out-back-routing "+Integer.toString(elm);
            outSettings.set(outPos,false);
        } else {
            assert ! ((outSettings.get(outPos) != null) && !outSettings.get(outPos)) :
                "Inconsistency found out-back-routing "+Integer.toString(elm);
            outSettings.set(outPos,true);
        }

        // move the element from the to list to the toIn list
        toIn.set(outPos+halfSize,to.get(elm));
        to.set(elm,null);

        // in switch setting: if elm is in the bottom half of the input list, straight through
        if (elmI >= halfSize) {
            assert ! ((inSettings.get(inPos) != null) && inSettings.get(inPos)) :
                "Inconsistency found in-back-routing "+Integer.toString(elm);
            inSettings.set(inPos,false);
        } else {
            assert ! ((inSettings.get(inPos) != null) && !inSettings.get(inPos)) :
                "Inconsistency found in-back-routing "+Integer.toString(elm);
            inSettings.set(inPos,true);
        }

        // move element from the from list to the fromOut list
        fromOut.set(inPos+halfSize,from.get(elmI));
        from.set(elmI,null);

        // check if the butterfly partner is already routed; if not, route it forward
        final int bPart = (elmI + halfSize)%(2*halfSize);
        // java does not support tail recursion, so don't recurse forever. Instead, use while() in route() as a trampoline
        if (null != from.get(bPart)) {
            return bPart;
        } else {
            return -1;
        }
    }

// **
// these are just for testing the routing algorithm
// **
    public static <X> List<X> perm(List<X> in, long pNum) {
        final int n = in.size();
        final List<Long> facts = new ArrayList<Long>(n);
        final List<Long> quots = new ArrayList<Long>(n);
        final List<X> out = new ArrayList<X>(n);

        facts.add(1L);
        for (long i=1;i<n;++i) { facts.add(i*facts.get(((int) i)-1)); }

        for (int i=0;i<n;++i) { // long division by the factorials
            quots.add(pNum / facts.get(n-1-i));
            pNum = pNum % facts.get(n-1-i);
        }

        // now we have to adjust each long division to include
        // results below it in the long division table <= this result
        for (int i=n-1;i>0;--i)
        for (int j=i-1;j>=0;--j)
        if (quots.get(i) >= quots.get(j))
            quots.set(i,quots.get(i)+1L);

        for (long quot : quots)
            out.add(in.get((int) quot));

        return out;
    }

    public static <X> List<List<X>> permute (List<X> in) {
        final List<List<X>> out = new ArrayList<List<X>>(); // output list-of-lists
        final List<X> inT = new ArrayList<X>(in);           // create copy of input list
        permuteHeap (inT.size(), inT, out);                 // B. Heap's algorithm
        return out;
    }

    public static <X> void permuteHeap(int n, List<X> in, List<List<X>> out) {
        if (1 == n) {
            out.add(new ArrayList<X>(in));  // one output permutation on the stack
            return;
        }
        for (int i=0;i<n;++i) {
            permuteHeap(n-1,in,out);
            if ((n%2) == 1)
                Collections.swap(in,0,n-1);
            else
                Collections.swap(in,i,n-1);
        }
    }

    public static List<Integer> fromTo (int from, int to) {
        final List<Integer> out = new ArrayList<Integer>(to-from);
        for (int i=from;i<to;++i) { out.add(i); }
        return out;
    }
}
