package com.example.controller;

import com.example.model.Ingredient;
import com.example.repository.IngredientRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@CrossOrigin
public class IngredientController {

    private final IngredientRepository repo;

    public IngredientController(IngredientRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Ingredient> getAll(@RequestParam(required = false) String search) {
        if (search != null) return repo.findByNameContainingIgnoreCase(search);
        return repo.findAll();
    }

    @PostMapping
    public Ingredient create(@RequestBody Ingredient ingredient) {
        return repo.findByNameIgnoreCase(ingredient.getName())
                .orElseGet(() -> repo.save(ingredient));
    }
}
