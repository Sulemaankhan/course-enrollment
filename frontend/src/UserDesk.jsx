import { useCallback, useEffect, useState } from "react";
import { command, fetchOfferings, fetchStatus, openStatusStream } from "./api";
import { Boards, CourseSummary } from "./StatusPanels";

export default function UserDesk({ session, onSignOut }) {
  const [offering, setOffering] = useState(null);
  const [status, setStatus] = useState(null);
  const [message, setMessage] = useState(`Signed in as ${session.displayName}.`);
  const [tone, setTone] = useState("neutral");
  const [busy, setBusy] = useState(false);
  const [live, setLive] = useState("idle");

  const refresh = useCallback(async () => {
    const offerings = await fetchOfferings(session.token);
    const course = offerings[0];
    setOffering(course);
    if (!course) return;
    const nextStatus = await fetchStatus(session.token, course.offeringId);
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
    const controller = new AbortController();
    if (offering?.offeringId) {
      openStatusStream(session.token, offering.offeringId, (next) => {
        setStatus(next);
        setLive("live");
      }, controller.signal);
    }
    return () => {
      stop = true;
      clearInterval(timer);
      controller.abort();
    };
  }, [session.token, offering?.offeringId, refresh, onSignOut]);

  async function act(action) {
    if (!offering) return;
    setBusy(true);
    try {
      const result = await command(session.token, offering.offeringId, action);
      setTone(result.outcome === "PREREQUISITE_NOT_MET" || result.outcome === "NOT_ENROLLED" ? "bad" : "good");
      setMessage(result.message);
      await refresh();
    } catch (error) {
      if (error.status === 401) onSignOut();
      setTone("bad");
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  const viewer = status?.viewer;
  const enrolled = viewer?.state === "ENROLLED";
  const waiting = viewer?.state === "WAITLISTED";

  return (
    <div className="desk">
      <header className="top">
        <div>
          <p className="eyebrow">Learner screen</p>
          <h1>Course enrollment</h1>
        </div>
        <div className="who">
          <p className={`live live-${live}`}>{live === "live" ? "Live" : live === "reconnecting" ? "Reconnecting" : live === "offline" ? "Offline" : "Waiting"}</p>
          <div>
            <strong>{session.displayName}</strong>
            <span>Learner</span>
          </div>
          <button type="button" className="text-button" onClick={onSignOut}>Sign out</button>
        </div>
      </header>

      <main className="stage single">
        {status && (
          <>
            <CourseSummary offering={offering} status={status} />
            <section className="you">
              <div>
                <p className="eyebrow">Signed in</p>
                <h3>{viewer.displayName}</h3>
                <p>{viewer.eligible ? `Eligible. Prerequisite ${status.prerequisiteCourseCode} is complete.` : `Not eligible. ${status.prerequisiteCourseCode} is required, so a request is skipped.`}</p>
                <p className="state">Status: {viewer.state === "WAITLISTED" ? `Waitlist #${viewer.waitlistPosition}` : viewer.state}</p>
              </div>
              <div className="actions">
                <button type="button" disabled={busy || enrolled || waiting} onClick={() => act("enroll")}>Enroll</button>
                <button type="button" className="secondary" disabled={busy || viewer.state === "NONE"} onClick={() => act("drop")}>
                  {waiting ? "Leave waitlist" : "Drop seat"}
                </button>
              </div>
            </section>
            <p className="policy">{status.eligibilityPolicy}</p>
            <Boards status={status} />
          </>
        )}
        {!status && <p className="empty">Loading the offering, the roster, and the FIFO waitlist.</p>}
        <p className={`flash flash-${tone}`} role="status">{message}</p>
      </main>
    </div>
  );
}
