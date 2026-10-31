async function readBody(response) {
  const text = await response.text();
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

function failure(response, body, fallback) {
  const error = new Error(body.message || body.detail || fallback);
  error.status = response.status;
  return error;
}

export async function login(username, password) {
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
  const body = await readBody(response);
  if (!response.ok) {
    const fallback = response.status === 401 ? "Invalid username or password." : "Sign-in failed.";
    throw failure(response, body, fallback);
  }
  return body;
}

export async function fetchOfferings(token) {
  const response = await fetch("/api/catalog/offerings", {
    headers: { Authorization: `Bearer ${token}` }
  });
  const body = await readBody(response);
  if (!response.ok) {
    throw failure(response, body, "The catalog is unavailable.");
  }
  return body;
}

export async function fetchStatus(token, offeringId) {
  const response = await fetch(`/api/enrollments/offerings/${offeringId}/status`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  const body = await readBody(response);
  if (!response.ok) {
    const fallback = response.status === 401 ? "Your session expired." : "Status could not be loaded.";
    throw failure(response, body, fallback);
  }
  return body;
}

export async function fetchOverview(token, offeringId) {
  const response = await fetch(`/api/enrollments/offerings/${offeringId}/overview`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  const body = await readBody(response);
  if (!response.ok) {
    const fallback = response.status === 403
      ? "The registrar screen is limited to the admin account."
      : response.status === 401
        ? "Your session expired."
        : "The roster could not be loaded.";
    throw failure(response, body, fallback);
  }
  return body;
}

export async function command(token, offeringId, action) {
  const response = await fetch(`/api/enrollments/offerings/${offeringId}/${action}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Idempotency-Key": crypto.randomUUID()
    }
  });
  const body = await readBody(response);
  if (response.status === 401 || response.status === 403) {
    const fallback = response.status === 401
      ? "Your session expired."
      : "This account cannot enroll or drop.";
    throw failure(response, body, fallback);
  }
  if (!body.outcome) {
    throw failure(response, body, "The enrollment service did not respond.");
  }
  return body;
}

export function openStatusStream(token, offeringId, onStatus, signal) {
  const run = async () => {
    const response = await fetch(`/api/enrollments/offerings/${offeringId}/events`, {
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "text/event-stream"
      },
      signal
    });
    if (!response.ok || !response.body) return;
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    while (!signal.aborted) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const frames = buffer.split("\n\n");
      buffer = frames.pop() || "";
      for (const frame of frames) {
        const data = frame
          .split("\n")
          .filter((line) => line.startsWith("data:"))
          .map((line) => line.slice(5).trim())
          .join("");
        if (!data) continue;
        try {
          onStatus(JSON.parse(data));
        } catch {
          // Heartbeat and comment frames are not status documents.
        }
      }
    }
  };
  run().catch(() => {});
}
