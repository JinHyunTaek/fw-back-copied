package my.mma.admin.web.home.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/htj-admin/dashboard")
@RequiredArgsConstructor
public class AdminHomeController {

    @GetMapping("")
    public String home(Model model){
        model.addAttribute("activeMenu", "dashboard");
        return "admin/index";
    }

}
