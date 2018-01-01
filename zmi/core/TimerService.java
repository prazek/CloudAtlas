package core;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.System.currentTimeMillis;

class TimerService extends TimerGrpc.TimerImplBase {
    private class TimerQueue extends Thread {
        private SortedMap<Long, List<Callback>> waiting = new TreeMap<>();

        synchronized long currentDelay() {
            long timestamp = System.currentTimeMillis();
            return waiting.firstKey() - timestamp;
        }

        synchronized void fireCallbacks() {
            long timestamp = System.currentTimeMillis();
            while (waiting.firstKey() <= timestamp) {
                List<Callback> callbacks = waiting.get(waiting.firstKey());
                waiting.remove(waiting.firstKey());
                for (Callback c: callbacks) {
                    c.responseObserver.onNext(TimerOuterClass.TimerResponse.newBuilder().setId(c.id).build());
                }
            }
        }

        synchronized void addCallback(long at, Callback callback) {
            List<Callback> callbackList = waiting.getOrDefault(at, new ArrayList<>());
            callbackList.add(callback);
            waiting.put(at, callbackList);
        }

        public void run() {

            try {
                if (waiting.isEmpty()) {
                    wait();
                } else {
                    long delay = currentDelay();
                    if (delay < 0) {
                        delay = 0;
                    }
                    wait(delay);
                }
            } catch (InterruptedException e) {
                // Do nothing
            }
            fireCallbacks();
        }
    }

    // TODO(sbarzowski) better name
    private class Callback {
        public int id;
        StreamObserver<TimerOuterClass.TimerResponse> responseObserver;
    }

    private TimerQueue timerQueue = new TimerQueue();

    @Override
    public void set(TimerOuterClass.TimerRequest request, StreamObserver<TimerOuterClass.TimerResponse> responseObserver) {
        Callback callback = new Callback();
        callback.id = request.getId();
        callback.responseObserver = responseObserver;
        timerQueue.addCallback(currentTimeMillis() + request.getDelay().getDuration(), callback);
    }

    public void startQueue() {
        timerQueue.start();
    }
}
