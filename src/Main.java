/*
 * Team name: CADD
 * Team members: Colin Nykanen, Anna Le, David Arreola, Dayanna Porta Ayllon
 * Course: CS 2430, section not specified (fill in before submission)
 * Project: Programming Project 3 – Fall 2026
 */
import java.util.List;

import routines.Experiment;
import routines.PayloadOptimizer;
import routines.PayloadOptimizer.ExhaustiveResult;
import routines.PayloadOptimizer.Payload;

public class Main {
    public static void main(String[] args) {
        PayloadOptimizer optimizer = new PayloadOptimizer();
        System.out.println("Programming Project 3 - Space Shuttle Payload Optimization");
        System.out.printf("Capacity: %d kg | Experiments: %d | Whole experiments only%n",
                PayloadOptimizer.getMaxWeight(), optimizer.getExperiments().size());

        List<String> strategyNames = List.of("Highest Rating First", "Lightest First",
                "Best Rating-to-Weight Ratio First");
        List<Payload> greedyPayloads = List.of(optimizer.highestRatingFirst(),
                optimizer.lightestFirst(), optimizer.bestRatingToWeightRatioFirst());
        System.out.println("\nPART 1 - GREEDY STRATEGIES");
        System.out.println("Equal priorities retain the original experiment table order.");
        for (int index = 0; index < strategyNames.size(); index++) {
            printPayload(strategyNames.get(index), greedyPayloads.get(index));
        }

        long bruteForceStart = System.nanoTime();
        ExhaustiveResult search = optimizer.exhaustiveSearch();
        long bruteForceNanos = System.nanoTime() - bruteForceStart;
        System.out.println("\nPART 2 - BRUTE FORCE / EXHAUSTIVE SEARCH");
        System.out.printf("Subsets examined: %,d (including the empty subset)%n",
                search.subsetsExamined());
        System.out.println("Ranked by rating descending, then weight ascending, then subset mask.");
        for (int index = 0; index < search.topPayloads().size(); index++) {
            String title = "Rank " + (index + 1) + (index == 0 ? " - OPTIMAL" : "");
            printPayload(title, search.topPayloads().get(index));
        }

        printComparison(strategyNames, greedyPayloads, search.topPayloads().getFirst());
        printDynamicProgramming(optimizer, search.topPayloads().getFirst(), bruteForceNanos);
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
        System.out.printf("DP table: %d rows x %d columns; %,d cells filled after the base row.%n",
                optimizer.getExperiments().size() + 1, PayloadOptimizer.getMaxWeight() + 1,
                optimizer.getExperiments().size() * (PayloadOptimizer.getMaxWeight() + 1));
        System.out.printf("Observed algorithm runtime (one run): brute force %.3f ms | DP %.3f ms%n",
                bruteForceNanos / 1_000_000.0, dpNanos / 1_000_000.0);
        System.out.println("Timings vary with JVM warm-up and system load; these are not benchmarks.");
        System.out.println("DP: O(n * W) time and space; table filling plus subset traceback.");
        System.out.println("Brute force: O(n * 2^n) enumeration plus O(v * log(v) * n) ranking.");
        System.out.println("n = experiments, W = capacity in kg, v = valid subsets.");
        System.out.println("DP needs table indexing and reconstruction; brute-force subset logic is simpler.");
        System.out.println("DP avoids exponential growth in n when W is moderate, but is pseudo-polynomial in W.");
        System.out.println("DP returns one optimum; brute force remains necessary here for the top three subsets.");
    }

    private static void printPayload(String title, Payload payload) {
        System.out.println("\n" + title);
        System.out.printf("  %-30s %12s %8s%n", "Experiment", "Weight (kg)", "Rating");
        System.out.println("  ------------------------------------------------------");
        for (Experiment experiment : payload.experiments()) {
            System.out.printf("  %-30s %12d %8d%n", experiment.getName(),
                    experiment.getWeight(), experiment.getRating());
        }
        System.out.printf("  TOTAL: %d kg | Rating: %d%n", payload.totalWeight(), payload.totalRating());
    }

    private static void printComparison(List<String> names, List<Payload> greedyPayloads,
                                        Payload optimal) {
        System.out.println("\nPART 3 - COMPARISON SUMMARY");
        System.out.printf("%-35s %12s %8s %10s %s%n",
                "Strategy", "Weight (kg)", "Rating", "Rating gap", "Outcome");
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
        System.out.println("Greedy choices are locally best; only exhaustive search guarantees the optimum.");
    }
}

