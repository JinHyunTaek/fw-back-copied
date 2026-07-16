package my.mma.api.home.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.home.dto.HomeScreenDto;
import my.mma.api.home.service.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/home")
public class HomeController {

    private final HomeService homeService;

    /**
     * @return HomeScreenDto? (nullable return value)
     */
    @GetMapping("")
    public ResponseEntity<HomeScreenDto> home(){
        return ResponseEntity.ok().body(homeService.home());
    }

}
