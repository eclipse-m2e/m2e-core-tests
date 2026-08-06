/*******************************************************************************
 * Copyright (c) 2020 Till Brychcy
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Till Brychcy - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.launch;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.mockito.Mockito;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.internal.launch.MavenRuntimeClasspathProvider;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MavenRuntimeClasspathProviderTest extends AbstractMavenProjectTestCase {

  @Test
  public void testAddJUnit5DependenciesAggregator() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_aggregator", //
        "junit-jupiter-5.4.2.jar", //
        "junit-jupiter-api-5.4.2.jar", //
        "apiguardian-api-1.0.0.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar", //
        "junit-jupiter-params-5.4.2.jar", //
        "junit-jupiter-engine-5.4.2.jar", //
        "junit-platform-engine-1.4.2.jar", //
        "junit-platform-launcher-1.4.2.jar" //
    );
  }

  @Test
  public void testAddJUnit5DependenciesAPIOnly() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_apionly", //
        "junit-jupiter-api-5.4.2.jar", //
        "apiguardian-api-1.0.0.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar", //
        "junit-platform-launcher-1.4.2.jar", //
        "junit-platform-engine-1.4.2.jar", //
        "junit-jupiter-engine-5.4.2.jar");
  }

  @Test
  public void testAddJUnit5DependenciesAPIOnlyDisabled() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_apionly_disabled", //
        "junit-jupiter-api-5.4.2.jar", //
        "apiguardian-api-1.0.0.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar");
  }

  @Test
  public void testAddJUnit5DependenciesWithEngine() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_withengine", //
        "junit-jupiter-engine-5.4.2.jar", "apiguardian-api-1.0.0.jar", //
        "junit-platform-engine-1.4.2.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar", //
        "junit-jupiter-api-5.4.2.jar", //
        "junit-platform-launcher-1.4.2.jar" //
    );
  }

  @Test
  public void testAddJUnit5DependenciesWithLauncher() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_withlauncher", //
        "junit-platform-launcher-1.4.2.jar", //
        "apiguardian-api-1.0.0.jar", //
        "junit-platform-engine-1.4.2.jar", //
        "junit-jupiter-api-5.4.2.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar", //
        "junit-jupiter-engine-5.4.2.jar" //
    );
  }

  @Test
  public void testClasspathScopeRuntime() throws Exception {
    runClasspathScopeTest("projects/548948_test_scope_jdt_setting",
        new String[] {"pom.xml", "project-with-launch-configs/pom.xml", "project-with-shared-runtime-code/pom.xml",
            "project-with-shared-test-code/pom.xml"},
        "project-with-launch-configs", true, "/project-with-launch-configs/target/classes",
        "/project-with-shared-runtime-code/target/classes");
  }

  @Test
  public void testClasspathScopeTest() throws Exception {
    runClasspathScopeTest("projects/548948_test_scope_jdt_setting",
        new String[] {"pom.xml", "project-with-launch-configs/pom.xml", "project-with-shared-runtime-code/pom.xml",
            "project-with-shared-test-code/pom.xml"},
        "project-with-launch-configs", false, "/project-with-launch-configs/target/test-classes",
        "/project-with-launch-configs/target/classes", "/project-with-shared-runtime-code/target/classes",
        "/project-with-shared-test-code/target/classes");
  }

  @Test
  public void testJUnit4TestWithJUnit5Dependency() throws CoreException, IOException {
    IProject project = importProject("projects/junit5TestProject/pom.xml");
    ILaunchConfiguration configuration = getLaunchConfiguration("projects/junit5TestProject",
        MavenRuntimeClasspathProvider.JDT_JUNIT_TEST);
    configuration.getAttributes().put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
        project.getName());

    IRuntimeClasspathEntry[] resolvedClasspathEntries = getResolvedMavenRuntimeClasspath(configuration);
    // make sure that vintage engine is on the classpath (as being part of the
    // m-surefire-p classpath)
    assertThat(Arrays.asList(resolvedClasspathEntries),
        Matchers.hasItem(new IRuntimeClasspathEntryMatcherByLocationSuffix("junit-vintage-engine-5.14.1.jar")));
  }

  private void runClasspathScopeTest(String baseDir, String[] pomNames, String subModuleWithLaunch,
      boolean useRuntimeScope, String... expectedBinDirectories) throws Exception {
    importProjects(baseDir, pomNames, new ResolverConfiguration());
    ILaunchConfiguration configuration = getLaunchConfiguration(baseDir + "/" + subModuleWithLaunch,
        MavenRuntimeClasspathProvider.JDT_JAVA_APPLICATION);
    configuration.getAttributes().put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, subModuleWithLaunch);
    configuration.getAttributes().put(IJavaLaunchConfigurationConstants.ATTR_EXCLUDE_TEST_CODE, useRuntimeScope);
    IRuntimeClasspathEntry[] resolvedClasspath = getResolvedMavenRuntimeClasspath(configuration);
    assertClasspathContainsDirectories(resolvedClasspath, expectedBinDirectories);
  }

  /**
   * @param configuration
   * @return
   * @throws CoreException
   */
  private IRuntimeClasspathEntry[] getResolvedMavenRuntimeClasspath(ILaunchConfiguration configuration)
      throws CoreException {
    MavenRuntimeClasspathProvider mavenRuntimeClasspathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolved = mavenRuntimeClasspathProvider.computeUnresolvedClasspath(configuration);
    return mavenRuntimeClasspathProvider.resolveClasspath(unresolved,
        configuration);
  }

  /**
   * Matcher for {@link IRuntimeClasspathEntry} that matches by filename/path suffix against its location
   */
  static final class IRuntimeClasspathEntryMatcherByLocationSuffix extends TypeSafeMatcher<IRuntimeClasspathEntry> {

    private final String locationSuffix;

    public IRuntimeClasspathEntryMatcherByLocationSuffix(String filename) {
      this.locationSuffix = filename;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("IRuntimeClasspathEntry with location ending with ").appendValue(locationSuffix);
    }

    @Override
    protected void describeMismatchSafely(IRuntimeClasspathEntry item, Description mismatchDescription) {
      mismatchDescription.appendText("was IRuntimeClasspathEntry with location ")
          .appendValue(item.getLocation());
    }

    @Override
    protected boolean matchesSafely(IRuntimeClasspathEntry item) {
      return item.getLocation() != null && item.getLocation().endsWith(locationSuffix);
    }
  }

  /**
   * @param classpathEntries array of classpath entries to check
   * @param expectedBinDirectories list of directories containing class files we expect to find
   */
  private void assertClasspathContainsDirectories(IRuntimeClasspathEntry[] classpathEntries,
      String[] expectedBinDirectories) {

    IRuntimeClasspathEntryMatcherByLocationSuffix[] binClasspathEntryMatchers = Arrays.stream(expectedBinDirectories)
        .map(folder -> new IRuntimeClasspathEntryMatcherByLocationSuffix(folder.replace('/', File.separatorChar)))
        .toArray(IRuntimeClasspathEntryMatcherByLocationSuffix[]::new);
    MatcherAssert.assertThat("Resolved classpath does not contain the binary class directories",
        Arrays.asList(classpathEntries), Matchers.hasItems(binClasspathEntryMatchers));
  }

  private void runAddJunit5DepsTest(String projectName, String... expectedJars) throws IOException, CoreException {
    importProject("projects/" + projectName + "/pom.xml");
    ILaunchConfiguration configuration = getLaunchConfiguration("projects/" + projectName,
        MavenRuntimeClasspathProvider.JDT_JUNIT_TEST);
    configuration.getAttributes().put("org.eclipse.jdt.junit.TEST_KIND", "org.eclipse.jdt.junit.loader.junit5");
    configuration.getAttributes().put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
    IRuntimeClasspathEntry[] resolvedClasspath = getResolvedMavenRuntimeClasspath(configuration);
    assertClasspathContainsJars(resolvedClasspath, expectedJars);
  }

  /**
   * @param classpathEntries
   * @param jars
   */
  private void assertClasspathContainsJars(IRuntimeClasspathEntry[] classpathEntries, String... jars) {
    IRuntimeClasspathEntryMatcherByLocationSuffix[] binClasspathEntryMatchers = Arrays.stream(jars)
        .map(folder -> new IRuntimeClasspathEntryMatcherByLocationSuffix(folder.replace('/', File.separatorChar)))
        .toArray(IRuntimeClasspathEntryMatcherByLocationSuffix[]::new);
    MatcherAssert.assertThat("Resolved classpath does not contain the binary class directories",
        Arrays.asList(classpathEntries), Matchers.hasItems(binClasspathEntryMatchers));
  }

  private ILaunchConfiguration getLaunchConfiguration(String pomDirectory, String type) throws CoreException {
    File file = new File(pomDirectory);
    String absPomDir = file.getAbsolutePath();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(MavenLaunchConstants.ATTR_POM_DIR, absPomDir);
    ILaunchConfigurationType launchConfigurationType = Mockito.mock(ILaunchConfigurationType.class);
    Mockito.when(launchConfigurationType.getAttribute("id")).thenReturn(type);
    return MavenLaunchDelegateTest.mockLaunchConfiguration(attributes, launchConfigurationType);
  }
}
