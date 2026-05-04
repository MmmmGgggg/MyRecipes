import React, { useState, useEffect } from "react";

const INGREDIENT_API = `${window.location.protocol}//${window.location.hostname}:8080/api/ingredients`;
const TAGS_API = `${window.location.protocol}//${window.location.hostname}:8080/api/recipes/tags`;

export default function RecipeForm({ api, recipe, onSave, onCancel, token, userName }) {
  const [name, setName] = useState(recipe?.name || "");
  const [instructions, setInstructions] = useState(recipe?.instructions || "");
  const [imageUrl, setImageUrl] = useState(recipe?.imageUrl || "");
  const [creator] = useState(recipe?.creator || userName || "");
  const [language, setLanguage] = useState(recipe?.language || "");
  const [servings, setServings] = useState(recipe?.servings || "");
  const [prepTime, setPrepTime] = useState(recipe?.prepTimeMinutes || "");
  const [cookTime, setCookTime] = useState(recipe?.cookTimeMinutes || "");
  const [visibility, setVisibility] = useState(recipe?.visibility || "PRIVATE");
  const [tags, setTags] = useState(recipe?.tags || []);
  const [tagInput, setTagInput] = useState("");
  const [tagSuggestions, setTagSuggestions] = useState([]);
  const [allTags, setAllTags] = useState([]);
  const [items, setItems] = useState(
    recipe?.recipeIngredients?.map(ri => ({ name: ri.ingredient?.name || "", amount: ri.amount || "", confirmed: true })) || [{ name: "", amount: "", confirmed: false }]
  );
  const [allIngredients, setAllIngredients] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [activeIndex, setActiveIndex] = useState(null);

  useEffect(() => {
    fetch(INGREDIENT_API).then(r => r.json()).then(setAllIngredients).catch(console.error);
    fetch(TAGS_API).then(r => r.json()).then(setAllTags).catch(console.error);
  }, []);

  const updateItem = (index, field, value) => {
    const updated = [...items];
    updated[index][field] = value;
    if (field === "name") updated[index].confirmed = false;
    setItems(updated);
    if (field === "name") {
      setActiveIndex(index);
      setSuggestions(value ? allIngredients.filter(i => i.name.toLowerCase().includes(value.toLowerCase())) : []);
    }
  };

  const selectSuggestion = (index, ingredientName) => {
    const updated = [...items];
    updated[index].name = ingredientName;
    updated[index].confirmed = true;
    setItems(updated);
    setSuggestions([]);
    setActiveIndex(null);
  };

  const confirmNewIngredient = (index) => {
    const updated = [...items];
    updated[index].confirmed = true;
    setItems(updated);
    setSuggestions([]);
    setActiveIndex(null);
  };

  const addItem = () => setItems([...items, { name: "", amount: "", confirmed: false }]);
  const removeItem = (index) => setItems(items.filter((_, i) => i !== index));

  const handleTagInput = (value) => {
    setTagInput(value);
    if (value) {
      setTagSuggestions(allTags.filter(t =>
        t.toLowerCase().includes(value.toLowerCase()) && !tags.includes(t)
      ));
    } else {
      setTagSuggestions([]);
    }
  };

  const addTag = (tag) => {
    const trimmed = tag.trim();
    if (trimmed && !tags.includes(trimmed)) {
      setTags([...tags, trimmed]);
    }
    setTagInput("");
    setTagSuggestions([]);
  };

  const handleTagKeyDown = (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      if (tagInput.trim()) addTag(tagInput);
    }
  };

  const removeTag = (tag) => setTags(tags.filter(t => t !== tag));

  const handleSubmit = (e) => {
    e.preventDefault();
    const unconfirmed = items.filter(i => i.name.trim() && !i.confirmed);
    if (unconfirmed.length > 0) {
      alert(`Please select ingredients from the dropdown or click "+ Create" to confirm new ones: ${unconfirmed.map(i => i.name).join(", ")}`);
      return;
    }
    const recipeIngredients = items
      .filter(i => i.name.trim())
      .map(i => ({ ingredient: { name: i.name.trim() }, amount: i.amount }));
    const method = recipe ? "PUT" : "POST";
    const url = recipe ? `${api}/${recipe.id}` : api;
    fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ name, instructions, imageUrl, creator, language, visibility, tags,
        servings: servings || null, prepTimeMinutes: prepTime || null, cookTimeMinutes: cookTime || null,
        recipeIngredients }),
    }).then(r => {
      if (!r.ok) {
        r.text().then(t => console.error("Save failed:", r.status, t));
        return;
      }
      onSave();
    }).catch(err => console.error("Save error:", err));
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
              className={item.name.trim() ? (item.confirmed ? "ingredient-confirmed" : "ingredient-unconfirmed") : ""}
            />
            {activeIndex === i && item.name.trim() && !item.confirmed && (
              <div className="suggestions">
                {suggestions.map(s => (
                  <div key={s.id} className="suggestion" onMouseDown={() => selectSuggestion(i, s.name)}>
                    {s.name}
                  </div>
                ))}
                {!suggestions.some(s => s.name.toLowerCase() === item.name.trim().toLowerCase()) && (
                  <div className="suggestion suggestion-create" onMouseDown={() => confirmNewIngredient(i)}>
                    + Create "{item.name.trim()}"
                  </div>
                )}
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
      <input placeholder="Creator" value={creator} readOnly className="readonly-input" />

      <h3>Tags</h3>
      <div className="tag-input-wrapper">
        <div className="selected-ingredients">
          {tags.map(tag => (
            <span key={tag} className="chip chip-active">
              {tag}
              <button type="button" className="chip-remove" onClick={() => removeTag(tag)}>✕</button>
            </span>
          ))}
        </div>
        <div className="ingredient-search-wrapper">
          <input
            placeholder="Type a tag and press Enter (e.g. Vegetarian, Quick, Dessert)"
            value={tagInput}
            onChange={e => handleTagInput(e.target.value)}
            onKeyDown={handleTagKeyDown}
            onBlur={() => setTimeout(() => setTagSuggestions([]), 200)}
          />
          {tagSuggestions.length > 0 && (
            <div className="suggestions">
              {tagSuggestions.map(t => (
                <div key={t} className="suggestion" onMouseDown={() => addTag(t)}>
                  {t}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <input placeholder="Language (e.g. Italian, Serbian, Thai)" value={language} onChange={e => setLanguage(e.target.value)} />
      <div className="time-row">
        <input type="number" placeholder="Servings" value={servings} onChange={e => setServings(e.target.value)} min="1" />
        <input type="number" placeholder="Prep time (min)" value={prepTime} onChange={e => setPrepTime(e.target.value)} min="0" />
        <input type="number" placeholder="Cook time (min)" value={cookTime} onChange={e => setCookTime(e.target.value)} min="0" />
      </div>
      <select value={visibility} onChange={e => setVisibility(e.target.value)} className="language-select">
        <option value="PRIVATE">🔒 Private</option>
        <option value="SHARED">👥 Shared (Friends)</option>
        <option value="PUBLIC">🌍 Public</option>
      </select>
      <div className="form-buttons">
        <button type="submit">{recipe ? "Update" : "Add"}</button>
        <button type="button" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
