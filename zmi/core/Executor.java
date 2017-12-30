package core;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class Executor {
    private BlockingQueue<ExecuteContext> tasks = new ArrayBlockingQueue<>(100);

    public final void run() throws InterruptedException {
        while (true) {
            ExecuteContext context = tasks.take();
            execute(context);
        }
    }


    public final void pushToExecute(ExecuteContext context) {
        tasks.add(context);
    }

    abstract void execute(ExecuteContext context);

}
