package my.mma.api.status.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.status.entity.AppStatus;
import my.mma.api.status.repository.AppStatusRepository;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppStatusQueryService {

    private final AppStatusRepository appStatusRepository;

    @Cacheable(value = "appStatus")
    public @NonNull AppStatus getAppStatus() {
        return appStatusRepository.findById(1L).orElseThrow(
                () -> new CustomException(ErrorCode.SERVER_ERROR_500));
    }

}
