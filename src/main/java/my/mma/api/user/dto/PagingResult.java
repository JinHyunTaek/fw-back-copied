package my.mma.api.user.dto;

import java.util.List;

public record PagingResult<T>(
        List<T> content,
        int number,
        boolean empty
) {
}

