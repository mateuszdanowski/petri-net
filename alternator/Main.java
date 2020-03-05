package alternator;

import petrinet.PetriNet;
import petrinet.Transition;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {

    private enum Place {
        Before1, Between1, After1,
        Before2, Between2, After2,
        Before3, Between3, After3,
        CriticalSection,
        Semaphore
    }

    private static Place[][] places = {
            {Place.Before1, Place.Between1, Place.After1},
            {Place.Before2, Place.Between2, Place.After2},
            {Place.Before3, Place.Between3, Place.After3}};

    public static class Process implements Runnable {
        private Collection<Transition<Place>> transitions;
        private PetriNet<Place> net;
        private String name;

        public Process(PetriNet<Place> net, Collection<Transition<Place>> transitions, String name) {
            this.net = net;
            this.transitions = transitions;
            this.name = name;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    net.fire(transitions);
                    System.out.print(name);
                    System.out.print('.');
                    net.fire(transitions);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static Map<Place, Integer> putAll(Place a, Place b, Place c, Place d) {
        Map<Place, Integer> result = new HashMap<>();
        result.put(a, 1);
        result.put(b, 1);
        result.put(c, 1);
        result.put(d, 1);
        return result;
    }

    private static Transition<Place> createTransiton(int placeNumber, int transitionNumber) {
        Map<Place, Integer> input = new HashMap<>();
        Collection<Place> reset = new ArrayList<>();
        Collection<Place> inhibitor = new ArrayList<>();
        Map<Place, Integer> output = new HashMap<>();

        switch (transitionNumber) {
            case 0:
                input.put(places[placeNumber][0], 1);
                input.put(Place.Semaphore, 1);

                inhibitor.add(places[placeNumber][2]);
                reset.add(places[(placeNumber + 1) % 3][2]);
                reset.add(places[(placeNumber + 2) % 3][2]);

                output.put(places[placeNumber][1], 1);
                output.put(Place.CriticalSection, 1);
                break;
            case 1:
                input.put(places[placeNumber][1], 1);
                input.put(Place.CriticalSection, 1);

                output.put(places[placeNumber][0], 1);
                output.put(places[placeNumber][2], 1);
                output.put(Place.Semaphore, 1);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return new Transition<>(input, reset, inhibitor, output);
    }

    private static Collection<Transition<Place>> createTransitions(int placeNumber) {
        Collection<Transition<Place>> transitions = new ArrayList<>();

        for (int j = 0; j < 2; j++) {
            transitions.add(createTransiton(placeNumber, j));
        }

        return transitions;
    }

    private static boolean isThreadSafe(Set<Map<Place, Integer>> reachable) {
        for (Map<Place, Integer> marking : reachable) {
            if (marking.containsKey(Place.CriticalSection)) {
                if (marking.get(Place.CriticalSection) > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] args) {

        Map<Place, Integer> begin = putAll(Place.Before1, Place.Before2, Place.Before3, Place.Semaphore);

        PetriNet<Place> net = new PetriNet<>(begin, true);

        Collection<Transition<Place>> transitionsA = createTransitions(0);
        Collection<Transition<Place>> transitionsB = createTransitions(1);
        Collection<Transition<Place>> transitionsC = createTransitions(2);

        Collection<Transition<Place>> allTransitions = new ArrayList<>();
        allTransitions.addAll(transitionsA);
        allTransitions.addAll(transitionsB);
        allTransitions.addAll(transitionsC);

        Set<Map<Place, Integer>> reachable = net.reachable(allTransitions);

        System.out.println(reachable.size());

        boolean threadSafe = isThreadSafe(reachable);

        if (!threadSafe) {
            System.err.println("Net is not thread-safe");
            return;
        }

        Thread A = new Thread(new Process(net, transitionsA, "A"));
        Thread B = new Thread(new Process(net, transitionsB, "B"));
        Thread C = new Thread(new Process(net, transitionsC, "C"));

        A.start();
        B.start();
        C.start();

        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            throw new IllegalArgumentException();
        }

        A.interrupt();
        B.interrupt();
        C.interrupt();
        try {
            A.join();
            B.join();
            C.join();
        } catch (InterruptedException e) {
            throw new IllegalArgumentException();
        }
    }
}
