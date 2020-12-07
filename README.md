# An interpreter an IDE for the Jet programming


## Grammar (pseud-BNF):
```
expr ::= expr op expr
  | (expr)
  | identifier
  | { expr, expr }
  | number
  | map(expr, identifier -> expr)
  | reduce(expr, expr, identifier identifier -> expr)
op ::= + | - | * | / | ^
stmt ::= var identifier = expr | out expr | print "string"
program ::= stmt | program stmt
```

## Example
```
var n = 500
var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))
var pi = 4 * reduce(sequence, 0, x y -> x + y)
print "pi = "
out pi
```

## Realization
- Program's source is first parsed by an ANTLR4-generated parser.
  The produced parse tree is then transformed into a slim executable tree.
  Currently, the parser doesn't perform any optimizations, but it shouldn't be difficult to implement them.
- I tried to follow the specification to the point, and didn't add any additional language constructs.
- A running program interacts with the run-time API which consumes results (output) produced by the program
  and provides access to an implementation of `Reducer` service (see below).
- Ranges and sequences (aka mapped ranges) are lazy and consume a negligible amount of memory.
  This means that range allocation and calling `map(sequence, lambda)` are very fast O(1) operations.
  Calling `toString()` on a large sequence is safe and produces an ellipsized representation.
  Expressions like `map({1, 10}, n -> {1, n})` create sequences of sequences.
- The language contains only one blocking (potentially long-running) operation, 'reduce'.
  Reduction can be executed using one of `Reducer` implementations. The IDE's run-time uses `ParallelReducer` which
  executes computations on `ForkJoinPool.commonPool` in `O(N/M)` time, where N stands for the sequence length,
  M for the number of available cores.
  By default, the common pool uses all but one available CPU cores.
  The remaining core is normally enough for keeping the UI responsive.
  Reduction doesn't require any significant amount of memory and triggers very low (< 1 %) GC activity.
  `ParallelReducer` produces correct results only for *associative* accumulator functions. An alternative implementation,
  `SequentialReducer` can work with non-associative functions and has `O(N)` performance.
- Cancellation of a program immediately terminates all computational jobs and releases CPU resources.
  I started with a coroutine-based approach, got a working version, but it looked quite cumbersome,
  so I decided to rewrite it using Java Streams and `CompletableFuture` which worked very well.
