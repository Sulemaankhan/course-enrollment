import { useCallback, useEffect, useState } from "react";
import { fetchOfferings, fetchOverview } from "./api";
import { Boards, CourseSummary } from "./StatusPanels";

export default function AdminDesk({ session, onSignOut }) {
  const [offering, setOffering] = useState(null);
  const [status, setStatus] = useState(null);
  const [message, setMessage] = useState("Registrar view. Enrollment and drops stay on the learner screen.");
  const [tone, setTone] = useState("neutral");
  const [live, setLive] = useState("idle");

  const refresh = useCallback(async () => {
    const offerings = await fetchOfferings(session.token);
    const course = offerings[0];
    setOffering(course);
    if (!course) return;
    const nextStatus = await fetchOverview(session.token, course.offeringId);
    setStatus(nextStatus);
    setLive("live");
  }, [session.token]);

  useEffect(() => {
    let stop = false;
    refresh().catch((error) => {
      if (error.status === 401) onSignOut();
      if (!stop) {
        setTone("bad");
        setMessage(error.message);
        setLive("offline");
      }
    });
    const timer = setInterval(() => {
      refresh().catch((error) => {
        if (error.status === 401) onSignOut();
        else setLive("reconnecting");
      });
    }, 2000);
    return () => {
      stop = true;
      clearInterval(timer);
    };
  }, [refresh, onSignOut]);

  return (
    <div className="desk">
      <header className="top">
        <div>
          <p className="eyebrow">Admin screen</p>
          <h1>Registrar overview</h1>
        </div>
        <p className={`live live-${live}`}>{live === "live" ? "Live" : live === "reconnecting" ? "Reconnecting" : live === "offline" ? "Offline" : "Waiting"}</p>
      </header>

      <main className="stage single">
        {status && (
          <>
            <CourseSummary offering={offering} status={status} />
            <section id="access" className="you">
              <div>
                <p className="eyebrow">Access</p>
                <h3>{session.displayName}</h3>
                <p>You can read the roster, the FIFO waitlist, and who will be skipped. Taking or leaving a seat is limited to a signed-in learner.</p>
              </div>
            </section>
            <p className="policy">{status.eligibilityPolicy}</p>
            <Boards status={status} />
          </>
        )}
        {!status && (
          <>
            <p id="course" className="empty">Loading the offering, the roster, and the FIFO waitlist.</p>
            <div id="access" />
            <div id="roster" />
            <div id="waitlist" />
          </>
        )}
        <p className={`flash flash-${tone}`} role="status">{message}</p>
      </main>
    </div>
  );
}
