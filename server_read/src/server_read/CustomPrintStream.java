package server_read;

import java.io.PrintStream;

// The best goddamn print that's ever existed on pure American hardware god damnit
public class CustomPrintStream extends PrintStream {
    //#region Constructor
    /**
     * Constructor
     * @param out The PrintStream to be wrapped
     */
    public CustomPrintStream(PrintStream out) {
        super(out);
    }
    //#endregion Constructor

    //#region Methods
    /**
     * Overriding print to add thread id to the beginning of the line
     */
    @Override
    public void print(String x) {
        super.print("[" + Thread.currentThread().getId() + "] " + x);
    }
    //#endregion Methods
}
