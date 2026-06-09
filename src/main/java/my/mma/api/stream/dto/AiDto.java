package my.mma.api.stream.dto;

public record AiDto(

) {
    public record AiAskRequest(String question){

    }

    public record AiChunkResponse(String answer){

    }
}
