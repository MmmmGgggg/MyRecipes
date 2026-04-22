package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return """
                <html>
                <head><title>My Java App</title></head>
                <body>
                    <h1>Welcome to My Java Web App!</h1>
                    <p>This is a Spring Boot application.</p>
                    <ul>
                        <li><a href="/hello">Say Hello</a></li>
                        <li><a href="/time">Current Time</a></li>
                    </ul>
                </body>
                </html>
                """;
    }

    @GetMapping("/hello")
    public String hello() {
        return "<h1>Hello, World!</h1><a href='/'>Back</a>";
    }

    @GetMapping("/time")
    public String time() {
        return "<h1>Current Time: " + java.time.LocalDateTime.now() + "</h1><a href='/'>Back</a>";
    }
}
