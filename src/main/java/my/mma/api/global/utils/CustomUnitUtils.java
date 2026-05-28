package my.mma.api.global.utils;

public class CustomUnitUtils {

    public static int toCentimeter(String footInch) {
        try {
            String[] split = footInch.split("'\\s*");
            if (split.length != 2)
                return 0;
            return (int) ((Integer.parseInt(split[0]) * 12 + Integer.parseInt(split[1])) * 2.54 + 0.5);
        } catch (Exception e) {
            return 0;
        }
    }

}
