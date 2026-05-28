package my.mma.api.status.dto;

public record AppStatusResponse(String minVersion, String latestVersion) {

    public static AppStatusResponse of(String minVersion, String latestVersion){
        return new AppStatusResponse(minVersion, latestVersion);
    }

}
