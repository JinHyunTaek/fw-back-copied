package my.mma.admin.web.controller.fighter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.dto.fighter.AdminFighterResponseForUpdate;
import my.mma.admin.web.dto.fighter.AdminFighterUpdateRequest;
import my.mma.admin.web.service.fighter.AdminFighterService;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fighter.entity.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@Controller
@Slf4j
@RequestMapping("/htj-admin/fighter")
@RequiredArgsConstructor
public class AdminFighterController {

    private final AdminFighterService fighterService;

    @GetMapping("/fighters")
    public String fighters(@RequestParam(name = "name", defaultValue = "") String name,
                           @PageableDefault Pageable pageable,
                           Model model){
        model.addAttribute("page", fighterService.search(name, pageable));
        model.addAttribute("name", name);
        model.addAttribute("activeMenu", "fighter");
        return "admin/fighter/fighters";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model){
        AdminFighterResponseForUpdate fighter = fighterService.detail(id);
        model.addAttribute("fighter", fighter);
        model.addAttribute("form", AdminFighterUpdateRequest.toDto(fighter));
        model.addAttribute("nationalities", Country.values());
        return "admin/fighter/fighter_detail";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Validated @ModelAttribute("form") AdminFighterUpdateRequest request,
                         BindingResult bindingResult, Model model){
        if(bindingResult.hasErrors()){
            AdminFighterResponseForUpdate fighter = fighterService.detail(id);
            model.addAttribute("fighter", fighter);
            model.addAttribute("form", AdminFighterUpdateRequest.toDto(fighter));
            model.addAttribute("nationalities", Country.values());
            return "admin/fighter/fighter_detail";
        }
        fighterService.updateFighter(id, request);
        return "redirect:/htj-admin/fighter/" + id;
    }

    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<Page<FighterDto>> searchJson(
            @RequestParam(defaultValue = "") String name,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(fighterService.search(name, pageable));
    }

    @PostMapping("/update_ranking")
    public String updateRanking() {
        fighterService.updateRanking();
        return "redirect:/htj-admin/update-buttons";
    }

//    @PostMapping("/image")
//    public String updateImage(
//            @RequestParam("name") String name,
//            @RequestParam("nameUrl") String nameUrl
//    ) {
//        fighterService.updateImage(name, nameUrl);
//        return "redirect:/htj-admin/fighter/fighters";
//    }

}
