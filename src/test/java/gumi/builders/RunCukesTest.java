package gumi.builders;


import cucumber.api.CucumberOptions;
import cucumber.api.testng.TestNGCucumberRunner;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.io.IOException;

@CucumberOptions(format = "json:target/cucumber-report.json", strict = false)
public class RunCukesTest implements IHookable {

    @Test(groups = "cucumber", description = "Runs Cucumber Features", skipFailedInvocations = true)
    public void runCukes() throws IOException {
        new TestNGCucumberRunner(getClass()).runCukes();
    }

    @Override
    public void run(IHookCallBack iHookCallBack, ITestResult iTestResult) {
        iHookCallBack.runTestMethod(iTestResult);
    }

}
