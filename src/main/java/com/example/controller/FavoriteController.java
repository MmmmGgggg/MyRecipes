package com.example.controller;

import com.example.model.Favorite;
import com.example.model.Recipe;
import com.example.repository.FavoriteRepository;
import com.example.repository.RecipeRepository;
import com.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteRepository favoriteRepo;
    private final RecipeRepository recipeRepo;
    private final UserRepository userRepo;

    public FavoriteController(FavoriteRepository favoriteRepo, RecipeRepository recipeRepo,
                              UserRepository userRepo) {
        this.favoriteRepo = favoriteRepo;
        this.recipeRepo = recipeRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<Recipe> getMyFavorites(Authentication auth) {
        return favoriteRepo.findByUserEmail(auth.getName()).stream()
                .map(Favorite::getRecipe)
                .toList();
    }

    @GetMapping("/{recipeId}/check")
    public boolean isFavorite(@PathVariable Long recipeId, Authentication auth) {
        return favoriteRepo.existsByUserEmailAndRecipeId(auth.getName(), recipeId);
    }

    @PostMapping("/{recipeId}")
    public ResponseEntity<?> addFavorite(@PathVariable Long recipeId, Authentication auth) {
        if (favoriteRepo.existsByUserEmailAndRecipeId(auth.getName(), recipeId)) {
            return ResponseEntity.ok().build();
        }
        return recipeRepo.findById(recipeId).map(recipe ->
            userRepo.findByEmail(auth.getName()).map(user -> {
                Favorite fav = new Favorite();
                fav.setUser(user);
                fav.setRecipe(recipe);
                favoriteRepo.save(fav);
                return ResponseEntity.ok().build();
            }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{recipeId}")
    public ResponseEntity<?> removeFavorite(@PathVariable Long recipeId, Authentication auth) {
        favoriteRepo.findByUserEmailAndRecipeId(auth.getName(), recipeId)
                .ifPresent(favoriteRepo::delete);
        return ResponseEntity.noContent().build();
    }
}
