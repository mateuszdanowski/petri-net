package petrinet;

import java.util.*;
import java.util.concurrent.Semaphore;

public class PetriNet<T> {

    public int getValue(T key) {
        if (!places.containsKey(key)) {
            return 0;
        }
        return places.get(key);
    }

    private Map<T, Integer> places;
    private Semaphore mutex;
    private Collection<WaitingProcess> waitingProcesses;

    private class WaitingProcess {
        Collection<Transition<T>> transitions;
        Semaphore sem;

        WaitingProcess(Collection<Transition<T>> transitions, Semaphore sem) {
            this.transitions = transitions;
            this.sem = sem;
        }
    }

    public PetriNet(Map<T, Integer> initial, boolean fair) {
        this.places = initial;
        this.mutex = new Semaphore(1, true);
        if (fair) {
            waitingProcesses = new LinkedList<>();
        }
        else {
            waitingProcesses = new HashSet<>();
        }
    }

    public Set<Map<T, Integer>> reachable(Collection<Transition<T>> transitions) {
        Set<Map<T, Integer>> result = new HashSet<>();
        Queue<Map<T, Integer>> queue = new LinkedList<>();

        Map<T, Integer> initial = new HashMap<>(places);
        Collection<Transition<T>> availableTransitions = new ArrayList<>(transitions);

        result.add(initial);
        queue.add(initial);

        while (!queue.isEmpty()) {
            Map<T, Integer> currentMarking = queue.remove();

            for (Transition<T> transition : availableTransitions) {
                Map<T, Integer> nextMarking = findNextMarking(currentMarking, transition);
                if (!result.contains(nextMarking)) {
                    result.add(nextMarking);
                    queue.add(nextMarking);
                }
            }
        }
        return result;
    }

    private Map<T, Integer> findNextMarking(Map<T, Integer> marking, Transition<T> transition) {
        Map<T, Integer> nextMarking = new HashMap<>(marking);
        if (transition.isEnabled(marking)) {
            transition.fire(nextMarking);
        }
        return nextMarking;
    }

    public Transition<T> fire(Collection<Transition<T>> transitions) throws InterruptedException {
        while (true) {
            mutex.acquire();
            for (Transition<T> transition : transitions) {
                if (transition.isEnabled(places)) {
                    transition.fire(places);
                    for (WaitingProcess waitingProcess : waitingProcesses) {
                        ArrayList<Transition<T>> transitionsAbleToFire =
                                getTransitionsAbleToFire(waitingProcess.transitions);
                        if (!transitionsAbleToFire.isEmpty()) {
                            waitingProcesses.remove(waitingProcess);
                            waitingProcess.sem.release();
                            break;
                        }
                    }
                    mutex.release();
                    return transition;
                }
            }
            Semaphore sem = new Semaphore(0);
            WaitingProcess waitingProcess = new WaitingProcess(transitions, sem);
            waitingProcesses.add(waitingProcess);
            mutex.release();
            sem.acquire();
        }
    }

    private ArrayList<Transition<T>> getTransitionsAbleToFire(Collection<Transition<T>> transitions) {
        ArrayList<Transition<T>> list = new ArrayList<>();
        for (Transition<T> t : transitions) {
            if (t.isEnabled(places)) {
                list.add(t);
            }
        }
        return list;
    }
}