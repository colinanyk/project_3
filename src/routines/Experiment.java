/*
 * Team name: CADD
 * Team members: Colin Nykanen, Anna Le, David Arreola, Dayanna Porta Ayllon
 * Course: CS 2430, section not specified (fill in before submission)
 * Project: Programming Project 3 – Fall 2026
 */
package routines;

public final class Experiment {
    private final String id;
    private final String name;
    private final int weight;
    private final int rating;

    public Experiment(String name, int weight, int rating) {
        this(null, name, weight, rating);
    }

    public Experiment(String id, String name, int weight, int rating) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.rating = rating;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public int getRating() {
        return rating;
    }
}
