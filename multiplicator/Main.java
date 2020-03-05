package multiplicator;

import petrinet.PetriNet;
import petrinet.Transition;

import java.util.*;

public class Main {

    private enum Place {
        StartA, EndA, BeforeA, AfterA, B, Answer
    }

    private static Transition<Place> createTransiton(int transitionNumber) {
        Map<Place, Integer> input = new HashMap<>();
        Collection<Place> reset = new ArrayList<>();
        Collection<Place> inhibitor = new ArrayList<>();
        Map<Place, Integer> output = new HashMap<>();

        switch (transitionNumber) {
            case 0:
                input.put(Place.StartA, 1);
                input.put(Place.BeforeA, 1);

                output.put(Place.EndA, 1);
                output.put(Place.BeforeA, 1);
                output.put(Place.Answer, 1);
                break;
            case 1:
                input.put(Place.EndA, 1);
                input.put(Place.AfterA, 1);

                output.put(Place.StartA, 1);
                output.put(Place.AfterA, 1);
                break;
            case 2:
                input.put(Place.BeforeA, 1);

                inhibitor.add(Place.StartA);

                output.put(Place.AfterA, 1);
                break;
            case 3:
                input.put(Place.AfterA, 1);
                input.put(Place.B, 1);

                inhibitor.add(Place.EndA);

                output.put(Place.BeforeA, 1);
                break;
            case 4:
                inhibitor.add(Place.B);
                inhibitor.add(Place.EndA);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return new Transition<>(input, reset, inhibitor, output);
    }

    private static Collection<Transition<Place>> createAllTransitions() {
        Collection<Transition<Place>> transitions = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            transitions.add(createTransiton(i));
        }

        return transitions;
    }

    public static class Process implements Runnable {
        private Collection<Transition<Place>> transitions;
        private PetriNet<Place> net;
        private String name;
        private int count;

        public Process(PetriNet<Place> net, Collection<Transition<Place>> transitions, String name) {
            this.net = net;
            this.transitions = transitions;
            this.name = name;
            this.count = 0;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    net.fire(transitions);
                    count++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(name + " fired " + count + " times");
            }
        }
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int A = scanner.nextInt();
        int B = scanner.nextInt();

        if (A < B) {
            int pom = B;
            B = A;
            A = pom;
        }

        Map<Place, Integer> begin = new HashMap<>();
        begin.put(Place.StartA, A);
        begin.put(Place.AfterA, 1);
        begin.put(Place.B, B);
        begin.put(Place.Answer, 0);

        PetriNet<Place> net = new PetriNet<>(begin, true);

        Collection<Transition<Place>> allTransitions = createAllTransitions();

        ArrayList<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            threads.add(new Thread(new Process(net, allTransitions, "Process" + i)));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        Transition<Place> finalTransition = createTransiton(4);

        try {
            net.fire(Collections.singleton(finalTransition));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            for (Thread thread : threads) {
                thread.interrupt();
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(net.getValue(Place.Answer));
    }
}
