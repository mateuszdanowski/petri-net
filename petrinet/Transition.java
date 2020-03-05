package petrinet;

import java.util.Collection;
import java.util.Map;

public class Transition<T> {

    private Map<T, Integer> input;
    private Collection<T> reset;
    private Collection<T> inhibitor;
    private Map<T, Integer> output;

    public Transition(Map<T, Integer> input, Collection<T> reset, Collection<T> inhibitor, Map<T, Integer> output) {
        this.input = input;
        this.reset = reset;
        this.inhibitor = inhibitor;
        this.output = output;
    }

    public boolean isEnabled(Map<T, Integer> places) {

        for (Map.Entry<T, Integer> arc : input.entrySet()) {
            T place = arc.getKey();
            Integer weight = arc.getValue();
            if (!places.containsKey(place) || places.get(place) < weight) {
                return false;
            }
        }

        for (T place : inhibitor) {
            if (places.containsKey(place) && places.get(place) > 0) {
                return false;
            }
        }
        return true;
    }

    public void fire(Map<T, Integer> places) {

        for (Map.Entry<T, Integer> arc : input.entrySet()) {
            T place = arc.getKey();
            Integer weight = arc.getValue();
            Integer tokens = places.get(place);

            if (tokens - weight > 0) {
                places.replace(place, tokens - weight);
            }
            else {
                places.remove(place);
            }
        }

        for (T place : reset) {
            places.remove(place);
        }

        for (Map.Entry<T, Integer> arc : output.entrySet()) {
            T place = arc.getKey();
            Integer weight = arc.getValue();

            if (!places.containsKey(place)) {
                places.put(place, 0);
            }
            Integer tokens = places.get(place);

            places.replace(place, tokens + weight);

            if (places.get(place) == 0) {
                places.remove(place);
            }
        }
    }
}