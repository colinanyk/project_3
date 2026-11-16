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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import routines.PayloadOptimizer.ExhaustiveResult;
import routines.PayloadOptimizer.Payload;

import static org.junit.jupiter.api.Assertions.*;

class PayloadOptimizerTest {
    private final PayloadOptimizer optimizer = new PayloadOptimizer();

    @Test
    void constructorLoadsThirteenExperimentsWith700KgCapacity() {
        assertEquals(700, PayloadOptimizer.getMaxWeight());
        assertEquals(13, optimizer.getExperiments().size());
    }

    @ParameterizedTest
    @CsvSource({
            "0, Cloud Patterns, 36, 5", "1, Solar Flares, 264, 9",
            "2, Solar Power, 188, 6", "3, Binary Stars, 203, 8",
            "4, Relativity, 104, 8", "5, Seed Viability, 7, 4",
            "6, Sun Spots, 90, 2", "7, Mice Tumors, 65, 8",
            "8, Microgravity Plant Growth, 75, 5", "9, Micrometeorites, 170, 9",
            "10, Cosmic Rays (Primary Band), 80, 7",
            "11, Cosmic Rays (Secondary Band), 10, 5", "12, Yeast Fermentation, 27, 4"
    })
    void sourceTableMatchesAssignment(int index, String name, int weight, int rating) {
        Experiment experiment = optimizer.getExperiments().get(index);
        assertAll(
                () -> assertEquals(name, experiment.getName()),
                () -> assertEquals(weight, experiment.getWeight()),
                () -> assertEquals(rating, experiment.getRating())
        );
    }

    @Test
    void highestRatingFirstUsesStableTiesAndContinuesAfterOversizedItems() {
        Payload payload = optimizer.highestRatingFirst();
        assertEquals(List.of("Solar Flares", "Micrometeorites", "Binary Stars",
                "Cloud Patterns", "Seed Viability", "Yeast Fermentation"), names(payload));
        assertEquals(700, payload.totalWeight());
        assertEquals(39, payload.totalRating());
    }

    @Test
    void lightestFirstReturnsExpectedPayload() {
        Payload payload = optimizer.lightestFirst();
        assertEquals(List.of("Seed Viability", "Yeast Fermentation", "Cloud Patterns",
                "Mice Tumors", "Microgravity Plant Growth", "Cosmic Rays (Primary Band)",
                "Cosmic Rays (Secondary Band)", "Sun Spots",
                "Relativity", "Micrometeorites"), names(payload));
        assertEquals(657, payload.totalWeight());
        assertEquals(57, payload.totalRating());
    }

    @Test
    void ratioFirstUsesFractionalScoresAndSkipsItemsThatDoNotFit() {
        Payload payload = optimizer.bestRatingToWeightRatioFirst();
        assertEquals(List.of("Seed Viability", "Yeast Fermentation", "Cloud Patterns",
                "Mice Tumors", "Cosmic Rays (Primary Band)", "Cosmic Rays (Secondary Band)",
                "Relativity", "Microgravity Plant Growth",
                "Micrometeorites", "Sun Spots"), names(payload));
        assertEquals(657, payload.totalWeight());
        assertEquals(57, payload.totalRating());
    }

    @Test
    void exhaustiveSearchExamines8192SubsetsAndRanksTheTopThree() {
        ExhaustiveResult result = optimizer.exhaustiveSearch();
        assertEquals(8192, result.subsetsExamined());
        assertEquals(3, result.topPayloads().size());
        assertEquals(3, new HashSet<>(result.topPayloads()).size());
        assertEquals(List.of(58, 57, 56),
                result.topPayloads().stream().map(Payload::totalRating).toList());
        assertEquals(List.of(695, 657, 680),
                result.topPayloads().stream().map(Payload::totalWeight).toList());
        assertEquals(List.of("Cloud Patterns", "Binary Stars", "Relativity", "Seed Viability",
                "Mice Tumors", "Micrometeorites", "Cosmic Rays (Primary Band)",
                "Cosmic Rays (Secondary Band)", "Yeast Fermentation"),
                names(result.topPayloads().getFirst()));
    }

    @Test
    void dynamicProgrammingMatchesBruteForceOptimalRatingAndExpectedSubset() {
        Payload dp = optimizer.dynamicProgramming();
        Payload bruteForce = optimizer.exhaustiveSearch().topPayloads().getFirst();
        assertEquals(bruteForce.totalRating(), dp.totalRating());
        assertEquals(58, dp.totalRating());
        assertEquals(695, dp.totalWeight());
        assertEquals(List.of("Cloud Patterns", "Binary Stars", "Relativity", "Seed Viability",
                "Mice Tumors", "Micrometeorites", "Cosmic Rays (Primary Band)",
                "Cosmic Rays (Secondary Band)", "Yeast Fermentation"), names(dp));
    }

    @Test
    void dynamicProgrammingIsRepeatableAndLeavesSourceDataUnchanged() {
        List<Experiment> original = List.copyOf(optimizer.getExperiments());
        Payload first = optimizer.dynamicProgramming();
        optimizer.highestRatingFirst();
        optimizer.exhaustiveSearch();
        assertEquals(first, optimizer.dynamicProgramming());
        assertEquals(original, optimizer.getExperiments());
        assertThrows(UnsupportedOperationException.class, () -> first.experiments().clear());
    }

    @Test
    void topThreeAgreeWithIndependentRecursiveEnumeration() {
        List<Payload> allSubsets = new ArrayList<>();
        enumerateRecursively(0, new ArrayList<>(), allSubsets);
        assertEquals(8192, allSubsets.size());
        assertEquals(8192, new HashSet<>(allSubsets).size());
        List<Payload> expected = allSubsets.stream()
                .filter(this::independentlyFeasible)
                .sorted(Comparator.comparingInt(Payload::totalRating).reversed()
                        .thenComparingInt(Payload::totalWeight))
                .limit(3).toList();
        assertEquals(expected, optimizer.exhaustiveSearch().topPayloads());
    }

    @Test
    void everyReturnedPayloadIsValid() {
        List<Payload> payloads = new ArrayList<>(optimizer.exhaustiveSearch().topPayloads());
        payloads.addAll(List.of(optimizer.highestRatingFirst(), optimizer.lightestFirst(),
                optimizer.bestRatingToWeightRatioFirst(), optimizer.dynamicProgramming()));
        for (Payload payload : payloads) {
            assertTrue(payload.totalWeight() <= 700);
            assertTrue(payload.evaluation().feasible());
            assertTrue(independentlyFeasible(payload));
            assertEquals(payload.experiments().size(), new HashSet<>(payload.experiments()).size());
            assertTrue(optimizer.getExperiments().containsAll(payload.experiments()));
            assertEquals(payload.experiments().stream().mapToInt(Experiment::getWeight).sum()
                            - payload.evaluation().thermalCredit(),
                    payload.totalWeight());
            assertEquals(payload.experiments().stream().mapToInt(Experiment::getRating).sum(),
                    payload.totalRating());
        }
    }

    @Test
    void repeatedCallsAreIndependentAndNoneOfTheGreediesMatchesTheOptimum() {
        List<Experiment> original = List.copyOf(optimizer.getExperiments());
        Payload ratingFirst = optimizer.highestRatingFirst();
        Payload lightest = optimizer.lightestFirst();
        Payload ratio = optimizer.bestRatingToWeightRatioFirst();
        int optimum = optimizer.exhaustiveSearch().topPayloads().getFirst().totalRating();
        assertEquals(original, optimizer.getExperiments());
        assertEquals(ratingFirst, optimizer.highestRatingFirst());
        assertEquals(lightest, optimizer.lightestFirst());
        assertEquals(ratio, optimizer.bestRatingToWeightRatioFirst());
        for (Payload payload : List.of(ratingFirst, lightest, ratio)) {
            assertTrue(payload.totalRating() < optimum);
        }
    }

    @Test
    void sourceAndResultsCannotBeModified() {
        List<Experiment> selected = new ArrayList<>(optimizer.getExperiments().subList(0, 2));
        Payload payload = new Payload(selected, optimizer.evaluate(selected));
        selected.clear();
        assertEquals(2, payload.experiments().size());
        assertEquals(300, payload.totalWeight());
        assertEquals(14, payload.totalRating());
        assertThrows(UnsupportedOperationException.class, () -> optimizer.getExperiments().clear());
        assertThrows(UnsupportedOperationException.class, () -> payload.experiments().clear());
        ExhaustiveResult result = optimizer.exhaustiveSearch();
        assertThrows(UnsupportedOperationException.class, () -> result.topPayloads().clear());
    }

    private static List<String> names(Payload payload) {
        return payload.experiments().stream().map(Experiment::getName).toList();
    }

    private void enumerateRecursively(int index, List<Experiment> selected, List<Payload> output) {
        if (index == optimizer.getExperiments().size()) {
            output.add(new Payload(selected, optimizer.evaluate(selected)));
            return;
        }
        enumerateRecursively(index + 1, selected, output);
        selected.add(optimizer.getExperiments().get(index));
        enumerateRecursively(index + 1, selected, output);
        selected.removeLast();
    }

    private boolean independentlyFeasible(Payload payload) {
        var ids = payload.ids();
        int baseline = payload.experiments().stream().mapToInt(Experiment::getWeight).sum();
        int adjusted = baseline - (ids.contains("6") && ids.contains("12") ? 7 : 0);
        return adjusted <= 700
                && !(ids.contains("2") && ids.contains("3") && ids.contains("7"))
                && (!ids.contains("11a") || ids.contains("11"));
    }

    @Test
    void solarLimitRejectsThreeButAllowsTwo() {
        var result = optimizer.evaluateIds("2", "3", "7");
        assertFalse(result.feasible());
        assertEquals(542, result.adjustedMass());
        assertEquals(17, result.totalRating());
        assertEquals(List.of("At most two solar instruments are allowed"), result.reasons());
        for (String[] pair : List.of(new String[]{"2", "3"},
                new String[]{"2", "7"}, new String[]{"3", "7"})) {
            assertTrue(optimizer.evaluateIds(pair).feasible());
        }
    }

    @Test
    void secondaryRequiresPrimary() {
        assertEquals(List.of("11a requires 11"), optimizer.evaluateIds("11a").reasons());
        assertTrue(optimizer.evaluateIds("11").feasible());
        var pair = optimizer.evaluateIds("11", "11a");
        assertTrue(pair.feasible());
        assertEquals(90, pair.adjustedMass());
        assertEquals(12, pair.totalRating());
    }

    @Test
    void thermalCreditMakes707KgCandidateFeasible() {
        var result = optimizer.evaluateIds("1", "2", "4", "6", "10", "12");
        assertTrue(result.feasible());
        assertEquals(707, result.baselineMass());
        assertEquals(7, result.thermalCredit());
        assertEquals(700, result.adjustedMass());
        assertEquals(39, result.totalRating());
        assertEquals(0, optimizer.evaluateIds("6").thermalCredit());
        assertEquals(0, optimizer.evaluateIds("12").thermalCredit());
        assertEquals(27, optimizer.evaluateIds("6", "12").adjustedMass());
        assertEquals(8, optimizer.evaluateIds("6", "12").totalRating());
        assertFalse(optimizer.evaluateIds("1", "2", "4", "6", "10", "12", "7").feasible());
    }

    @Test
    void greedyDefersSecondaryAndDoesNotForcePrimary() {
        for (Payload payload : List.of(optimizer.lightestFirst(),
                optimizer.bestRatingToWeightRatioFirst())) {
            assertTrue(payload.ids().containsAll(List.of("11", "11a")));
            assertEquals(payload.ids().indexOf("11") + 1, payload.ids().indexOf("11a"));
        }
        assertFalse(optimizer.highestRatingFirst().ids().contains("11"));
        assertFalse(optimizer.highestRatingFirst().ids().contains("11a"));
    }

    @Test
    void evaluatorHandlesEmptyDuplicatesAndUnknowns() {
        assertTrue(optimizer.evaluateIds().feasible());
        assertEquals(0, optimizer.evaluateIds().adjustedMass());
        assertFalse(optimizer.evaluateIds("1", "1").feasible());
        assertThrows(IllegalArgumentException.class, () -> optimizer.getExperiment("missing"));
        assertThrows(IllegalArgumentException.class,
                () -> optimizer.evaluate(List.of(new Experiment("fake", 1, 1))));
    }
}
