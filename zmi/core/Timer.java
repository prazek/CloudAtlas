package core;

import java.util.TimerTask;

public class Timer extends Executor {

    java.util.Timer timer = new java.util.Timer();
    void execute(ExecuteContext context) {
        // TODO time
        int time = 10;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // TODO
                context.sender.execute(new ExecuteContext());
                cancel();
            }
        }, time);
    }
}
