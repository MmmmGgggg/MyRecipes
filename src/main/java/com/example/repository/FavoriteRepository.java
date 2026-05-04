package com.example.repository;

import com.example.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserEmail(String email);
    Optional<Favorite> findByUserEmailAndRecipeId(String email, Long recipeId);
    boolean existsByUserEmailAndRecipeId(String email, Long recipeId);
    void deleteByRecipeId(Long recipeId);
}
