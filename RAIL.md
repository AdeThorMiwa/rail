# Rail

Rail is a personal consistency engine and growth companion. It turns your intentions into a living weekly plan, executes that plan through a simple tactile UI, and over time builds a deep understanding of your psychology to help you become more consistent and self-aware.

---

## Core Philosophy

Most planning tools make *you* do the scheduling and then track what you did. Rail inverts this: **you define intent, Rail owns the schedule, you just execute.** The UI reflects this — there is nothing to plan inside the app day-to-day. You just show up and the next thing is waiting.

Rail is not a task manager. It is a personal operating system for how you spend your time and energy.

---

## Goals

Goals are the foundation of Rail. They come in two shapes:

**Habits** — recurring, open-ended commitments with no finish line.
- Examples: "Journal every morning", "Meditate 5x/week", "Read 20 min daily"
- These never complete — they streak, they slip, they recover

**Projects** — goals with an end state, broken into recurring work sessions.
- Examples: "Finish Atomic Habits → read 20 min, 4x/week until done"
- Rail knows when a project is complete and retires it

### Goal Configuration

Each goal has:
- **Cadence** — how often per day/week
- **Duration** — estimated time per session
- **Soft preferences** — time of day (morning / afternoon / evening), days of week

Soft preferences are weights, not hard rules. Rail uses them as inputs when scheduling, but will override them if the week demands it. Rail always suggests a time first — your preferences only take effect if you explicitly override Rail's suggestion.

---

## The Weekly Ritual

Every week begins with a structured conversation between you and Rail. By default this happens Sunday evening, but Rail will suggest a better time if it sees one based on your patterns and schedule.

The ritual has three acts:

### 1. Retro
Rail pulls last week's data and leads the reflection. It asks what you did right, what went wrong, what you think you could do better, and what action items come out of that. Rail listens and stores your responses — they inform future planning and the personality file.

### 2. Carryover
Rail surfaces everything that didn't get done last week — incomplete project sessions, skipped habits, deferred items. Together you decide what carries forward and what gets dropped.

### 3. Planning
You tell Rail what you want to accomplish this week. Rail proposes a rough plan using the LLM. You can push back, adjust goals, or approve as-is. The plan is intentionally loose — Rail knows it will evolve as the week unfolds.

---

## Daily Execution

### The Stack

The entire daily experience is a stack of colored task cards. Similar goals share similar colors. The next task is always on top.

- **Swipe right** — done. The card flies off, the next one bubbles up.
- **Swipe left** — skip. Rail silently reschedules and notes the skip.
- **Tap** — expand the card for context, duration, or notes Rail has left.

No calendar. No timeline. No drag-and-drop. Just the stack.

### Notifications

Rail fires a real alarm (not just a banner) when it's time for a task. Tapping the notification opens the app directly to that card on top.

---

## When Plans Change

Life happens. When you skip tasks or something comes up, Rail reshuffles the rest of the week automatically. It always tells you why:

> *"You missed gym Monday and Tuesday. I've added sessions on Thursday and Saturday to keep your 3x goal. Sunday is now a rest day."*

You don't need to do anything. The explanation is informational — Rail handles the rest.

---

## The Notes Thread

A conversational interface that lives below the stack. This is where Rail and you communicate:

- Rail leaves notes: reshuffles, pattern observations, encouragement, questions
- You message Rail freely: "I'm burned out this week, go easy on me." / "Traveling Wed–Fri." / "I crushed it today."
- If you say something ambiguous, Rail asks a clarifying question before acting on it

Rail reads the full thread when planning and making decisions. Everything you say here is context.

---

## Psychology & Pattern Intelligence

Rail tracks everything over time and periodically surfaces insights — not just streaks, but behavioral patterns:

- "You almost never complete afternoon tasks on Mondays."
- "Your gym consistency drops significantly in weeks where you have 3+ social events."
- "You journal most reliably when it's your first card of the day, not second."

These surface as Rail notes in the thread and directly inform future planning.

---

## The Personality File

Rail maintains a private, living document about you. It is never shown to you directly, but it informs everything: how Rail speaks, how it frames retros, what it chooses to surface, when it pushes vs. gives grace.

The personality file is updated from:
- How you write in the notes thread (terse, expansive, frustrated, energized)
- What you skip and when, over time
- How retros go — do you take ownership or attribute to external factors?
- Moments of honesty or vulnerability in the thread
- Long-term consistency data across goals

Rail's tone mirrors your psychology. Early on it is neutral. Over time it becomes distinctly yours.

### Rail's Read On You

To keep the personality file honest and build trust, Rail surfaces a periodic "here's my read on you" note — a plain-language summary of what it thinks it knows. You can correct it. Corrections update the file.

---

## Gamification

**Streaks** — current streak + personal best per habit. A streak flame that intensifies the longer it runs.

**Consistency score** — a weekly percentage, not a grade. Shown with a trend arrow. Feels like a fitness tracker, not a report card.

**Goal health** — each goal card has a subtle visual health state. Goals you've been nailing glow slightly. Goals you've been avoiding look faded.

**Weekly wrap** — at the end of the Sunday retro, Rail generates a single "week in review" card. A satisfying artifact, shareable if you want.

**Milestone moments** — Rail notices when you hit 30, 60, 100 days on a habit and makes it a moment. Not a notification — a card that appears at the top of your stack that morning: *"30 days of journaling. That's not nothing."*

**In the zone** — complete 3+ cards in a row without skipping and the UI subtly shifts. The stack gets a glow, slight haptic rhythm. Rewards momentum.

**Comeback recognition** — fall off a habit for a week and return to it, Rail acknowledges it in the thread: *"You're back. That's the hard part."*

---

## Settings

A single, simple settings page. No dashboard. Key configs:

- Retro day + time (default: Sunday 7pm)
- Daily start / end window
- Days off (never schedule on these days)
- Notification style (alarm vs. banner)

Rail can always suggest changing these based on what it observes. Settings exist as a starting point — Rail evolves beyond them.

---

## Platforms

- **Mobile** — primary execution surface. Notifications, alarms, swipe interactions.
- **Desktop** — planning and retro surface. Notes thread, weekly review.

---

## What Rail Is Not

- Not a calendar app
- Not a task manager
- Not a project management tool
- Not a habit tracker that just counts checkboxes

Rail is the layer above all of those things — the intelligence that decides *what you should be doing right now* and holds you accountable to the person you said you want to be.
