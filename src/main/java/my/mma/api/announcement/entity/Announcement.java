package my.mma.api.announcement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import my.mma.api.fighter.entity.BaseEntity;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@AllArgsConstructor(access = PRIVATE)
@Builder
public class Announcement extends BaseEntity {

    @Id
    @Column(name = "announcement_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private boolean pinned;

    public void updateAnnouncement(String title, String content, boolean pinned){
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }

}
