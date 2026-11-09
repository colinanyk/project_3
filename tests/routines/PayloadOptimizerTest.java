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
    void constructorLoadsTwelveExperimentsWith700KgCapacity() {
        assertEquals(700, PayloadOptimizer.getMaxWeight());
        assertEquals(12, optimizer.getExperiments().size());
    }

    @ParameterizedTest
    @CsvSource({
            "0, Cloud Patterns, 36, 5", "1, Solar Flares, 264, 9",
            "2, Solar Power, 188, 6", "3, Binary Stars, 203, 8",
            "4, Relativity, 104, 8", "5, Seed Viability, 7, 4",
            "6, Sun Spots, 90, 2", "7, Mice Tumors, 65, 8",
            "8, Microgravity Plant Growth, 75, 5", "9, Micrometeorites, 170, 9",
            "10, Cosmic Rays, 80, 7", "11, Yeast Fermentation, 27, 4"
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
                "Cloud Patterns", "Seed Viability"), names(payload));
        assertEquals(680, payload.totalWeight());
        assertEquals(35, payload.totalRating());
    }

    @Test
    void lightestFirstReturnsExpectedPayload() {
        Payload payload = optimizer.lightestFirst();
        assertEquals(List.of("Seed Viability", "Yeast Fermentation", "Cloud Patterns",
                "Mice Tumors", "Microgravity Plant Growth", "Cosmic Rays", "Sun Spots",
                "Relativity", "Micrometeorites"), names(payload));
        assertEquals(654, payload.totalWeight());
        assertEquals(52, payload.totalRating());
    }

    @Test
    void ratioFirstUsesFractionalScoresAndSkipsItemsThatDoNotFit() {
        Payload payload = optimizer.bestRatingToWeightRatioFirst();
        assertEquals(List.of("Seed Viability", "Yeast Fermentation", "Cloud Patterns",
                "Mice Tumors", "Cosmic Rays", "Relativity", "Microgravity Plant Growth",
                "Micrometeorites", "Sun Spots"), names(payload));
        assertEquals(654, payload.totalWeight());
        assertEquals(52, payload.totalRating());
    }

    @Test
    void exhaustiveSearchExamines4096SubsetsAndRanksTheTopThree() {
        ExhaustiveResult result = optimizer.exhaustiveSearch();
        assertEquals(4096, result.subsetsExamined());
        assertEquals(3, result.topPayloads().size());
        assertEquals(3, new HashSet<>(result.topPayloads()).size());
        assertEquals(List.of(53, 52, 51),
                result.topPayloads().stream().map(Payload::totalRating).toList());
        assertEquals(List.of(692, 654, 677),
                result.topPayloads().stream().map(Payload::totalWeight).toList());
        assertEquals(List.of("Cloud Patterns", "Binary Stars", "Relativity", "Seed Viability",
                "Mice Tumors", "Micrometeorites", "Cosmic Rays", "Yeast Fermentation"),
                names(result.topPayloads().getFirst()));
    }

    @Test
    void topThreeAgreeWithIndependentRecursiveEnumeration() {
        List<Payload> allSubsets = new ArrayList<>();
        enumerateRecursively(0, new ArrayList<>(), allSubsets);
        assertEquals(4096, allSubsets.size());
        assertEquals(4096, new HashSet<>(allSubsets).size());
        List<Payload> expected = allSubsets.stream()
                .filter(payload -> payload.totalWeight() <= 700)
                .sorted(Comparator.comparingInt(Payload::totalRating).reversed()
                        .thenComparingInt(Payload::totalWeight))
                .limit(3).toList();
        assertEquals(expected, optimizer.exhaustiveSearch().topPayloads());
    }

    @Test
    void everyReturnedPayloadIsValid() {
        List<Payload> payloads = new ArrayList<>(optimizer.exhaustiveSearch().topPayloads());
        payloads.addAll(List.of(optimizer.highestRatingFirst(), optimizer.lightestFirst(),
                optimizer.bestRatingToWeightRatioFirst()));
        for (Payload payload : payloads) {
            assertTrue(payload.totalWeight() <= 700);
            assertEquals(payload.experiments().size(), new HashSet<>(payload.experiments()).size());
            assertTrue(optimizer.getExperiments().containsAll(payload.experiments()));
            assertEquals(payload.experiments().stream().mapToInt(Experiment::getWeight).sum(),
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
        Payload payload = new Payload(selected);
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
            output.add(new Payload(selected));
            return;
        }
        enumerateRecursively(index + 1, selected, output);
        selected.add(optimizer.getExperiments().get(index));
        enumerateRecursively(index + 1, selected, output);
        selected.removeLast();
    }
}
