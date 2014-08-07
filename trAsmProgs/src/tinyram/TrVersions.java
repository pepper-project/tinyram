package tinyram;

public class TrVersions {
    public final static int numVers = 2;
    public final static String[] versions = { "tinyram.ver0", "tinyram.ver1" };

    public static void checkVersion ( final int version ) {
        if ( version < 0 || version >= numVers ) {
            throw new IllegalArgumentException("Version "+Integer.toString(version)+" is unimplemented.");
        }
    }

    public static Class getClass ( final int version, final String name ) {
        checkVersion(version);
        try {
            return Class.forName(versions[version]+name);
        } catch (Exception ex) {
            throw new RuntimeException(ex.toString());
        }
    }
}
