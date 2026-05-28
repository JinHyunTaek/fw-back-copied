package my.mma.api.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private boolean first;
    private boolean last;
    private int number;
    private List<T> content;
    private int page;
    private int size;
    private int totalPages;
    private boolean empty;

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                new ArrayList<>(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.isEmpty()
        );
    }
}