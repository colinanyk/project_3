/*
 * Team name: CADD
 * Team members: Colin Nykanen, Anna Le, David Arreola, Dayanna Porta Ayllon
 * Course: CS 2430, section not specified (fill in before submission)
 * Project: Programming Project 3 – Fall 2026
 */
import java.util.List;

import routines.Experiment;
import routines.PayloadOptimizer;
import routines.PayloadOptimizer.Evaluation;
import routines.PayloadOptimizer.ExhaustiveResult;
import routines.PayloadOptimizer.Payload;

public class Main {
    public static void main(String[] args) {
        PayloadOptimizer optimizer = new PayloadOptimizer();
        System.out.println("Programming Project 3 - REVISED Space Shuttle Payload Manifest");
        System.out.printf("Adjusted capacity: %d kg | Experiments: %d | Whole experiments only%n",
                PayloadOptimizer.getMaxWeight(), optimizer.getExperiments().size());

        List<String> strategyNames = List.of("Highest Rating First", "Lightest First",
                "Best Rating-to-Weight Ratio First");
        List<Payload> greedyPayloads = List.of(optimizer.highestRatingFirst(),
                optimizer.lightestFirst(), optimizer.bestRatingToWeightRatioFirst());
        System.out.println("\nPART 1 - GREEDY STRATEGIES");
        System.out.println("Equal priorities retain the original experiment table order.");
        System.out.println("Rankings: rating descending; baseline mass ascending; rating/baseline mass descending.");
        System.out.println("11a is deferred until 11 is accepted, then retried immediately; 11 is not auto-selected.");
        System.out.println("Every tentative addition checks all rules and adjusted mass; previous choices are not removed.");
        for (int index = 0; index < strategyNames.size(); index++) {
            printPayload(strategyNames.get(index), greedyPayloads.get(index));
        }

        long bruteForceStart = System.nanoTime();
        ExhaustiveResult search = optimizer.exhaustiveSearch();
        long bruteForceNanos = System.nanoTime() - bruteForceStart;
        System.out.println("\nPART 2 - BRUTE FORCE / EXHAUSTIVE SEARCH");
        System.out.printf("Subsets examined: %,d (including the empty subset)%n",
                search.subsetsExamined());
        System.out.println("Ranked by rating descending, then adjusted mass ascending, then subset mask.");
        for (int index = 0; index < search.topPayloads().size(); index++) {
            String title = "Rank " + (index + 1) + (index == 0 ? " - OPTIMAL" : "");
            printPayload(title, search.topPayloads().get(index));
        }

        printComparison(strategyNames, greedyPayloads, search.topPayloads().getFirst());
        printDynamicProgramming(optimizer, search.topPayloads().getFirst(), bruteForceNanos);
        printRuleTests(optimizer);
    }

    private static void printDynamicProgramming(PayloadOptimizer optimizer, Payload optimal,
                                                long bruteForceNanos) {
        long dpStart = System.nanoTime();
        Payload dpPayload = optimizer.dynamicProgramming();
        long dpNanos = System.nanoTime() - dpStart;

        System.out.println("\nPART 4 - DYNAMIC PROGRAMMING");
        printPayload("DP Optimal", dpPayload);
        System.out.println("DP matches brute-force optimal rating: "
                + (dpPayload.totalRating() == optimal.totalRating()));
        System.out.println("DP uses 9 disjoint groups: 3 related-package groups plus 6 singletons.");
        System.out.println("Each group offers legal bundles, including none; thermal bundle masses include the credit.");
        System.out.println("DP rating and traceback tables: each 10 rows x 701 columns.");
        System.out.printf("Observed algorithm runtime (one run): brute force %.3f ms | DP %.3f ms%n",
                bruteForceNanos / 1_000_000.0, dpNanos / 1_000_000.0);
        System.out.println("Timings vary with JVM warm-up and system load; these are not benchmarks.");
        System.out.println("Grouped DP: O(G * W * B) time and O(G * W) table space, plus bundle storage.");
        System.out.println("Brute force: O(n^2 * 2^n) evaluation bound plus O(v * log(v)) cached-total ranking.");
        System.out.println("n = packages, G = groups, B = max legal options/group (7), W = capacity, v = valid subsets.");
        System.out.println("The evaluator checks source-list membership per item, accounting for the extra n factor.");
        System.out.println("DP needs legal bundle generation and traceback; brute-force subset logic is simpler.");
        System.out.println("DP avoids exponential growth in n when W is moderate, but is pseudo-polynomial in W.");
        System.out.println("DP returns one optimum; this program uses brute force for the top three subsets.");
    }

    private static void printPayload(String title, Payload payload) {
        System.out.println("\n" + title);
        System.out.println("  Selected IDs: " + payload.ids());
        System.out.printf("  %-4s %-32s %12s %8s%n", "ID", "Experiment", "Baseline kg", "Rating");
        System.out.println("  ------------------------------------------------------");
        for (Experiment experiment : payload.experiments()) {
            System.out.printf("  %-4s %-32s %12d %8d%n", experiment.getId(), experiment.getName(),
                    experiment.getWeight(), experiment.getRating());
        }
        System.out.printf("  Baseline: %d kg | Credit: %d kg | ADJUSTED: %d kg | Rating: %d%n",
                payload.evaluation().baselineMass(), payload.evaluation().thermalCredit(),
                payload.totalWeight(), payload.totalRating());
    }

    private static void printComparison(List<String> names, List<Payload> greedyPayloads,
                                        Payload optimal) {
        System.out.println("\nPART 3 - COMPARISON SUMMARY");
        System.out.printf("%-35s %12s %8s %10s %s%n",
                "Strategy", "Adjusted kg", "Rating", "Rating gap", "Outcome");
        System.out.println("----------------------------------------------------------------------------------------");
        for (int index = 0; index < names.size(); index++) {
            Payload payload = greedyPayloads.get(index);
            int gap = optimal.totalRating() - payload.totalRating();
            System.out.printf("%-35s %12d %8d %10d %s%n", names.get(index),
                    payload.totalWeight(), payload.totalRating(), gap,
                    gap == 0 ? "MATCHED OPTIMAL" : "DID NOT MATCH OPTIMAL");
        }
        System.out.printf("%-35s %12d %8d %10d %s%n", "Brute Force Optimal",
                optimal.totalWeight(), optimal.totalRating(), 0, "TRUE BEST");
        System.out.println("A strategy matches the optimum if its total rating equals the best rating.");
        System.out.println("Greedy choices are local and never exchanged; exhaustive search and grouped DP are exact.");
        System.out.println("Heavy high-rating choices can crowd out better combinations; baseline rankings ignore future credits.");
        System.out.println("Deferring 11a preserves eligibility without forcing its lower-ranked prerequisite onto the manifest.");
    }

    private static void printRuleTests(PayloadOptimizer optimizer) {
        System.out.println("\nREVISED RULE TESTS - EXPECTED VS ACTUAL");
        printRuleTest(optimizer, "Solar limit", new String[]{"2", "3", "7"},
                new Evaluation(542, 0, 542, 17, List.of("At most two solar instruments are allowed")));
        printRuleTest(optimizer, "11a prerequisite", new String[]{"11a"},
                new Evaluation(10, 0, 10, 5, List.of("11a requires 11")));
        printRuleTest(optimizer, "Thermal credit", new String[]{"1", "2", "4", "6", "10", "12"},
                new Evaluation(707, 7, 700, 39, List.of()));
    }

    private static void printRuleTest(PayloadOptimizer optimizer, String title,
                                      String[] ids, Evaluation expected) {
        Evaluation actual = optimizer.evaluateIds(ids);
        System.out.println(title + " " + List.of(ids));
        System.out.println("  Expected: " + (expected.feasible() ? "ACCEPT " : "REJECT ") + expected);
        System.out.println("  Actual:   " + (actual.feasible() ? "ACCEPT " : "REJECT ") + actual);
        System.out.println("  " + (expected.equals(actual) ? "PASS" : "FAIL"));
    }
}

