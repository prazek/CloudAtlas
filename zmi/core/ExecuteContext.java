package core;

public class ExecuteContext {
    ExecuteContext(Executor sender, MessageOuterClass.Message data) {
        this.sender = sender;
        this.data = data;
    }

    Executor sender;
    MessageOuterClass.Message data;
}
