import React, { useState, useEffect } from "react";

export default function RecipeDetail({ api, id, onBack, onEdit }) {
  const [recipe, setRecipe] = useState(null);

  useEffect(() => {
    fetch(`${api}/${id}`).then(r => r.json()).then(setRecipe).catch(console.error);
  }, [api, id]);

  const handleDelete = () => {
    if (window.confirm("Delete this recipe?")) {
      fetch(`${api}/${id}`, { method: "DELETE" }).then(onBack).catch(console.error);
    }
  };

  if (!recipe) return <p>Loading...</p>;

  return (
    <div className="recipe-detail">
      <button onClick={onBack} className="back-btn">← Back</button>
      {recipe.imageUrl && <img src={recipe.imageUrl} alt={recipe.name} />}
      <h2>{recipe.name}</h2>
      {recipe.creator && <p className="recipe-meta">👤 {recipe.creator}</p>}
      {recipe.language && <p className="recipe-meta">🌍 {recipe.language}</p>}
      <h3>Ingredients</h3>
      <ul className="ingredient-list">
        {recipe.recipeIngredients?.map((ri, i) => (
          <li key={i}>
            <strong>{ri.ingredient?.name}</strong>
            {ri.amount && <span> — {ri.amount}</span>}
          </li>
        ))}
      </ul>
      <h3>Instructions</h3>
      <p>{recipe.instructions}</p>
      <div className="detail-buttons">
        <button onClick={() => onEdit(recipe)}>Edit</button>
        <button onClick={handleDelete} className="delete-btn">Delete</button>
      </div>
    </div>
  );
}
