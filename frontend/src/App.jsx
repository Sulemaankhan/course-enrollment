import { useCallback, useState } from "react";
import AdminDesk from "./AdminDesk";
import LoginScreen from "./LoginScreen";
import SiteFrame from "./SiteFrame";
import UserDesk from "./UserDesk";

const SESSION_KEY = "course-enrollment-session";

function loadSession() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(SESSION_KEY) || "null");
    if (!saved?.token) return null;
    return { ...saved, role: saved.role === "ADMIN" ? "ADMIN" : "USER" };
  } catch {
    return null;
  }
}

export default function App() {
  const [session, setSession] = useState(loadSession);

  const persist = useCallback((next) => {
    setSession(next);
    if (next) sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
    else sessionStorage.removeItem(SESSION_KEY);
  }, []);

  const signOut = useCallback(() => persist(null), [persist]);

  let screen = <LoginScreen onSignedIn={persist} />;
  if (session?.role === "ADMIN") screen = <AdminDesk session={session} onSignOut={signOut} />;
  else if (session) screen = <UserDesk session={session} onSignOut={signOut} />;

  return (
    <SiteFrame session={session} onSignOut={signOut}>
      {screen}
    </SiteFrame>
  );
}
