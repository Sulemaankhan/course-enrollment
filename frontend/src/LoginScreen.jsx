import { useState } from "react";
import { login } from "./api";

const ACCOUNTS = [
  { id: "alice", name: "Alice Chen", note: "Learner" },
  { id: "bob", name: "Bob Patel", note: "Learner" },
  { id: "carol", name: "Carol Diaz", note: "Learner" },
  { id: "dan", name: "Dan Brooks", note: "Learner" },
  { id: "eve", name: "Eve Nguyen", note: "Learner" },
  { id: "admin", name: "Registrar", note: "Admin" }
];

export default function LoginScreen({ onSignedIn }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("Sign in to open the enrollment desk or the registrar screen.");
  const [tone, setTone] = useState("neutral");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    const id = username.trim();
    if (!id || !password) {
      setTone("bad");
      setMessage("Enter a username and password.");
      return;
    }
    setBusy(true);
    try {
      const token = await login(id, password);
      onSignedIn({
        token: token.token,
        learnerId: token.learnerId,
        displayName: token.displayName,
        role: token.role === "ADMIN" ? "ADMIN" : "USER"
      });
    } catch (error) {
      setTone("bad");
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-shell">
      <section id="sign-in" className="login-card">
        <div className="login-aside">
          <p className="eyebrow">Registrar desk</p>
          <h1>Course enrollment</h1>
          <p>Learners open their own enrollment screen. The admin account opens the roster and the FIFO waitlist.</p>
        </div>
        <div className="login-panel">
          <h2>Sign in</h2>
          <p className="hint">Choose a demo account, then sign in with the shared password.</p>
          <form onSubmit={submit}>
            <label>
            Username
            <input
              name="username"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
            />
            </label>
            <label>
            Password
            <input
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
            </label>
            <button type="submit" disabled={busy}>{busy ? "Signing in" : "Sign in"}</button>
          </form>
          <p className="hint">Demo password is <strong>password</strong>. Choose an account to fill the username.</p>
          <div className="account-row">
            {ACCOUNTS.map((account) => (
              <button
                key={account.id}
                type="button"
                className={username === account.id ? "chip active" : "chip"}
                onClick={() => setUsername(account.id)}
              >
                <span>{account.name}</span>
                <small>{account.note}</small>
              </button>
            ))}
          </div>
          <p className={`flash flash-${tone}`} role="status">{message}</p>
        </div>
      </section>
    </div>
  );
}
