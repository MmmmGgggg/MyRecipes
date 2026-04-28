import React, { useState } from "react";

const AUTH_API = `${window.location.protocol}//${window.location.hostname}:8080/api/auth`;

export default function AuthForm({ onLogin }) {
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    const url = isRegister ? `${AUTH_API}/register` : `${AUTH_API}/login`;
    const body = isRegister ? { email, password, name } : { email, password };

    try {
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data.error || "Something went wrong");
        return;
      }
      onLogin(data);
    } catch {
      setError("Could not connect to server");
    }
  };

  return (
    <form onSubmit={handleSubmit} className="auth-form">
      <h2>{isRegister ? "Register" : "Login"}</h2>
      {error && <p className="auth-error">{error}</p>}
      {isRegister && (
        <input placeholder="Name" value={name} onChange={e => setName(e.target.value)} required />
      )}
      <input type="email" placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} required />
      <input type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} required />
      <button type="submit">{isRegister ? "Register" : "Login"}</button>
      <p className="auth-toggle">
        {isRegister ? "Already have an account?" : "Don't have an account?"}{" "}
        <span onClick={() => { setIsRegister(!isRegister); setError(""); }}>
          {isRegister ? "Login" : "Register"}
        </span>
      </p>
    </form>
  );
}
