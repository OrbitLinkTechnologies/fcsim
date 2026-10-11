package fc.sim;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
class SimController {

    @GetMapping("/workbench")
    public String workbench(Model model) {
        model.addAttribute("workbench", SimApplication.workbench);
        model.addAttribute("robotRunner", SimApplication.robotRunner);
        return "workbench";
    }

}
