# Programming Project 3 — Pseudocode

**Team:** CADD  
**Members:** Colin Nykanen, Anna Le, David Arreola, Dayanna Porta Ayllon  
**Course:** CS 2430, section not specified (fill in before submission)  
**Project:** Programming Project 3 – Fall 2026

This document describes the current implementation in `src/Main.java`,
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
| `routines.PayloadOptimizer` | Store the source table and perform greedy and exhaustive searches. |
| `PayloadOptimizer.Payload` | Store an immutable selection and calculate its totals. |
| `PayloadOptimizer.ExhaustiveResult` | Store the subset count and the top three valid payloads. |

## 1. `Main`

### `main(args)` — Coordinate all three assignment parts

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

    search ← optimizer.exhaustiveSearch()
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
        IF payload.totalWeight() ≤ MAX_WEIGHT
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

None of the three greedy strategies matches the optimal rating with the current
tie rules. These results summarize the implementation; they are not hard-coded
answers used by the algorithms.
