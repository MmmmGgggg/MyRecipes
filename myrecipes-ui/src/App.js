import React, { useState } from "react";
import RecipeList from "./components/RecipeList";
import RecipeForm from "./components/RecipeForm";
import RecipeDetail from "./components/RecipeDetail";
import "./App.css";

const API = "http://localhost:8080/api/recipes";

function App() {
  const [view, setView] = useState("list");
  const [editingRecipe, setEditingRecipe] = useState(null);
  const [selectedId, setSelectedId] = useState(null);

  const goToList = () => { setView("list"); setEditingRecipe(null); };
  const goToAdd = () => { setEditingRecipe(null); setView("form"); };
  const goToEdit = (recipe) => { setEditingRecipe(recipe); setView("form"); };
  const goToDetail = (id) => { setSelectedId(id); setView("detail"); };

  return (
    <div className="app">
      <header>
        <h1 onClick={goToList}>🍳 MyRecipes</h1>
        {view !== "form" && <button onClick={goToAdd}>+ Add Recipe</button>}
      </header>
      <main>
        {view === "list" && <RecipeList api={API} onSelect={goToDetail} />}
        {view === "form" && <RecipeForm api={API} recipe={editingRecipe} onSave={goToList} onCancel={goToList} />}
        {view === "detail" && <RecipeDetail api={API} id={selectedId} onBack={goToList} onEdit={goToEdit} />}
      </main>
    </div>
  );
}

export default App;
