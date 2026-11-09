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

        ExhaustiveResult search = optimizer.exhaustiveSearch();
        System.out.println("\nPART 2 - BRUTE FORCE / EXHAUSTIVE SEARCH");
        System.out.printf("Subsets examined: %,d (including the empty subset)%n",
                search.subsetsExamined());
        System.out.println("Ranked by rating descending, then weight ascending, then subset mask.");
        for (int index = 0; index < search.topPayloads().size(); index++) {
            String title = "Rank " + (index + 1) + (index == 0 ? " - OPTIMAL" : "");
            printPayload(title, search.topPayloads().get(index));
        }

        printComparison(strategyNames, greedyPayloads, search.topPayloads().getFirst());
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

