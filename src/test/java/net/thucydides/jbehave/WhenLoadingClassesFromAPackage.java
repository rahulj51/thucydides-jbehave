package net.thucydides.jbehave;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class WhenLoadingClassesFromAPackage {

    @Test
    public void shouldLoadAllClassesInAPackage() throws IOException, ClassNotFoundException {
        List<Class> classes = ClassFinder.loadClasses().fromPackage("net.thucydides.jbehave.pages");
        assertThat(classes.size(), is(1));
        assertThat(classes.get(0).getName(), is("net.thucydides.jbehave.pages.StaticSitePage"));
    }

    @Test
    public void shouldLoadAllClassesInNestedPackages() throws IOException, ClassNotFoundException {
        List<Class> classes = ClassFinder.loadClasses().fromPackage("net.thucydides.jbehave");
        assertThat(classes.size(), greaterThan(10));
    }

    @Test
    public void shouldLoadNoClassesIfThePackageDoesNotExist() throws IOException, ClassNotFoundException {
        List<Class> classes = ClassFinder.loadClasses().fromPackage("that.does.not.exist");
        assertThat(classes.size(), is(0));
    }


    @Test
    public void shouldNotLoadResourcesOnClasspath() throws IOException, ClassNotFoundException {
        List<Class> classes = ClassFinder.loadClasses().fromPackage("stories");
        assertThat(classes.size(), is(0));
    }
}
