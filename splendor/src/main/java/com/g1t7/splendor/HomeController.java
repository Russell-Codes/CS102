package com.g1t7.splendor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")                // handles GET requests to the root
    public String home() {
        return "index";              // name of the view to render
    }
}