package my.mma.api.security.oauth2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JoinDto {

    @NotBlank
    @Size(min = 5, max = 20)
    private String email;

    @NotBlank
    @Size(min = 2, max = 10)
    private String username;

    @NotBlank
    @Size(min = 5, max = 20)
    private String password;

}
