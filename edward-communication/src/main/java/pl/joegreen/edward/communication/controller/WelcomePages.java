package pl.joegreen.edward.communication.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WelcomePages {
    @RequestMapping("/volunteer")
    public String volunteerIndex() {
        return "redirect:/volunteer/index.html";
    }
}
