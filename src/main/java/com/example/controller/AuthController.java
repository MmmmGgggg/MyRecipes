package com.example.controller;

import com.example.model.User;
import com.example.repository.FavoriteRepository;
import com.example.repository.RecipeRepository;
import com.example.repository.UserRepository;
import com.example.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final RecipeRepository recipeRepo;
    private final FavoriteRepository favoriteRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepo, RecipeRepository recipeRepo,
                          FavoriteRepository favoriteRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.recipeRepo = recipeRepo;
        this.favoriteRepo = favoriteRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String name = body.get("name");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        if (userRepo.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        userRepo.save(user);

        String token = jwtUtil.generateToken(email);
        return ResponseEntity.ok(Map.of("token", token, "name", user.getName(), "email", email));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        return userRepo.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    String token = jwtUtil.generateToken(email);
                    return ResponseEntity.ok(Map.of("token", token, "name", user.getName(), "email", email));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid email or password")));
    }

    @DeleteMapping("/account")
    @Transactional
    public ResponseEntity<?> deleteAccount(Authentication auth) {
        return userRepo.findByEmail(auth.getName()).map(user -> {
            favoriteRepo.findByUserEmail(user.getEmail()).forEach(favoriteRepo::delete);
            recipeRepo.findByUserEmail(user.getEmail()).forEach(recipe -> {
                favoriteRepo.deleteByRecipeId(recipe.getId());
                recipeRepo.delete(recipe);
            });
            userRepo.delete(user);
            return ResponseEntity.ok(Map.of("message", "Account deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
