package uga.lig;

import java.io.IOException;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import uga.lig.csp_clean_wStaticFluents.CoreMain;

@Controller("/")
public class OptiPlanController {

    private static final StringBuilder LOG = new StringBuilder();

    @Get(uri = "/o", produces = MediaType.TEXT_PLAIN)
    public String optiPlan() {
        long[] counter = new long[] { 0 };
        long start = System.currentTimeMillis();

        try {
            String[] args = { "1", "2" };
            CoreMain.main(args);
        } catch (IOException e) {
            LOG.append("EXCEPTION " + e);

            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        LOG.append("Done");

        return LOG.toString();
    }

}