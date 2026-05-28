package my.mma.api.global.utils;

public class ModifyUtils {

    public static double toKg(String pound){
        return Math.round(Double.parseDouble(pound)*0.453592*10)/10.0;
    }

}
