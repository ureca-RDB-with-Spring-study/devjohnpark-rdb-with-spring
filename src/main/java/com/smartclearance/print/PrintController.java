package com.smartclearance.print;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrintController {

    @GetMapping("/api/print")
    public String print() {
        System.out.println("print");
        return "print";
    }
}
