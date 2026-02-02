package eg.mqzen.cardinal.util;

import java.util.UUID;

public class TypeUtils {

    public static boolean isUUID(String input) {
        try {
            UUID.fromString(input);
            return true;
        }catch (Exception ex) {
            return false;
        }
    }


}
