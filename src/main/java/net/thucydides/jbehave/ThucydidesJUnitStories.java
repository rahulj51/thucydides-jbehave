package net.thucydides.jbehave;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.ThucydidesWebDriverSupport;
import net.thucydides.jbehave.runners.ThucydidesReportingRunner;
import org.codehaus.plexus.util.StringUtils;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.Format;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jbehave.core.reporters.Format.CONSOLE;
import static org.jbehave.core.reporters.Format.HTML;
import static org.jbehave.core.reporters.Format.XML;

/**
 * A JUnit-runnable test case designed to run a set of ThucydidesWebdriverIntegration-enabled JBehave stories in a given package.
 * By default, it will look for *.story files on the classpath, and steps in or underneath the current package.
 * You can redefine these constraints as follows:
 */
@RunWith(ThucydidesReportingRunner.class)
public class ThucydidesJUnitStories extends JUnitStories {

    public static final String DEFAULT_STORY_NAME =  "**/*.story";

    private net.thucydides.core.webdriver.Configuration systemConfiguration;
    private EnvironmentVariables environmentVariables;

    private String storyFolder = "";
    private String storyNamePattern = DEFAULT_STORY_NAME;

    private Configuration configuration;
    private List<Format> formats = Arrays.asList(CONSOLE, HTML, XML);

    public ThucydidesJUnitStories() {}

    protected ThucydidesJUnitStories(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables.copy();
    }

    protected ThucydidesJUnitStories(net.thucydides.core.webdriver.Configuration configuration) {
        this.setSystemConfiguration(configuration);
    }

    @Override
    public Configuration configuration() {
        if (configuration == null) {
            net.thucydides.core.webdriver.Configuration thucydidesConfiguration = getSystemConfiguration();
            if (environmentVariables != null) {
                thucydidesConfiguration = thucydidesConfiguration.withEnvironmentVariables(environmentVariables);
            }
            configuration = ThucydidesJBehave.defaultConfiguration(thucydidesConfiguration, formats, this);
        }
        return configuration;
    }

    @Override
    public InjectableStepsFactory stepsFactory() {
        return ThucydidesStepFactory.withStepsFromPackage(getRootPackage(), configuration()).andClassLoader(getClassLoader());
    }

    /**
     * The class loader used to obtain the JBehave and Step implementation classes.
     * You normally don't need to worry about this, but you may need to override it if your application
     * is doing funny business with the class loaders.
     */
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public List<String> storyPaths() {
        Set<String> storyPaths = Sets.newHashSet();

        Iterable<String> pathExpressions = getStoryPathExpressions();
        StoryFinder storyFinder = new StoryFinder();
        for(String pathExpression : pathExpressions) {
            List<URL> classpathRoots = findAllClasspathRoots();
            for(URL classpathRootUrl : classpathRoots) {
                storyPaths.addAll(storyFinder.findPaths(classpathRootUrl, pathExpression, ""));
            }
        }
        return Lists.newArrayList(storyPaths);
    }

    private ArrayList<URL> findAllClasspathRoots() {
        try {
            return Collections.list(Thread.currentThread().getContextClassLoader().getResources("."));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load the classpath roots when looking for story files",e);
        }
    }

    /**
     * The root package on the classpath containing the JBehave stories to be run.
     */
    protected String getRootPackage() {
        return this.getClass().getPackage().getName();
    }

    protected Iterable<String> getStoryPathExpressions() {
        return Splitter.on(';').trimResults().split(getStoryPath());
    }

        /**
        * The root package on the classpath containing the JBehave stories to be run.
        */
    protected String getStoryPath() {
        return (StringUtils.isEmpty(storyFolder)) ? storyNamePattern : storyFolder + "/" + storyNamePattern;
    }

    /**
     * Define the folder on the class path where the stories should be found
     */
    public void findStoriesIn(String storyFolder) {
        this.storyFolder = storyFolder;
    }

    public void useFormats(Format... formats) {
        this.formats = Arrays.asList(formats);
    }

    public void findStoriesCalled(String storyName) {
        if (storyName.startsWith("**/")) {
            storyNamePattern = storyName;
        } else {
            storyNamePattern = "**/" + storyName;
        }

    }

    /**
     * Use this to override the default ThucydidesWebdriverIntegration configuration - for testing purposes only.
     */
    public void setSystemConfiguration(net.thucydides.core.webdriver.Configuration systemConfiguration) {
        this.systemConfiguration = systemConfiguration;// copyOf(systemConfiguration);
    }

    private net.thucydides.core.webdriver.Configuration copyOf(net.thucydides.core.webdriver.Configuration systemConfiguration) {
        return systemConfiguration.copy();
    }

    public net.thucydides.core.webdriver.Configuration getSystemConfiguration() {
        if (systemConfiguration == null) {
            systemConfiguration = Injectors.getInjector().getInstance(net.thucydides.core.webdriver.Configuration.class);
        }
        return systemConfiguration;
    }

    protected void useDriver(String driver) {
        getSystemConfiguration().setIfUndefined(ThucydidesSystemProperty.DRIVER.getPropertyName(), driver);
    }

    protected void useUniqueSession() {
        getSystemConfiguration().setIfUndefined(ThucydidesSystemProperty.UNIQUE_BROWSER.getPropertyName(), "true");
    }

    public ThucydidesConfigurationBuilder runThucydides() {
        return new ThucydidesConfigurationBuilder(this);
    }

    public class ThucydidesConfigurationBuilder {
        private final ThucydidesJUnitStories thucydidesJUnitStories;

        public ThucydidesConfigurationBuilder(ThucydidesJUnitStories thucydidesJUnitStories) {
            this.thucydidesJUnitStories = thucydidesJUnitStories;
        }

        public ThucydidesConfigurationBuilder withDriver(String driver) {
            useDriver(driver);
            return this;
        }

        public ThucydidesPropertySetter withProperty(ThucydidesSystemProperty property) {
            return new ThucydidesPropertySetter(thucydidesJUnitStories, property);
        }

        public ThucydidesConfigurationBuilder inASingleSession() {
            useUniqueSession();
            return this;
        }
    }

    public class ThucydidesPropertySetter {
        private final ThucydidesJUnitStories thucydidesJUnitStories;
        private final ThucydidesSystemProperty propertyToSet;

        public ThucydidesPropertySetter(ThucydidesJUnitStories thucydidesJUnitStories,
                                        ThucydidesSystemProperty propertyToSet) {
            this.thucydidesJUnitStories = thucydidesJUnitStories;
            this.propertyToSet = propertyToSet;
        }

        public void setTo(boolean value) {
            thucydidesJUnitStories.getSystemConfiguration().setIfUndefined(propertyToSet.getPropertyName(),
                    Boolean.toString(value));
        }

        public void setTo(String value) {
            thucydidesJUnitStories.getSystemConfiguration().setIfUndefined(propertyToSet.getPropertyName(), value);
        }

        public void setTo(Integer value) {
            thucydidesJUnitStories.getSystemConfiguration().setIfUndefined(propertyToSet.getPropertyName(),
                    Integer.toString(value));
        }
    }
}
