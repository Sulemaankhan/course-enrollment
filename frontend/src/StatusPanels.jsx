export function CourseSummary({ offering, status }) {
  return (
    <section className="course">
      <div>
        <p className="eyebrow">{status.term} · {status.courseCode}</p>
        <h2>{status.title}</h2>
        <p>{offering?.description}</p>
      </div>
      <div className="meter">
        <span>{status.enrolledCount} / {status.capacity} seats</span>
        <div className="bar" aria-hidden="true">
          <span style={{ width: `${Math.min(100, (status.enrolledCount / Math.max(status.capacity, 1)) * 100)}%` }} />
        </div>
        <strong>{status.seatsRemaining} open</strong>
      </div>
    </section>
  );
}

export function Boards({ status }) {
  return (
    <section className="boards">
      <article>
        <h3>Roster</h3>
        {status.roster.length === 0 && <p className="hint">No seats taken.</p>}
        <ol>
          {status.roster.map((seat) => (
            <li key={seat.learnerId}>
              <strong>{seat.displayName}</strong>
              <span>{new Date(seat.enrolledAt).toLocaleTimeString()}</span>
            </li>
          ))}
        </ol>
      </article>
      <article>
        <h3>FIFO waitlist</h3>
        {status.waitlist.length === 0 && <p className="hint">Nobody is waiting.</p>}
        <ol>
          {status.waitlist.map((seat) => (
            <li key={seat.learnerId} className={seat.eligible ? "" : "skipped"}>
              <strong>#{seat.position} {seat.displayName}</strong>
              <span>{seat.eligible ? new Date(seat.enqueuedAt).toLocaleTimeString() : "Will be skipped"}</span>
            </li>
          ))}
        </ol>
      </article>
    </section>
  );
}
