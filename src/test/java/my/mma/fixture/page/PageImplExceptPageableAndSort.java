package my.mma.fixture.page;

import java.util.List;

public record PageImplExceptPageableAndSort<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        boolean last,
        int size,
        int number,
        boolean first,
        long numberOfElements,
        boolean empty
) {
}
