# Programming Project 3 — Pseudocode

> **Original 12-experiment run (historical).** The application now uses the
> for current rules, evaluator, greedy deferral, exhaustive search, grouped DP,
> verified results, and test evidence. The pseudocode and totals below describe
> the pre-revision implementation, not the current executable.

**Team:** CADD  
**Members:** Colin Nykanen, Anna Le, David Arreola, Dayanna Porta Ayllon  
**Course:** CS 2430, section not specified (fill in before submission)  
**Project:** Programming Project 3 – Fall 2026

This document describes the original implementation in `src/Main.java`,
`src/routines/Experiment.java`, and `src/routines/PayloadOptimizer.java`.
The pseudocode is language-independent; names match the Java methods.
Lists use zero-based indexing, and `FOR EACH` visits items in list order.

## Goal and Class Responsibilities

Choose whole experiments with a combined weight of at most **700 kg**, maximizing
the sum of their ratings. An experiment may be selected once or not at all.

| Class or record | Responsibility |
|---|---|
| `Main` | Run the strategies and print payloads and comparison results. |
| `routines.Experiment` | Store an immutable name, weight, and rating. |
| `routines.PayloadOptimizer` | Store the source table and perform greedy, exhaustive, and DP searches. |
| `PayloadOptimizer.Payload` | Store an immutable selection and calculate its totals. |
| `PayloadOptimizer.ExhaustiveResult` | Store the subset count and the top three valid payloads. |

## 1. `Main`

### `main(args)` — Coordinate the three required parts and DP extra credit

The command-line arguments are unused. Each greedy strategy starts with the
same experiment table, not the previous strategy's selected items.

```text
PROCEDURE main(args)
    optimizer ← NEW PayloadOptimizer()
    PRINT project title
    PRINT optimizer capacity, number of experiments, and whole-item requirement

    strategyNames ← [
        "Highest Rating First",
        "Lightest First",
        "Best Rating-to-Weight Ratio First"
    ]
    greedyPayloads ← [
        optimizer.highestRatingFirst(),
        optimizer.lightestFirst(),
        optimizer.bestRatingToWeightRatioFirst()
    ]

    PRINT "PART 1 - GREEDY STRATEGIES"
    PRINT explanation that equal priorities retain source-table order
    FOR index FROM 0 THROUGH length(strategyNames) - 1
        CALL printPayload(strategyNames[index], greedyPayloads[index])
    END FOR

    bruteForceStart ← CURRENT MONOTONIC TIME
    search ← optimizer.exhaustiveSearch()
    bruteForceElapsed ← CURRENT MONOTONIC TIME - bruteForceStart
    PRINT "PART 2 - BRUTE FORCE / EXHAUSTIVE SEARCH"
    PRINT search.subsetsExamined
    PRINT explanation of rating, weight, and subset-mask ordering
    FOR index FROM 0 THROUGH length(search.topPayloads) - 1
        title ← "Rank " + (index + 1)
        IF index = 0
            title ← title + " - OPTIMAL"
        END IF
        CALL printPayload(title, search.topPayloads[index])
    END FOR

    optimal ← search.topPayloads[0]
    CALL printComparison(strategyNames, greedyPayloads, optimal)
    CALL printDynamicProgramming(optimizer, optimal, bruteForceElapsed)
END PROCEDURE
```

### `printDynamicProgramming(optimizer, optimal, bruteForceElapsed)`

The final payload is displayed instead of dumping the whole DP table. Timings
cover algorithm execution, not console output, and are single-run observations.

```text
PROCEDURE printDynamicProgramming(optimizer, optimal, bruteForceElapsed)
    dpStart ← CURRENT MONOTONIC TIME
    dpPayload ← optimizer.dynamicProgramming()
    dpElapsed ← CURRENT MONOTONIC TIME - dpStart

    PRINT "PART 4 - DYNAMIC PROGRAMMING"
    CALL printPayload("DP Optimal", dpPayload)
    PRINT whether dpPayload.totalRating() = optimal.totalRating()
    PRINT table dimensions (number of experiments + 1) BY (MAX_WEIGHT + 1)
    PRINT number of experiments × (MAX_WEIGHT + 1) cells filled after the base row
    PRINT bruteForceElapsed and dpElapsed converted to milliseconds
    PRINT caveat: JVM warm-up and system load affect single-run timings
    PRINT DP and brute-force runtime and code-complexity comparison
    PRINT that DP returns one optimum while brute force supplies the top three
END PROCEDURE
```

### `printPayload(title, payload)` — Display a complete selection

```text
PROCEDURE printPayload(title, payload)
    PRINT title
    PRINT table headings: Experiment, Weight (kg), Rating
    FOR EACH experiment IN payload.experiments
        PRINT experiment.getName(), experiment.getWeight(), experiment.getRating()
    END FOR
    PRINT "TOTAL", payload.totalWeight(), payload.totalRating()
END PROCEDURE
```

### `printComparison(names, greedyPayloads, optimal)` — Compare objective values

A strategy matches the optimum when its **total rating** matches. Its selected
items do not have to be identical to the exhaustive-search selection.

```text
PROCEDURE printComparison(names, greedyPayloads, optimal)
    PRINT "PART 3 - COMPARISON SUMMARY"
    PRINT headings: Strategy, Weight (kg), Rating, Rating gap, Outcome

    FOR index FROM 0 THROUGH length(names) - 1
        payload ← greedyPayloads[index]
        gap ← optimal.totalRating() - payload.totalRating()
        IF gap = 0
            outcome ← "MATCHED OPTIMAL"
        ELSE
            outcome ← "DID NOT MATCH OPTIMAL"
        END IF
        PRINT names[index], payload.totalWeight(), payload.totalRating(), gap, outcome
    END FOR

    PRINT "Brute Force Optimal", optimal.totalWeight(), optimal.totalRating(), 0, "TRUE BEST"
    PRINT that matching depends on total rating
    PRINT that greedy choices are locally best, but exhaustive search guarantees the optimum
END PROCEDURE
```

## 2. `routines.Experiment`

An experiment is an immutable data object. The constructor stores its inputs
unchanged; it does not validate or normalize them. The supplied optimization
table contains positive weights and ratings.

```text
CLASS Experiment
    PRIVATE READ-ONLY name
    PRIVATE READ-ONLY weight
    PRIVATE READ-ONLY rating

    CONSTRUCTOR Experiment(inputName, inputWeight, inputRating)
        name ← inputName
        weight ← inputWeight
        rating ← inputRating
    END CONSTRUCTOR

    FUNCTION getName()
        RETURN name
    END FUNCTION

    FUNCTION getWeight()
        RETURN weight
    END FUNCTION

    FUNCTION getRating()
        RETURN rating
    END FUNCTION
END CLASS
```

## 3. `routines.PayloadOptimizer`

### Constructor and source-table access

The table order is significant because stable sorting preserves it when greedy
priority scores tie. The source list is immutable.

```text
CONSTANT MAX_WEIGHT ← 700

CONSTRUCTOR PayloadOptimizer()
    experiments ← IMMUTABLE LIST OF Experiment(name, weight, rating):
        ("Cloud Patterns",             36, 5)
        ("Solar Flares",              264, 9)
        ("Solar Power",               188, 6)
        ("Binary Stars",              203, 8)
        ("Relativity",                104, 8)
        ("Seed Viability",              7, 4)
        ("Sun Spots",                  90, 2)
        ("Mice Tumors",                65, 8)
        ("Microgravity Plant Growth",  75, 5)
        ("Micrometeorites",            170, 9)
        ("Cosmic Rays",                80, 7)
        ("Yeast Fermentation",         27, 4)
END CONSTRUCTOR

FUNCTION getMaxWeight()
    RETURN MAX_WEIGHT
END FUNCTION

FUNCTION getExperiments()
    RETURN experiments
END FUNCTION
```

### Three greedy entry points

Each function supplies a different ordering rule to the shared `greedy` helper.
The ratio uses floating-point division, preserving fractional scores.

```text
FUNCTION highestRatingFirst()
    RETURN greedy(order by experiment.getRating() DESCENDING)
END FUNCTION

FUNCTION lightestFirst()
    RETURN greedy(order by experiment.getWeight() ASCENDING)
END FUNCTION

FUNCTION bestRatingToWeightRatioFirst()
    RETURN greedy(order by
        FLOAT(experiment.getRating()) / experiment.getWeight() DESCENDING)
END FUNCTION
```

### `greedy(priority)` — Shared whole-item selection

Sort a copy so the original table remains unchanged. If an item does not fit,
skip it and continue: later items may be lighter and still fit. Accepting an
item that brings the total to exactly 700 kg is allowed.

```text
FUNCTION greedy(priority)
    ordered ← MUTABLE COPY OF experiments
    STABLE SORT ordered USING priority
    selected ← EMPTY LIST
    totalWeight ← 0

    FOR EACH experiment IN ordered
        IF totalWeight + experiment.getWeight() ≤ MAX_WEIGHT
            APPEND experiment TO selected
            totalWeight ← totalWeight + experiment.getWeight()
        END IF
    END FOR

    RETURN NEW Payload(selected)
END FUNCTION
```

### `exhaustiveSearch()` — Enumerate and rank every subset

For 12 experiments, there are **2^12 = 4,096** subsets. A mask's bit at index
`i` is 1 when experiment `i` is included and 0 when excluded. Mask 0 represents
the empty subset. Every mask is examined, even if its resulting payload is
overweight.

```text
FUNCTION exhaustiveSearch()
    subsetCount ← 2 ^ length(experiments)
    validPayloads ← EMPTY LIST

    FOR mask FROM 0 THROUGH subsetCount - 1
        selected ← EMPTY LIST
        FOR index FROM 0 THROUGH length(experiments) - 1
            IF bit at position index in mask IS 1
                APPEND experiments[index] TO selected
            END IF
        END FOR

        payload ← NEW Payload(selected)
        totalWeight ← payload.totalWeight()
        COMPUTE payload.totalRating() even if the subset is overweight
        IF totalWeight ≤ MAX_WEIGHT
            APPEND payload TO validPayloads
        END IF
    END FOR

    STABLE SORT validPayloads BY:
        FIRST:  payload.totalRating() DESCENDING
        SECOND: payload.totalWeight() ASCENDING

    countToReturn ← MINIMUM(3, length(validPayloads))
    topPayloads ← FIRST countToReturn ITEMS OF validPayloads
    RETURN NEW ExhaustiveResult(subsetCount, topPayloads)
END FUNCTION
```

Higher rating is the objective; lighter weight only breaks rating ties. Since
masks are visited in ascending order, stable sorting also preserves ascending
mask order when both totals tie. The first returned payload is optimal.

### `dynamicProgramming()` — Exact 0/1 knapsack solution

`dp[i][capacity]` is the best total rating achievable using the first `i`
experiments within that weight limit. All cells initially contain zero. Reading
the previous row for both choices ensures that each experiment is used at most
once. This method optimizes rating only; ties skip the current experiment, rather
than applying exhaustive search's minimum-weight tie-breaker.

```text
FUNCTION dynamicProgramming()
    n ← length(experiments)
    dp ← ZERO-FILLED TABLE WITH (n + 1) ROWS AND (MAX_WEIGHT + 1) COLUMNS

    FOR i FROM 1 THROUGH n
        experiment ← experiments[i - 1]
        FOR capacity FROM 0 THROUGH MAX_WEIGHT
            dp[i][capacity] ← dp[i - 1][capacity]  // Skip the current item
            IF experiment.getWeight() ≤ capacity
                take ← experiment.getRating()
                        + dp[i - 1][capacity - experiment.getWeight()]
                dp[i][capacity] ← MAXIMUM(dp[i][capacity], take)
            END IF
        END FOR
    END FOR

    selected ← EMPTY LIST
    capacity ← MAX_WEIGHT
    FOR i FROM n DOWN THROUGH 1
        IF dp[i][capacity] ≠ dp[i - 1][capacity]
            experiment ← experiments[i - 1]
            APPEND experiment TO selected
            capacity ← capacity - experiment.getWeight()
        END IF
    END FOR
    REVERSE selected TO RESTORE SOURCE-TABLE ORDER
    RETURN NEW Payload(selected)
END FUNCTION
```

The optimum rating is `dp[n][700]`. Traceback reconstructs the selected subset;
unused capacity is allowed. For this integer-weight problem, DP is **exact**.

#### Runtime and Code Complexity Compared with Brute Force

- **DP:** O(n × W) time and space, plus O(n) traceback. The current table has
  13 × 701 cells, with 8,412 cells filled after its zero-initialized base row.
  Implementation requires table indexing, a recurrence, and reconstruction.
- **Brute force:** O(n × 2^n) enumeration and O(v log(v) × n) ranking of `v`
  valid subsets in the current implementation. Subset logic is simpler, but
  the search grows exponentially with the number of experiments.
- DP scales better in `n` when integer capacity `W` is moderate. It is
  pseudo-polynomial: a very large numeric capacity can still be expensive.
- Printed elapsed times vary by JVM warm-up and machine load; they are not
  controlled benchmarks or a guarantee that DP always runs faster.
- Keep exhaustive search for the required top-three subsets. This DP method
  reconstructs only one optimum, not the top three.

## 4. Nested Result Records

### `PayloadOptimizer.Payload`

Copying the selection into an immutable list prevents later changes to the
caller's list from changing the payload. Totals are calculated from the stored
experiments rather than stored separately.

```text
RECORD Payload
    READ-ONLY experiments

    CONSTRUCTOR Payload(selected)
        experiments ← IMMUTABLE COPY OF selected
    END CONSTRUCTOR

    FUNCTION experiments()
        RETURN experiments
    END FUNCTION

    FUNCTION totalWeight()
        RETURN SUM OF experiment.getWeight() FOR EACH experiment IN experiments
    END FUNCTION

    FUNCTION totalRating()
        RETURN SUM OF experiment.getRating() FOR EACH experiment IN experiments
    END FUNCTION
END RECORD
```

### `PayloadOptimizer.ExhaustiveResult`

The record preserves the number of subsets examined and an immutable snapshot
of the ranked results. Its accessors return those stored values.

```text
RECORD ExhaustiveResult
    READ-ONLY subsetsExamined
    READ-ONLY topPayloads

    CONSTRUCTOR ExhaustiveResult(count, rankedPayloads)
        subsetsExamined ← count
        topPayloads ← IMMUTABLE COPY OF rankedPayloads
    END CONSTRUCTOR

    FUNCTION subsetsExamined()
        RETURN subsetsExamined
    END FUNCTION

    FUNCTION topPayloads()
        RETURN topPayloads
    END FUNCTION
END RECORD
```

## Expected Results for the Supplied Table

| Strategy or exhaustive rank | Total weight (kg) | Total rating |
|---|---:|---:|
| Highest Rating First | 680 | 35 |
| Lightest First | 654 | 52 |
| Best Rating-to-Weight Ratio First | 654 | 52 |
| Exhaustive rank 1 — optimal | 692 | 53 |
| Exhaustive rank 2 | 654 | 52 |
| Exhaustive rank 3 | 677 | 51 |
| Dynamic Programming — optimal | 692 | 53 |

None of the three greedy strategies matches the optimal rating with the current
tie rules. DP matches the brute-force optimal rating of 53 and selects the same
subset for this table. These results summarize the implementation; they are not
hard-coded answers used by the algorithms.
