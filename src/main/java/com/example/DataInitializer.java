package com.example;

import com.example.model.*;
import com.example.repository.IngredientRepository;
import com.example.repository.RecipeRepository;
import com.example.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RecipeRepository recipeRepo;
    private final IngredientRepository ingredientRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RecipeRepository recipeRepo, IngredientRepository ingredientRepo,
                           UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.recipeRepo = recipeRepo;
        this.ingredientRepo = ingredientRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (recipeRepo.count() > 0) return;

        User chef = userRepo.findByEmail("chef@myrecipes.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail("chef@myrecipes.com");
                    u.setPassword(passwordEncoder.encode("chef123"));
                    u.setName("Chef Demo");
                    return userRepo.save(u);
                });

        createRecipe(chef, "Pasta Carbonara",
                "1. Cook spaghetti in salted boiling water\n2. Fry pancetta until crispy\n3. Mix eggs, parmesan, and pepper\n4. Drain pasta, toss with pancetta\n5. Remove from heat, stir in egg mixture\n6. Serve immediately with extra parmesan",
                "Italian", 4, 10, 20,
                List.of("Quick", "Comfort Food"),
                new String[]{"spaghetti", "400g"}, new String[]{"pancetta", "150g"},
                new String[]{"egg", "3"}, new String[]{"parmesan", "100g"},
                new String[]{"black pepper", "to taste"});

        createRecipe(chef, "Thai Green Curry",
                "1. Heat oil in a wok\n2. Fry green curry paste for 1 minute\n3. Add coconut milk and bring to simmer\n4. Add chicken, cook 10 minutes\n5. Add vegetables, cook 5 more minutes\n6. Season with fish sauce and lime\n7. Serve with jasmine rice",
                "Thai", 4, 15, 25,
                List.of("Spicy", "Gluten Free"),
                new String[]{"chicken breast", "500g"}, new String[]{"coconut milk", "400ml"},
                new String[]{"green curry paste", "3 tbsp"}, new String[]{"bell pepper", "1"},
                new String[]{"bamboo shoot", "100g"}, new String[]{"fish sauce", "2 tbsp"},
                new String[]{"jasmine rice", "300g"});

        createRecipe(chef, "Greek Salad",
                "1. Chop tomatoes, cucumber, and onion\n2. Add olives and capers\n3. Top with feta cheese\n4. Drizzle with olive oil and oregano\n5. Season with salt and pepper",
                "Greek", 2, 10, 0,
                List.of("Vegetarian", "Quick", "Healthy", "Gluten Free"),
                new String[]{"tomato", "3 large"}, new String[]{"cucumber", "1"},
                new String[]{"red onion", "1/2"}, new String[]{"feta cheese", "150g"},
                new String[]{"kalamata olive", "handful"}, new String[]{"olive oil", "3 tbsp"});

        createRecipe(chef, "Banana Pancakes",
                "1. Mash bananas in a bowl\n2. Mix in eggs and vanilla\n3. Add flour, baking powder, and milk\n4. Heat butter in a pan\n5. Pour small circles of batter\n6. Flip when bubbles form\n7. Serve with maple syrup and berries",
                "American", 3, 10, 15,
                List.of("Vegetarian", "Breakfast", "Quick"),
                new String[]{"banana", "2 ripe"}, new String[]{"egg", "2"},
                new String[]{"flour", "150g"}, new String[]{"milk", "100ml"},
                new String[]{"baking powder", "1 tsp"}, new String[]{"vanilla extract", "1 tsp"});

        createRecipe(chef, "Lentil Soup",
                "1. Sauté onion, carrot, and celery in olive oil\n2. Add garlic and cumin, cook 1 minute\n3. Add lentils and vegetable broth\n4. Bring to boil, then simmer 25 minutes\n5. Season with lemon juice, salt, and pepper\n6. Blend partially for creamy texture",
                "Middle Eastern", 6, 10, 30,
                List.of("Vegan", "Healthy", "Comfort Food"),
                new String[]{"red lentil", "300g"}, new String[]{"onion", "1"},
                new String[]{"carrot", "2"}, new String[]{"celery", "2 stalks"},
                new String[]{"garlic", "3 cloves"}, new String[]{"vegetable broth", "1.5L"},
                new String[]{"cumin", "1 tsp"}, new String[]{"lemon", "1"});
    }

    private void createRecipe(User user, String name, String instructions, String language,
                               int servings, int prepTime, int cookTime,
                               List<String> tags, String[]... ingredients) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setInstructions(instructions);
        recipe.setLanguage(language);
        recipe.setCreator(user.getName());
        recipe.setUser(user);
        recipe.setVisibility(Visibility.PUBLIC);
        recipe.setServings(servings);
        recipe.setPrepTimeMinutes(prepTime);
        recipe.setCookTimeMinutes(cookTime);
        recipe.setTags(tags);
        recipe = recipeRepo.save(recipe);

        for (String[] ing : ingredients) {
            Ingredient ingredient = ingredientRepo.findByNameIgnoreCase(ing[0])
                    .orElseGet(() -> ingredientRepo.save(new Ingredient(ing[0])));
            RecipeIngredient ri = new RecipeIngredient();
            ri.setRecipe(recipe);
            ri.setIngredient(ingredient);
            ri.setAmount(ing[1]);
            recipe.getRecipeIngredients().add(ri);
        }

        recipeRepo.save(recipe);
    }
}
