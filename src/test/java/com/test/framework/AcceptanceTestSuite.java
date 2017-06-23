package com.test.framework;

import com.epam.reportportal.jbehave.ReportPortalFormat;
import com.epam.reportportal.jbehave.ReportPortalViewGenerator;
import com.test.framework.utils.OsCheck;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import net.serenitybdd.jbehave.SerenityStories;
import org.jbehave.core.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcceptanceTestSuite extends OptimizedParallelAcceptanceTestSuite {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptanceTestSuite.class.getSimpleName());
    private static final String X64_ARCH = "amd64";

    public AcceptanceTestSuite() {
        setDriverAccordingToOS();
        new SerenityStories().getSystemConfiguration().getEnvironmentVariables().setProperty("webdriver.chrome.driver", System.getProperty("webdriver.chrome.driver"));
        selectStoryFilesForRunningSuite();
    }

    public Configuration configuration() {
        final Configuration configuration = super.configuration();
        return configuration
                .useStoryReporterBuilder(configuration.storyReporterBuilder().withFormats(ReportPortalFormat.INSTANCE))
                .useViewGenerator(new ReportPortalViewGenerator());
    }

    private void setDriverAccordingToOS() {
        OsCheck.OSType ostype = OsCheck.getOperatingSystemType();
        switch (ostype) {
            case Windows:
                setChromeDriverWindows();
                break;
            case MacOS:
                setChromeDriverOsx();
                break;
            case Linux:
                if (X64_ARCH.equals(System.getProperty("os.arch"))) {
                    setChromeDriverLinux64();
                } else {
                    setChromeDriverLinux32();
                }
                break;
            case Other:
                LOG.error("Can't define OS");
                break;
        }
    }

    private void setChromeDriverLinux32() {
        ChromeDriverManager.getInstance().arch32().setup();
    }

    private void setChromeDriverLinux64() {
        ChromeDriverManager.getInstance().arch64().setup();
    }

    private void setChromeDriverWindows() {
        ChromeDriverManager.getInstance().setup();
    }

    private void setChromeDriverOsx() {
        ChromeDriverManager.getInstance().setup();
    }
}
