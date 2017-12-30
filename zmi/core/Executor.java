package core;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class Executor implements Runnable {
    private BlockingQueue<ExecuteContext> tasks = new ArrayBlockingQueue<>(100);
    protected Map<Integer, MessageOuterClass.Message> responses = new HashMap<>();
    // TODO make it threadsafe.
    protected int currentId;

    public final void run() {
        try {
            while (true) {
                ExecuteContext context = tasks.take();
                execute(context);
            }
        } catch(InterruptedException ex) {
            System.out.println("Exiting executor");
        }

    }


    public final void pushToExecute(ExecuteContext context) {
        tasks.add(context);
    }

    abstract void execute(ExecuteContext context);

}
