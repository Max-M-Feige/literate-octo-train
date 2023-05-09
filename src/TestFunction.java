import java.util.function.Function;

public class TestFunction
{
    public Function<Integer,Double> func;
    public String name;

    @Override
    public String toString()
    {
        return name;
    }

    public TestFunction(Function<Integer,Double> func, String name) {
        this.func = func;
        this.name = name;
    }
}
