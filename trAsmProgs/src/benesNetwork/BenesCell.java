package benesNetwork;

class BenesCell<X> {
    private X value = null;
    private final BenesCell<X> in0;
    private final BenesCell<X> in1;
    private BenesCell<X> out;

    BenesCell ( final BenesCell<X> in0, final BenesCell<X> in1 ) {
        this.value = null;
        this.in0 = in0;
        this.in1 = in1;
        this.out = null;
    }

    BenesCell ( final X value ) {
        this.value = value;
        this.in0 = null;
        this.in1 = null;
        this.out = null;
    }

    X get () {
        if (null != value) {
            return value;
        } else if (null != out) {
            return out.get();
        } else {
            return null;
        }
    }

    void set (boolean set1) {
        if (set1) { out = in1; }
        else      { out = in0; }
    }

    void set (X value) {
        this.value = value;
    }
}
