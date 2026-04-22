import React, { useState, useEffect } from "react";

const INGREDIENT_API = "http://localhost:8080/api/ingredients";

export default function RecipeForm({ api, recipe, onSave, onCancel }) {
  const [name, setName] = useState(recipe?.name || "");
  const [instructions, setInstructions] = useState(recipe?.instructions || "");
  const [imageUrl, setImageUrl] = useState(recipe?.imageUrl || "");
  const [creator, setCreator] = useState(recipe?.creator || "");
  const [language, setLanguage] = useState(recipe?.language || "");
  const [items, setItems] = useState(
    recipe?.recipeIngredients?.map(ri => ({ name: ri.ingredient?.name || "", amount: ri.amount || "" })) || [{ name: "", amount: "" }]
  );
  const [allIngredients, setAllIngredients] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [activeIndex, setActiveIndex] = useState(null);

  useEffect(() => {
    fetch(INGREDIENT_API).then(r => r.json()).then(setAllIngredients).catch(console.error);
  }, []);

  const updateItem = (index, field, value) => {
    const updated = [...items];
    updated[index][field] = value;
    setItems(updated);
    if (field === "name") {
      setActiveIndex(index);
      setSuggestions(value ? allIngredients.filter(i => i.name.toLowerCase().includes(value.toLowerCase())) : []);
    }
  };

  const selectSuggestion = (index, ingredientName) => {
    updateItem(index, "name", ingredientName);
    setSuggestions([]);
    setActiveIndex(null);
  };

  const addItem = () => setItems([...items, { name: "", amount: "" }]);

  const removeItem = (index) => setItems(items.filter((_, i) => i !== index));

  const handleSubmit = (e) => {
    e.preventDefault();
    const recipeIngredients = items
      .filter(i => i.name.trim())
      .map(i => ({ ingredient: { name: i.name.trim() }, amount: i.amount }));
    const method = recipe ? "PUT" : "POST";
    const url = recipe ? `${api}/${recipe.id}` : api;
    fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, instructions, imageUrl, creator, language, recipeIngredients }),
    }).then(onSave).catch(console.error);
  };

  return (
    <form onSubmit={handleSubmit} className="recipe-form">
      <h2>{recipe ? "Edit Recipe" : "Add Recipe"}</h2>
      <input placeholder="Recipe name" value={name} onChange={e => setName(e.target.value)} required />

      <h3>Ingredients</h3>
      {items.map((item, i) => (
        <div key={i} className="ingredient-row">
          <div className="ingredient-input-wrapper">
            <input
              placeholder="Ingredient name"
              value={item.name}
              onChange={e => updateItem(i, "name", e.target.value)}
              onFocus={() => setActiveIndex(i)}
              onBlur={() => setTimeout(() => setSuggestions([]), 200)}
            />
            {activeIndex === i && suggestions.length > 0 && (
              <div className="suggestions">
                {suggestions.map(s => (
                  <div key={s.id} className="suggestion" onMouseDown={() => selectSuggestion(i, s.name)}>
                    {s.name}
                  </div>
                ))}
              </div>
            )}
          </div>
          <input placeholder="Amount (e.g. 200g)" value={item.amount} onChange={e => updateItem(i, "amount", e.target.value)} className="amount-input" />
          {items.length > 1 && <button type="button" onClick={() => removeItem(i)} className="remove-btn">✕</button>}
        </div>
      ))}
      <button type="button" onClick={addItem} className="add-ingredient-btn">+ Add Ingredient</button>

      <textarea placeholder="Instructions" value={instructions} onChange={e => setInstructions(e.target.value)} rows={6} required />
      <input placeholder="Image URL (optional)" value={imageUrl} onChange={e => setImageUrl(e.target.value)} />
      <input placeholder="Creator (e.g. your name)" value={creator} onChange={e => setCreator(e.target.value)} />
      <select value={language} onChange={e => setLanguage(e.target.value)} className="language-select">
        <option value="">Select language...</option>
        <option value="English">English</option>
        <option value="Spanish">Spanish</option>
        <option value="French">French</option>
        <option value="Italian">Italian</option>
        <option value="German">German</option>
        <option value="Chinese">Chinese</option>
        <option value="Japanese">Japanese</option>
        <option value="Indian">Indian</option>
        <option value="Other">Other</option>
      </select>
      <div className="form-buttons">
        <button type="submit">{recipe ? "Update" : "Add"}</button>
        <button type="button" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
