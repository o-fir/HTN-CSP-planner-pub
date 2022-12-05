package uga.lig;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/")
public class FibonacciController {

    private static final StringBuilder LOG = new StringBuilder();

    @Get(uri = "/nthFibonacci/{nth}", produces = MediaType.TEXT_PLAIN)
    public String nthFibonacci(Integer nth) {
        long[] counter = new long[] { 0 };
        long start = System.currentTimeMillis();

        LOG.append("Fibonacci number #").append(nth).append(" is ");
        LOG.append(computeNthFibonacci(nth, counter));
        LOG.append(" (computed in ").append(System.currentTimeMillis() - start).append(" ms, ").append(counter[0])
                .append(" steps)\n");

        return LOG.toString();
    }

    private static long computeNthFibonacci(int nth, long[] counter) {
        counter[0]++;

        if (nth == 0 || nth == 1)
            return nth;

        return computeNthFibonacci(nth - 1, counter) + computeNthFibonacci(nth - 2, counter);
    }

}