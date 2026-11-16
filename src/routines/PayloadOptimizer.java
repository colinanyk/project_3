/*
 * Team name: CADD
 * Team members: Colin Nykanen, Anna Le, David Arreola, Dayanna Porta Ayllon
 * Course: CS 2430, section not specified (fill in before submission)
 * Project: Programming Project 3 – Fall 2026
 */
package routines;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PayloadOptimizer {
    private static final int MAX_WEIGHT = 700;
    private final List<Experiment> experiments;

    public PayloadOptimizer() {
        experiments = List.of(
                new Experiment("1", "Cloud Patterns", 36, 5),
                new Experiment("2", "Solar Flares", 264, 9),
                new Experiment("3", "Solar Power", 188, 6),
                new Experiment("4", "Binary Stars", 203, 8),
                new Experiment("5", "Relativity", 104, 8),
                new Experiment("6", "Seed Viability", 7, 4),
                new Experiment("7", "Sun Spots", 90, 2),
                new Experiment("8", "Mice Tumors", 65, 8),
                new Experiment("9", "Microgravity Plant Growth", 75, 5),
                new Experiment("10", "Micrometeorites", 170, 9),
                new Experiment("11", "Cosmic Rays (Primary Band)", 80, 7),
                new Experiment("11a", "Cosmic Rays (Secondary Band)", 10, 5),
                new Experiment("12", "Yeast Fermentation", 27, 4)
        );
    }

    public static int getMaxWeight() {
        return MAX_WEIGHT;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public Experiment getExperiment(String id) {
        return experiments.stream().filter(item -> item.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown ID: " + id));
    }

    public Evaluation evaluateIds(String... ids) {
        return evaluate(List.of(ids).stream().map(this::getExperiment).toList());
    }

    public Evaluation evaluate(List<Experiment> selected) {
        Set<String> ids = new HashSet<>();
        List<String> reasons = new ArrayList<>();
        int baseline = 0;
        int rating = 0;
        for (Experiment item : selected) {
            if (!experiments.contains(item)) {
                throw new IllegalArgumentException("Use this optimizer's source experiments");
            }
            if (!ids.add(item.getId())) {
                reasons.add("Duplicate ID: " + item.getId());
            }
            baseline += item.getWeight();
            rating += item.getRating();
        }
        long solarCount = List.of("2", "3", "7").stream().filter(ids::contains).count();
        if (solarCount > 2) {
            reasons.add("At most two solar instruments are allowed");
        }
        if (ids.contains("11a") && !ids.contains("11")) {
            reasons.add("11a requires 11");
        }
        int credit = ids.contains("6") && ids.contains("12") ? 7 : 0;
        int adjusted = baseline - credit;
        if (adjusted > MAX_WEIGHT) {
            reasons.add("Adjusted mass exceeds 700 kg");
        }
        return new Evaluation(baseline, credit, adjusted, rating, reasons);
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
        Experiment deferredSecondary = null;
        for (Experiment experiment : ordered) {
            if (experiment.getId().equals("11a") && !selected.contains(getExperiment("11"))) {
                deferredSecondary = experiment;
                continue;
            }
            boolean accepted = tryAdd(selected, experiment);
            if (accepted && experiment.getId().equals("11") && deferredSecondary != null) {
                tryAdd(selected, deferredSecondary);
                deferredSecondary = null;
            }
        }
        return new Payload(selected, evaluate(selected));
    }

    private boolean tryAdd(List<Experiment> selected, Experiment experiment) {
        List<Experiment> candidate = new ArrayList<>(selected);
        candidate.add(experiment);
        if (!evaluate(candidate).feasible()) {
            return false;
        }
        selected.add(experiment);
        return true;
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
            Evaluation evaluation = evaluate(selected);
            if (evaluation.feasible()) {
                validPayloads.add(new Payload(selected, evaluation));
            }
        }

        validPayloads.sort(Comparator.comparingInt(Payload::totalRating).reversed()
                .thenComparingInt(Payload::totalWeight));
        return new ExhaustiveResult(subsetCount,
                validPayloads.subList(0, Math.min(3, validPayloads.size())));
    }

    public Payload dynamicProgramming() {
        List<List<Payload>> groups = List.of(
                groupOptions("1"), groupOptions("2", "3", "7"),
                groupOptions("4"), groupOptions("5"), groupOptions("6", "12"),
                groupOptions("8"), groupOptions("9"), groupOptions("10"),
                groupOptions("11", "11a"));
        int[][] dp = new int[groups.size() + 1][MAX_WEIGHT + 1];
        int[][] choice = new int[groups.size() + 1][MAX_WEIGHT + 1];
        for (int g = 1; g <= groups.size(); g++) {
            List<Payload> options = groups.get(g - 1);
            for (int capacity = 0; capacity <= MAX_WEIGHT; capacity++) {
                dp[g][capacity] = dp[g - 1][capacity];
                for (int option = 1; option < options.size(); option++) {
                    Payload bundle = options.get(option);
                    if (bundle.totalWeight() <= capacity) {
                        int rating = bundle.totalRating()
                                + dp[g - 1][capacity - bundle.totalWeight()];
                        if (rating > dp[g][capacity]) {
                            dp[g][capacity] = rating;
                            choice[g][capacity] = option;
                        }
                    }
                }
            }
        }
        List<Experiment> selected = new ArrayList<>();
        int capacity = MAX_WEIGHT;
        for (int g = groups.size(); g > 0; g--) {
            Payload bundle = groups.get(g - 1).get(choice[g][capacity]);
            selected.addAll(bundle.experiments());
            capacity -= bundle.totalWeight();
        }
        selected.sort(Comparator.comparingInt(experiments::indexOf));
        return new Payload(selected, evaluate(selected));
    }

    private List<Payload> groupOptions(String... ids) {
        List<Payload> options = new ArrayList<>();
        for (int mask = 0; mask < (1 << ids.length); mask++) {
            List<Experiment> selected = new ArrayList<>();
            for (int i = 0; i < ids.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    selected.add(getExperiment(ids[i]));
                }
            }
            Evaluation evaluation = evaluate(selected);
            if (evaluation.feasible()) {
                options.add(new Payload(selected, evaluation));
            }
        }
        return List.copyOf(options);
    }

    public record Evaluation(int baselineMass, int thermalCredit, int adjustedMass,
                             int totalRating, List<String> reasons) {
        public Evaluation {
            reasons = List.copyOf(reasons);
        }

        public boolean feasible() {
            return reasons.isEmpty();
        }
    }

    public record Payload(List<Experiment> experiments, Evaluation evaluation) {
        public Payload {
            experiments = List.copyOf(experiments);
        }

        public int totalWeight() {
            return evaluation.adjustedMass();
        }

        public int totalRating() {
            return evaluation.totalRating();
        }

        public List<String> ids() {
            return experiments.stream().map(Experiment::getId).toList();
        }
    }

    public record ExhaustiveResult(int subsetsExamined, List<Payload> topPayloads) {
        public ExhaustiveResult {
            topPayloads = List.copyOf(topPayloads);
        }
    }
}
