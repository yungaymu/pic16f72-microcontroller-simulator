# Implemented Instructions — Week 2

All 8 instructions required by Week 2 are implemented as separate Java classes extending
`Instruction` (class-based representation, per Week 1 decision — see `docs/decisions/issue-01.md`).

| # | Instruction | Category | Operand | Operation | Registers/Memory Affected | Flags Affected |
|---|---|---|---|---|---|---|
| 1 | `MOVLW k` | Data Transfer | Literal `k` | W ← k | W | Z |
| 2 | `MOVWF f` | Data Transfer | Address `f` | memory[f] ← W | memory[f] | Z |
| 3 | `ADDLW k` | Arithmetic | Literal `k` | W ← W + k | W | Z, C |
| 4 | `SUBLW k` | Arithmetic | Literal `k` | W ← k − W | W | Z, C |
| 5 | `ANDLW k` | Logical | Literal `k` | W ← W AND k | W | Z |
| 6 | `INCF f` | Increment/Decrement | Address `f` | memory[f] ← memory[f] + 1 | memory[f] | Z |
| 7 | `GOTO addr` | Control Flow | Line `addr` | PC ← addr | PC | — |
| 8 | `HALT` | Program Termination | — | Stops execution | — | — |

**Note:** in this implementation, the Z flag is recalculated after every instruction that produces
a result (via `CPU.updateStatus()`, called after `execute()` on each step), not only after
logical/arithmetic operations. This is a simplification for the simulator and is documented here
per the Week 2 instructions.

## Test Cases

| Test | Instruction | Precondition | Expected Result | Status |
|---|---|---|---|---|
| TC01 | `MOVLW 5` | W = 0 | W = 5 | PASS |
| TC02 | `MOVWF 20` | W = 5 | memory[20] = 5 | PASS |
| TC03 | `ADDLW 10` | W = 5 | W = 15, C = false | PASS |
| TC04 | `SUBLW 50` | W = 8 | W = 42, C = true | PASS |
| TC05 | `ANDLW 6` | W = 15 | W = 6, Z = false | PASS |
| TC06 | `INCF 20` | memory[20] = 5 | memory[20] = 6 | PASS |
| TC07 | `GOTO 8` | PC = 6 | PC jumps to 8, skipping line 7 | PASS |
| TC08 | `HALT` | — | Execution stops; further Step shows "Program already halted." | PASS |

## Demonstration Program

```
MOVLW 5      ; W = 5
MOVWF 20     ; memory[20] = W
ADDLW 10     ; W = W + 10
MOVWF 21     ; memory[21] = W
ANDLW 6      ; W = W AND 6
INCF 20      ; memory[20] = memory[20] + 1
GOTO 8       ; jump over the next line
MOVLW 99     ; skipped - proves GOTO works
SUBLW 50     ; W = 50 - W
HALT
```

This exercises all 8 implemented instructions and terminates via `HALT`.
