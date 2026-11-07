
package org.eclipse.m2e.tests.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;
import org.osgi.service.component.annotations.Reference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.jdt.internal.JavaProjectConfigurator;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.RequireMavenExecutionContext;


@RequireMavenExecutionContext
public class MavenProjectFacadeTest extends AbstractMavenProjectTestCase {

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID)
        .remove(MavenPreferenceConstants.P_USE_PROPERTIES_FROM_BUILD);
  }

  @Test
  public void testGetMojoExecution() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);

    MojoExecutionKey compileKey = null;
    for(MojoExecutionKey executionKey : facade.getMojoExecutionMapping().keySet()) {
      if("maven-compiler-plugin".equals(executionKey.artifactId()) && "compile".equals(executionKey.goal())) {
        compileKey = executionKey;
        break;
      }
    }

    assertNotNull(compileKey);
    MojoExecution compileMojo = facade.getMojoExecution(compileKey, monitor);

    assertEquals("1.5", facade.getMojoParameterValue(compileMojo, "source", String.class, monitor));
    assertEquals("1.6", facade.getMojoParameterValue(compileMojo, "target", String.class, monitor));
  }

  @Test
  public void testGetMojoExecutionAfterWorkspaceRestart() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    deserializeFromWorkspaceState(facade);

    MojoExecutionKey compileKey = null;
    for(MojoExecutionKey executionKey : facade.getMojoExecutionMapping().keySet()) {
      if("maven-compiler-plugin".equals(executionKey.artifactId()) && "compile".equals(executionKey.goal())) {
        compileKey = executionKey;
        break;
      }
    }

    assertNotNull(compileKey);
    MojoExecution compileMojo = facade.getMojoExecution(compileKey, monitor);

    assertEquals("1.5", facade.getMojoParameterValue(compileMojo, "source", String.class, monitor));
    assertEquals("1.6", facade.getMojoParameterValue(compileMojo, "target", String.class, monitor));
  }

  @Test
  public void testGetMojoExecutions() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);

    List<MojoExecution> executions = facade.getMojoExecutions("org.apache.maven.plugins", "maven-compiler-plugin",
        monitor, "compile");
    assertEquals(executions.toString(), 1, executions.size());

    assertEquals("1.5", facade.getMojoParameterValue(executions.get(0), "source", String.class, monitor));
    assertEquals("1.6", facade.getMojoParameterValue(executions.get(0), "target", String.class, monitor));
  }

  @RequireMavenExecutionContext(require = false)
  @Test
  public void testGetMojoExecutionsWithoutMavenExecutionContext() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);

    List<MojoExecution> executions = facade.getMojoExecutions("org.apache.maven.plugins", "maven-compiler-plugin",
        monitor, "compile");
    assertEquals(executions.toString(), 1, executions.size());

    // in backward compat mode MavenProject is not disposed
    assertSame(facade.getMavenProject(monitor), facade.getMavenProject(monitor));
  }

  @Test
  public void testGetMojoExecutionsAfterWorkspaceRestart() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    deserializeFromWorkspaceState(facade);

    List<MojoExecution> executions = facade.getMojoExecutions("org.apache.maven.plugins", "maven-compiler-plugin",
        monitor, "compile");
    assertEquals(executions.toString(), 1, executions.size());

    assertEquals("1.5", facade.getMojoParameterValue(executions.get(0), "source", String.class, monitor));
    assertEquals("1.6", facade.getMojoParameterValue(executions.get(0), "target", String.class, monitor));
  }

  @Test
  public void testGetProjectConfigurators() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertNotNull("Expected not null MavenProjectFacade", facade);
    ILifecycleMapping lifecycleMapping = MavenPlugin.getProjectConfigurationManager().getLifecycleMapping(facade);
    assertNotNull("Expected not null LifecycleMapping", lifecycleMapping);

    List<AbstractProjectConfigurator> projectConfigurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertNotNull("Expected not null projectConfigurators", projectConfigurators);
    assertHasJavaConfiguratior(projectConfigurators);
  }

  /**
   * @param projectConfigurators
   */
  private void assertHasJavaConfiguratior(List<AbstractProjectConfigurator> projectConfigurators) {
    for(AbstractProjectConfigurator configurator : projectConfigurators) {
      if(configurator instanceof JavaProjectConfigurator) {
        return;
      }
    }
    fail("Java Project configurator not found!");

  }

  @Test
  public void testGetProjectConfiguratorsAfterWorkspaceRestart() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertNotNull("Expected not null MavenProjectFacade", facade);
    deserializeFromWorkspaceState(facade);

    ILifecycleMapping lifecycleMapping = MavenPlugin.getProjectConfigurationManager().getLifecycleMapping(facade);
    assertNotNull("Expected not null LifecycleMapping", lifecycleMapping);

    List<AbstractProjectConfigurator> projectConfigurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertNotNull("Expected not null projectConfigurators", projectConfigurators);
    assertHasJavaConfiguratior(projectConfigurators);
  }

  @Test
  public void testIsStale() throws Exception {
    IProject project = importProject("projects/testIsStale/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(project);

    testIsStale(project, "pom.xml");
    for(IPath filename : ProjectRegistryManager.METADATA_PATH) {
      MavenImpl.execute(MavenPlugin.getMaven(), true /*offline*/, false /*updateSnapshots*/, (c, m) -> {
        ProjectRegistryManager projectManager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();
        projectManager.refresh(getPomFiles(project), monitor);
        return null;
      }, monitor);

      testIsStale(project, filename.toString());
    }
  }

  @Test
  public void test436668_EclipseProjectMetadataNotStale() throws Exception {
    IProject project = importProject("projects/testIsStale/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(project);

    testIsNotStale(project, ".classpath");
    testIsNotStale(project, ".project");
  }

  private void testIsStale(IProject project, String filename) throws Exception {
    testStale(project, filename, true);
  }

  private void testIsNotStale(IProject project, String filename) throws Exception {
    testStale(project, filename, false);
  }

  private void testStale(IProject project, String filename, boolean staleIfFileModified) throws Exception {
    IMavenProjectFacade projectFacade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertFalse("Expected not stale MavenProjectFacade before changing the " + filename + " file",
        projectFacade.isStale());

    project.getFile(filename).touch(monitor);
    if(staleIfFileModified) {
      assertTrue("Expected stale MavenProjectFacade after changing the " + filename + " file", projectFacade.isStale());
    } else {
      assertFalse("Expected not stale MavenProjectFacade after changing the " + filename + " file",
          projectFacade.isStale());
    }
  }

  @Reference
  private ProjectRegistryManager manager;

  @Test
  public void testPersistPropertiesFromBuild() throws Exception {
    IProject project = importProject("projects/simple-pom/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertTrue(facade instanceof MavenProjectFacade);

    Properties props = new Properties();
    props.setProperty("somekey", "somevalue");

    ((MavenProjectFacade) facade).setPropertiesFromBuild(props);

    MavenProjectFacade newFacade = new MavenProjectFacade(
        manager, facade.getPom(), facade.getMavenProject(), facade.getConfiguration()
    );
    Properties newProps = newFacade.getPropertiesFromBuild();
    assertNotSame(props, newProps);
    assertEquals("somevalue", newProps.getProperty("somekey"));
  }

  @Test
  public void testPropertiesFromBuildResolvedForMojoExecution() throws Exception {
    InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID)
        .putBoolean(MavenPreferenceConstants.P_USE_PROPERTIES_FROM_BUILD, true);
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertTrue(facade instanceof MavenProjectFacade);

    Properties props = new Properties();
    props.setProperty("somekey", "somevalue");

    ((MavenProjectFacade) facade).setPropertiesFromBuild(props);

    List<MojoExecution> executions = facade
        .getMojoExecutions("org.apache.maven.plugins", "maven-surefire-plugin", monitor, "test");
    assertEquals(1, executions.size());
    assertEquals("somevalue", facade.getMojoParameterValue(executions.get(0), "argLine", String.class, monitor));
  }

  @Test
  public void testPropertiesFromBuildUnresolvedForMojoExecutionWithoutPropertiesFromBuild() throws Exception {
    InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID)
        .putBoolean(MavenPreferenceConstants.P_USE_PROPERTIES_FROM_BUILD, false);
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertTrue(facade instanceof MavenProjectFacade);

    Properties props = new Properties();
    props.setProperty("somekey", "somevalue");

    ((MavenProjectFacade) facade).setPropertiesFromBuild(props);

    List<MojoExecution> executions = facade
        .getMojoExecutions("org.apache.maven.plugins", "maven-surefire-plugin", monitor, "test");
    assertEquals(1, executions.size());
    assertEquals(null, facade.getMojoParameterValue(executions.get(0), "argLine", String.class, monitor));
  }

  @Test
  public void testImportWithDynamicProperty() throws Exception {
    InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID)
        .putBoolean(MavenPreferenceConstants.P_USE_PROPERTIES_FROM_BUILD, true);
    IProject project = importProject("projects/dynamic-property/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(project);

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertTrue(facade instanceof MavenProjectFacade);

    Properties properties = ((MavenProjectFacade) facade).getPropertiesFromBuild();
    assertNotNull(properties);
    String value = properties.getProperty("org.junit.jupiter:junit-jupiter-api:jar");
    assertNotNull(value);
    assertTrue(value, value.endsWith("junit-jupiter-api-6.1.3.jar"));
  }
}
