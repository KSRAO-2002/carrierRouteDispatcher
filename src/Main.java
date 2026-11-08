import runner.Phase1Runner;
import runner.Phase2Runner;

/**
 * ╔══════════════════════════════════════════════════╗
 * ║       CARRIER ROUTE DISPATCHER                  ║
 * ║       Java Concurrency Deep-Dive Project        ║
 * ╚══════════════════════════════════════════════════╝
 *
 * This project demonstrates the evolution of Java concurrency
 * from JDK 17 platform threads to JDK 21 virtual threads.
 *
 * Run this class with a phase number argument, or run each
 * PhaseRunner class directly from your IDE.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("       CARRIER ROUTE DISPATCHER");
        System.out.println("       Java Concurrency Deep-Dive");
        System.out.println("========================================================");
        System.out.println();

        if (args.length == 0) {
            printUsage();
            return;
        }

        switch (args[0]) {
            case "1" -> {
                System.out.println(">>> Running Phase 1...");
                System.out.println();
                Phase1Runner.main(args);
            }
            case "2" -> {
                System.out.println(">>> Running Phase 2...");
                System.out.println();
                Phase2Runner.main(args);
            }
            case "3" -> {
                System.out.println(">>> Running Phase 3 (requires JDK 21+)...");
                System.out.println();
                // Phase3Runner uses Executors.newVirtualThreadPerTaskExecutor()
                // which only compiles on JDK 21+.
                // Uncomment the line below when running on JDK 21:
                // runner.Phase3Runner.main(args);
                System.out.println("Phase 3 requires JDK 21+. Uncomment the runner in Main.java");
                System.out.println("or run Phase3Runner directly with JDK 21.");
            }
            case "all" -> {
                System.out.println(">>> Running Phase 1...");
                System.out.println();
                Phase1Runner.main(args);
                System.out.println();
                System.out.println("========================================================");
                System.out.println();
                System.out.println(">>> Running Phase 2...");
                System.out.println();
                Phase2Runner.main(args);
                System.out.println();
                System.out.println("========================================================");
                System.out.println();
                System.out.println(">>> Phase 3 requires JDK 21+ — run Phase3Runner directly.");
            }
            default -> printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java Main <phase>");
        System.out.println();
        System.out.println("  1    -> Phase 1: Fixed Thread Pool + ConcurrentHashMap (JDK 17)");
        System.out.println("  2    -> Phase 2: I/O Bottleneck & Thread Starvation   (JDK 17)");
        System.out.println("  3    -> Phase 3: Virtual Threads Revolution           (JDK 21+)");
        System.out.println("  all  -> Run Phase 1 and 2 sequentially");
        System.out.println();
        System.out.println("Or run each runner class directly from your IDE:");
        System.out.println("  runner.Phase1Runner");
        System.out.println("  runner.Phase2Runner");
        System.out.println("  runner.Phase3Runner  (JDK 21+ only)");
    }
}