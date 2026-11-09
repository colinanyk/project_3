/*
 * Team name: CADD
 * Team members: Colin Nykanen, Anna Le, David Arreola, Dayanna Porta Ayllon
 * Course: CS 2430, section not specified (fill in before submission)
 * Project: Programming Project 3 – Fall 2026
 */
package routines;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PayloadOptimizer {
    private static final int MAX_WEIGHT = 700;
    private final List<Experiment> experiments;

    public PayloadOptimizer() {
        experiments = List.of(
                new Experiment("Cloud Patterns", 36, 5),
                new Experiment("Solar Flares", 264, 9),
                new Experiment("Solar Power", 188, 6),
                new Experiment("Binary Stars", 203, 8),
                new Experiment("Relativity", 104, 8),
                new Experiment("Seed Viability", 7, 4),
                new Experiment("Sun Spots", 90, 2),
                new Experiment("Mice Tumors", 65, 8),
                new Experiment("Microgravity Plant Growth", 75, 5),
                new Experiment("Micrometeorites", 170, 9),
                new Experiment("Cosmic Rays", 80, 7),
                new Experiment("Yeast Fermentation", 27, 4)
        );
    }

    public static int getMaxWeight() {
        return MAX_WEIGHT;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public Payload highestRatingFirst() {
        return greedy(Comparator.comparingInt(Experiment::getRating).reversed());
    }

    public Payload lightestFirst() {
        return greedy(Comparator.comparingInt(Experiment::getWeight));
    }

    public Payload bestRatingToWeightRatioFirst() {
        return greedy(Comparator.comparingDouble(
                (Experiment experiment) -> (double) experiment.getRating() / experiment.getWeight()
        ).reversed());
    }

    private Payload greedy(Comparator<Experiment> priority) {
        List<Experiment> ordered = new ArrayList<>(experiments);
        ordered.sort(priority);
        List<Experiment> selected = new ArrayList<>();
        int totalWeight = 0;
        for (Experiment experiment : ordered) {
            if (totalWeight + experiment.getWeight() <= MAX_WEIGHT) {
                selected.add(experiment);
                totalWeight += experiment.getWeight();
            }
        }
        return new Payload(selected);
    }

    public ExhaustiveResult exhaustiveSearch() {
        int subsetCount = 1 << experiments.size();
        List<Payload> validPayloads = new ArrayList<>();
        for (int mask = 0; mask < subsetCount; mask++) {
            List<Experiment> selected = new ArrayList<>();
            for (int index = 0; index < experiments.size(); index++) {
                if ((mask & (1 << index)) != 0) {
                    selected.add(experiments.get(index));
                }
            }
            Payload payload = new Payload(selected);
            if (payload.totalWeight() <= MAX_WEIGHT) {
                validPayloads.add(payload);
            }
        }

        validPayloads.sort(Comparator.comparingInt(Payload::totalRating).reversed()
                .thenComparingInt(Payload::totalWeight));
        return new ExhaustiveResult(subsetCount,
                validPayloads.subList(0, Math.min(3, validPayloads.size())));
    }

    public record Payload(List<Experiment> experiments) {
        public Payload {
            experiments = List.copyOf(experiments);
        }

        public int totalWeight() {
            return experiments.stream().mapToInt(Experiment::getWeight).sum();
        }

        public int totalRating() {
            return experiments.stream().mapToInt(Experiment::getRating).sum();
        }
    }

    public record ExhaustiveResult(int subsetsExamined, List<Payload> topPayloads) {
        public ExhaustiveResult {
            topPayloads = List.copyOf(topPayloads);
        }
    }
}
