import { useEffect, useState } from "react";

const GUEST_LINKS = [{ href: "#sign-in", label: "Sign in" }];

const LEARNER_LINKS = [
  { href: "#course", label: "Course" },
  { href: "#seat", label: "Your seat" },
  { href: "#roster", label: "Roster" },
  { href: "#waitlist", label: "Waitlist" }
];

const ADMIN_LINKS = [
  { href: "#course", label: "Overview" },
  { href: "#access", label: "Access" },
  { href: "#roster", label: "Roster" },
  { href: "#waitlist", label: "Waitlist" }
];

function useHash() {
  const [hash, setHash] = useState(() => window.location.hash);
  useEffect(() => {
    const sync = () => setHash(window.location.hash);
    window.addEventListener("hashchange", sync);
    return () => window.removeEventListener("hashchange", sync);
  }, []);
  return hash;
}

export default function SiteFrame({ session, onSignOut, children }) {
  const hash = useHash();
  const links = !session ? GUEST_LINKS : session.role === "ADMIN" ? ADMIN_LINKS : LEARNER_LINKS;
  const current = links.some((link) => link.href === hash) ? hash : links[0].href;

  return (
    <div className="app-frame">
      <header className="site-header">
        <div className="site-header-inner">
          <a className="brand" href={links[0].href}>
            <span className="brand-mark" aria-hidden="true">E</span>
            <span>Enrollment</span>
          </a>
          <nav className="site-nav" aria-label="Primary">
            {links.map((link) => (
              <a key={link.href} href={link.href} aria-current={link.href === current ? "page" : undefined}>
                {link.label}
              </a>
            ))}
          </nav>
          {session && (
            <div className="site-user">
              <span className="avatar" aria-hidden="true">{session.displayName.trim().charAt(0).toUpperCase()}</span>
              <span className="site-user-meta">
                <strong>{session.displayName}</strong>
                <span>{session.role === "ADMIN" ? "Admin" : "Learner"}</span>
              </span>
              <button type="button" className="text-button" onClick={onSignOut}>Sign out</button>
            </div>
          )}
        </div>
      </header>

      <div className="app-main">{children}</div>

      <footer className="site-footer">
        <div className="site-footer-inner">
          <div>
            <p className="footer-title">Course enrollment desk</p>
            <p>Learners take a seat or join the FIFO waitlist. The registrar watches the roster and who will be skipped.</p>
          </div>
          <nav aria-label="Footer">
            <p className="eyebrow">On this page</p>
            <div className="footer-links">
              {links.map((link) => (
                <a key={link.href} href={link.href}>{link.label}</a>
              ))}
            </div>
          </nav>
        </div>
        <div className="footer-base">
          <span>Enrollment desk</span>
          <span>Demo accounts only</span>
        </div>
      </footer>
    </div>
  );
}
