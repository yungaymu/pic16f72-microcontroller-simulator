# Test Cases and Results

Backend: `pic_cpu_backend.py` (Python core). Tests talk to it over TCP
127.0.0.1:45772 like the Java UI, read the shared memory
`pic16f72_sim_shared`, and receive events on a mock logger (port 45773).

Run date: 2 November 2026. Python 3.12.3.
Result: **28 tests ran. 26 passed. 2 known-bug tests failed as expected. 0 unexpected failures.**
Raw output: `test_results.txt`. Full demo trace: `demo_run_output.txt`.

## Instructions

| Test | Instruction | Setup | Expected result | Actual result | Status |
|---|---|---|---|---|---|
| TC01 | MOVLW 5 | W = 0 | W = 5 | W = 5 | PASS |
| TC02 | MOVWF 20 | W = 5 | memory[20] = 5 | memory[20] = 5 | PASS |
| TC03 | ADDLW 10 | W = 5 | W = 15, C = 0 | W = 15, C = 0 | PASS |
| TC03b | ADDLW 10 (overflow) | W = 250 | W = 4, C = 1 | W = 4, C = 1 | PASS |
| TC04 | SUBLW 50 | W = 8 | W = 42, C = 1 | W = 42, C = 1 | PASS |
| TC04b | SUBLW 3 (borrow) | W = 10 | W = 249, C = 0 | W = 249, C = 0 | PASS |
| TC05 | ANDLW 6 | W = 15 | W = 6, Z = 0 | W = 6, Z = 0 | PASS |
| TC05b | ANDLW 2 (zero) | W = 1 | W = 0, Z = 1 | W = 0, Z = 1 | PASS |
| TC06 | INCF 20 | memory[20] = 5 | memory[20] = 6 | memory[20] = 6 | PASS |
| TC06b | INCF 20 (wrap) | memory[20] = 255 | memory[20] = 0, Z = 1 | memory[20] = 0, Z = 1 | PASS |
| TC07 | GOTO 2 | PC = 0 | PC goes 0 to 2, line 1 skipped | PC 0 to 2, W = 1 | PASS |
| TC08 | HALT | none | "PROGRAM TERMINATED"; next STEP says "already halted" | as expected | PASS |
| TC-trace | any | MOVLW 5 | trace shows FETCH, DECODE, EXECUTE | all three shown | PASS |

## Demonstration program (`demo_program.txt`)

| Test | Check | Expected result | Actual result | Status |
|---|---|---|---|---|
| TC09 | Run 9 steps to HALT | W = 44, memory[20] = 6, memory[21] = 15, C = 1, halted | same | PASS |
| TC10 | GOTO 8 skips MOVLW 99 | W is never 99 | W is never 99 | PASS |

## Protocol

| Test | Check | Expected result | Actual result | Status |
|---|---|---|---|---|
| TC11 | LOAD 2 lines | "OK 2 instructions" | same | PASS |
| TC12 | LOAD unknown opcode FOO | "ERROR Unknown instruction: FOO" | same | PASS |
| TC13 | Comments, blank lines, lowercase | accepted, program runs | accepted, W = 7 | PASS |
| TC14 | RESET | W, PC, memory cleared; program kept | same | PASS |
| TC15 | STEP after last instruction | "No more instructions. Program halted." | same | PASS |
| TC16 | LOAD after running | CPU state cleared | W = 0 | PASS |

## Shared memory

| Test | Check | Expected result | Actual result | Status |
|---|---|---|---|---|
| TC17 | Segment layout | 21 ints = 84 bytes | 84 bytes | PASS |
| TC18 | Read from a separate process | W = 42 after MOVLW 42 | W = 42 | PASS |

## Events, concurrency

| Test | Check | Expected result | Actual result | Status |
|---|---|---|---|---|
| TC19 | Logger receives events | COMMAND LOAD, LOADED, EXECUTED, HALTED, RESET | all received | PASS |
| TC20 | Two clients send 40 STEPs each at once | memory[20] = 80 (no lost or doubled step) | 80 | PASS |
| TC21 | 30 LOADs in a row | no deadlock | no deadlock | PASS |

## Known issues (bugs reproduced, tracked as GitHub issues)

These tests describe the correct behaviour and currently fail. They are marked
`expectedFailure`, so the suite stays green. When a bug is fixed, unittest
reports "unexpected success" and the marker can be removed.

| Test | Bug | Expected result | Actual result | Status |
|---|---|---|---|---|
| KI1 | `MOVWF 200` (address out of range) | STEP replies "ERROR ..." | no reply: the executor thread crashed and later commands hang | KNOWN BUG |
| KI2 | `MOVWF -1` (negative address) | LOAD replies "ERROR ..." | accepted; writes memory[127] | KNOWN BUG |

## Observation

The Z flag is recalculated after every STEP from the last result, so the first
instruction of a program (for example GOTO) shows `Z Flag : true`. This matches
the documented design in `docs/instructions.md` and is not counted as a bug.
