import java.sql.Array;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntConsumer;

public class Main {
    public static void main(String[] args) throws Exception {
        //CLI basic setup
        if(args.length == 0 || args[0] == "-h")
        {
            System.out.println("Usage:");
            System.out.println("    Test functions for equality: test [num-threads]" );
            System.out.println("        e.x.: test 6" );
            System.out.println("    Check function speed: speed [integers-per-test] [iterations]" );
            System.out.println("        e.x.: speed 134217728 100" );
            return;
        }
        for(var i = 0; i < args.length;i++)
        {
            args[i] = args[i].toLowerCase();
        }
        //Run test function, asserting each function is equal (within a floating range)
        if(args[0].equals("test"))
        {
            int threads = Runtime.getRuntime().availableProcessors()-2;;
            if(args.length > 1)
            {
                threads = Integer.parseInt(args[1]);
            }
            System.out.println("Running test with : " + threads + " threads");
            Test(threads);
        }
        else if(args[0].equals("speed"))
        {
            int intsToCheck = 134_217_728;
            int iterations = 100;
            if(args.length > 1)
            {
                intsToCheck = Integer.parseInt(args[1]);
            }
            if(args.length > 2)
            {
                iterations = Integer.parseInt(args[2]);
            }
            System.out.printf("Starting speed test: %d random numbers per iteration, %d iterations\n",intsToCheck,iterations);
            SpeedCheck(intsToCheck,iterations);
        }
    }

    public static List<TestFunction> GenerateTestFunctionList()
    {
        List<TestFunction> functions = new ArrayList<>();

        functions.add(new TestFunction(Main::SimpleString,"SimpleString"));
        functions.add(new TestFunction(Main::StringMath,"StringMath"));
        functions.add(new TestFunction(Main::PureMath,"PureMath"));
        functions.add(new TestFunction(Main::WeakAlgorithm,"WeakAlgorithm"));
        functions.add(new TestFunction(Main::MediumAlgorithm,"MediumAlgorithm"));
        functions.add(new TestFunction(Main::RecursiveDivide,"RecursiveDivide"));
        functions.add(new TestFunction(Main::IfMadness,"IfMadness"));

        return functions;
    }

    //Test the performance of each function
    public static void SpeedCheck(int numIntegersToTest, int totalIterations )
    {
        List<TestFunction> functions = GenerateTestFunctionList();
        Random rand = new Random();
        long speedStartTime = System.nanoTime();
        int[] nums = new int[numIntegersToTest];
        HashMap<TestFunction,Long> times = new HashMap<>();

        //Setup time recording
        for(var tfunc: functions)
        {
            times.put(tfunc,0L);
        }

        //Do this a bunch of times randomizing order each time to minimize order related performance (caching?)
        for(var i = 0; i < totalIterations;i++)
        {
            //Shuffle order of functions
            Collections.shuffle(functions);
            //Set up numbers
            for (var j = 0; j < numIntegersToTest; j++)
            {
                nums[j]= rand.nextInt(Integer.MAX_VALUE);
            }


            for (var tfunc : functions)
            {
                long startTime = System.nanoTime();

                //loop over enough number and apply function
                for (var j = 0; j < numIntegersToTest; j++) {
                    tfunc.func.apply(nums[j]);
                }
                long endTime = System.nanoTime();

                long nanoseconds = (endTime - startTime);

                times.put(tfunc,times.get(tfunc)+nanoseconds);

                System.out.println((i+1) + " : " + tfunc);

            }
            //Print out progress report
            System.out.printf("%d / %d iterations completed, %.4f%% complete.  Time elapsed: %.4f seconds\n  ",
                    i+1,totalIterations,(double)(i+1)/(totalIterations)*100.0,
                    (System.nanoTime()-speedStartTime)/1_000_000_000.0);
        }

        //print out results
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("Speed test complete.  Total time taken: " + (System.nanoTime()-speedStartTime)/1_000_000_000.0 + " seconds.  Results:");
        System.out.println();
        System.out.println();
        System.out.println();
        //sort so best is last
        functions.sort(Comparator.comparingLong(times::get).reversed());
        for(var tfunc : functions)
        {
            double totalTimeSeconds = times.get(tfunc)/1_000_000_000.0;
            long totalNumbersTested = (long)numIntegersToTest * (long)totalIterations;
            double numbersPerSecond = totalNumbersTested/totalTimeSeconds;
            System.out.printf("%d numbers in %.4f seconds\n",totalNumbersTested,totalTimeSeconds);
            System.out.println(tfunc + " crunched " + (long)Math.floor(numbersPerSecond) + " numbers per second rounded down");
            System.out.println();

        }
    }
    /***
     * This function tests every function we have with every positive (32 bit signed) int.
     * We use the simple string function as the most accurate answer and compare others to it.
     */
    public static void Test(int numThreads)
    {
        TestFunction truth = new TestFunction(Main::SimpleString,"SimpleString");

        List<TestFunction> functions = GenerateTestFunctionList();

        //Note: May want to change this number if you don't wanna use a lot of your CPU
        numThreads = Math.max(1,numThreads);
        System.out.println("Testing on : " + numThreads + " threads");

        RunTests(functions,truth,0.0001,numThreads);
    }
    public static void RunTests(List<TestFunction> functions, TestFunction truth, double errorMargin, int threads)
    {
        long startTime = System.nanoTime();
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        List<Runnable> runnables = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        for(var func : functions)
        {
            Runnable runnable = () -> {
                var result = TestValues(truth, func, 0.0001);
                if(!result)
                {
                    System.out.println("Failed on : " + func.name);
                    count.incrementAndGet();
                }
                else
                {
                    System.out.println("Succeeded on : " + func.name);
                }
            };
            runnables.add(runnable);
        }
        System.out.println("Functions to test: " + runnables.size());
        for(var runnable: runnables)
        {
            executorService.submit(runnable);
        }

        executorService.shutdown();

        try
        {
            executorService.awaitTermination(2, TimeUnit.DAYS);
        }
        catch(Exception e)
        {
            System.out.println("Could not finish execution");
        }
        long endTime = System.nanoTime();
        System.out.println("Number of failures: " + count.get());
        double seconds = (endTime-startTime) / 1_000_000_000.0;
        System.out.println("Testing took : " + seconds + " seconds");


    }
    //Test two functions to see if they line up in every possible output.
    public static boolean TestValues(TestFunction truth, TestFunction test, double errorMargin)
    {
        Random rand = new Random();
        for(int i = 0; i < Integer.MAX_VALUE;i++)
        {
            int n = i;
            if(n % 10000000 == 0)
            {
                var percent = ((double)n) / Integer.MAX_VALUE * 100;
                System.out.printf("%s : %.4f%%\n",test,percent);
            }
            double realValue = truth.func.apply(n);
            double testValue = test.func.apply(n);
            double error = realValue - testValue;
            if( Math.abs(error) > errorMargin)
            {
                System.out.printf("%d %.20f : %.20f, error is %.20f",n,realValue,testValue, error);
                return false;
            }
        }
        return true;
    }
    //String conversion
    public static double SimpleString(int x)
    {
        return Double.parseDouble("0."+x);
    }
    //Slightly mathy string conversion
    public static double StringMath(int x)
    {
        return x / (Math.pow(10,String.valueOf(x).length()));
    }
    //Math attempt
    public static double PureMath(int x)
    {
        return x / (Math.pow(10,Math.floor(Math.log10(x))+1));
    }
    //Basic algorithmic approach
    public static double WeakAlgorithm(int x)
    {
        double f = x;
        while(f >= 1.0)
        {
            f /= 10.0;
        }
        return f;
    }
    //More optimized algorithm
    public static double MediumAlgorithm(int x)
    {
        double f = x*0.1;
        while(f >= 1.0)
        {
            f *= 0.1;
        }
        return f;
    }

    //A chain of if/else if statements.
    public static double IfMadness(int x)
    {
        if ( x >= 1000000000)
        {
            return x * .0000000001;
        }
        else if ( x >= 100000000)
        {
            return x * .000000001;
        }
        else if ( x >= 10000000)
        {
            return x * .00000001;
        }
        else if ( x >= 1000000)
        {
            return x * .0000001;
        }
        else if ( x >= 100000)
        {
            return x * .000001;
        }
        else if ( x >= 10000)
        {
            return x * .00001;
        }
        else if ( x >= 1000)
        {
            return x * .0001;
        }
        else if ( x >= 100)
        {
            return x * .001;
        }
        else if ( x >= 10)
        {
            return x * .01;
        }
        else
        {
            return x*.1;
        }
    }
    public static double RecursiveDivide(int x)
    {
        return RecursiveDivide(x*0.1);
    }
    public static double RecursiveDivide(double x)
    {
        if(x < 1.0)
        {
            return x;
        }
        return RecursiveDivide(x * 0.1);
    }
}