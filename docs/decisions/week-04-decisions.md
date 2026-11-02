# Week 4 Decisions

**Project:** PIC16F72 Microcontroller Simulator
**Team:** Mohammed Ayman (Team Leader, Integration), Mohammed Shayaan (UI), Prakrithi (Core), Sancia (Logging)
**Date recorded:** 2 November 2026
**Branch:** week-04

## Summary

| # | Decision | Owner | Status |
|---|---|---|---|
| D1 | Core language changes from Java to Python | Prakrithi | Decided, faculty approval [pending / date] |
| D2 | User interface stays in Java Swing | Mohammed Shayaan | Decided |
| D3 | Processes talk over local TCP sockets and shared memory | Mohammed Ayman | Decided |
| D4 | One executor thread runs all commands; shared state is protected by a semaphore | Prakrithi | Decided |
| D5 | Logging runs as a separate process and never blocks the CPU | Sancia | Decided |
| D6 | Testing uses an automated suite against the real backend | Whole team | Decided |

---

## D1: Core language changes from Java to Python

**Problem:**
The team wanted to use POSIX IPC mechanisms (pipes, shared memory, semaphores,
message queues). Java could not use them, so the core could not stay in Java.

**Options:**
1. Keep Java for everything.
2. Write the core in Python and keep the Java Swing UI.

**Discussion:**
The Week 1 decision chose Java for the whole project, and it says a language
change needs a documented reason and faculty approval. This record is that
documentation. The project is now a two-language system, so the UI and the
core need an interface between them (see D3), and both Java and Python must be
installed to run it.

**Decision:**
Option 2. The core (CPU, instructions, fetch/decode/execute, registers, memory)
is written in Python (`pic_cpu_backend.py`). The reason given by the team is
that Python with the Win32 API can provide the IPC the team needs on Windows.

**Consequences:**
- The 8 instructions and the trace format were rewritten in Python with the same behaviour.
- The Java UI no longer runs CPU logic. It sends commands and shows the replies.

**Responsible:** Prakrithi (core), Mohammed Ayman (approval, integration)
**Status:** Decided. Faculty approval: [name, date, how approved]

---

## D2: User interface stays in Java Swing

**Problem:**
With the core moving to Python, should the UI move too?

**Options:**
1. Rewrite the UI in Python.
2. Keep the Java Swing UI.

**Discussion:**
The Swing UI already works (Load, Reset, Step, Run, state panels, trace).
Rewriting it adds work without adding features.

**Decision:**
Option 2. The UI stays in Java and connects to the Python core as a client.

**Responsible:** Mohammed Shayaan
**Status:** Decided

---

## D3: Processes talk over local TCP sockets and shared memory

**Problem:**
The Java UI, the Python core and the logger are separate programs. They need an
IPC mechanism that works on Windows.

**Options:**
1. Named pipes and POSIX message queues (explored earlier; Linux and WSL only).
2. TCP sockets on localhost, plus shared memory for the CPU state.

**Discussion:**
Sockets work the same in Java and Python on Windows with no extra tools.
Shared memory lets any other process read the CPU state without using the socket.

**Decision:**
Option 2.

| Channel | Mechanism | Name or port |
|---|---|---|
| UI and core | TCP socket, line protocol (LOAD, STEP, RESET, QUIT; replies end with `---END---`) | 127.0.0.1:45772 |
| Core to logger | TCP socket, one short connection per event | 127.0.0.1:45773 |
| CPU state | Shared memory, 84 bytes (W, PC, Z, C, halted, memory 16 to 31) | pic16f72_sim_shared |

**Responsible:** Mohammed Ayman
**Status:** Decided

---

## D4: One executor thread runs all commands; a semaphore protects shared state

**Problem:**
Several client connections can send commands at the same time, but the CPU
state must change one command at a time.

**Options:**
1. Let every client thread run its own commands, with locks around the CPU.
2. Put commands on a queue and let one executor thread run them in order.

**Discussion:**
Option 2 keeps the order simple and avoids races between commands. The queue
is limited to 16 entries, so a flood of commands waits instead of growing
without limit.

**Decision:**
Option 2. Client threads put commands on a bounded queue. One executor thread
runs them. `cpu.sem` (a Semaphore of 1, used as a lock) protects the CPU state
during step, load, reset and while the state is copied for shared memory.

**Responsible:** Prakrithi
**Status:** Decided

---

## D5: Logging is a separate process and never blocks the CPU

**Problem:**
Writing log messages must not slow down or stop instruction execution.

**Options:**
1. Log directly from the executor thread.
2. Put events on a queue and let a separate thread send them to a logger process.

**Discussion:**
If the logger is slow or not running, direct logging would stall the CPU.

**Decision:**
Option 2. Events go on a bounded queue (16 entries). If the queue is full, the
event is dropped instead of making the CPU wait. An event worker thread prints
each event and sends it to the logger on port 45773. If the logger cannot be
reached, the core prints a warning and carries on.

**Responsible:** Sancia
**Status:** Decided

---

## D6: Testing uses an automated suite against the real backend

**Problem:**
How do we check the core without opening the UI for every test?

**Options:**
1. Test by hand through the UI.
2. Write automated tests that start the real backend and talk to it over the socket.

**Discussion:**
Automated tests repeat the same checks every time and exercise the same
protocol the UI uses. They also read the shared memory and the logger events.

**Decision:**
Option 2. The suite is in `tests/test_pic_cpu_backend.py`: 28 tests, 26 pass.
The 2 remaining tests reproduce known bugs and are marked as expected failures,
so the suite stays green until the bugs are fixed. Results are in
`tests/test_cases.md`.

**Responsible:** Whole team (Mohammed Ayman keeps the results up to date)
**Status:** Decided

---

## Open points

- Faculty approval for D1 is still [pending / received on date].
- The core reason for D1 mentions the Win32 API, but `pic_cpu_backend.py` as
  submitted uses only the Python standard library. The team must confirm which
  statement is correct and make the report and the code agree.
- Known bugs found by the tests, to be fixed:
  - An address of 128 or more (for example `MOVWF 200`) stops the executor thread.
  - A negative address (for example `MOVWF -1`) is accepted and writes to the wrong cell.
  - The shared memory write has no cross-process lock, so another process could briefly read half-updated values.
