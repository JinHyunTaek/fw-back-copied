package my.mma.api.status.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AppStatus {

    @Id
    @Column(name = "app_status_id")
    private Long id;

    @Column(nullable = false)
    private String minVersion;

    @Column(nullable = false)
    private String latestVersion;

    public void update(String minVersion, String latestVersion){
        this.minVersion = minVersion;
        this.latestVersion = latestVersion;
    }

}
