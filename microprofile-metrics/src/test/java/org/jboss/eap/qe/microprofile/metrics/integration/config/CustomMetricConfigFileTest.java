package org.jboss.eap.qe.microprofile.metrics.integration.config;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * MP Config property is configured in microprofile-config.properties in META INF.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CustomMetricConfigFileTest extends CustomMetricBaseTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap.create(WebArchive.class, CustomMetricConfigFileTest.class.getSimpleName() + ".war")
                .addClasses(CustomCounterIncrementProvider.class, CustomCounterMetric.class, CustomMetricService.class,
                        CustomMetricApplication.class, CustomMetricAppInitializer.class)
                .addAsManifestResource(new StringAsset(INCREMENT_CONFIG_PROPERTY + "=" + DEFAULT_VALUE),
                        "microprofile-config.properties")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
        return webArchive;
    }

    void setConfigProperties(int increment) {
        /* invalid scenario */ }
}
