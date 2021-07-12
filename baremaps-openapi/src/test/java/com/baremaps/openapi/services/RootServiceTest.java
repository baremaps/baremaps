package com.baremaps.openapi.services;

import static junit.framework.Assert.assertEquals;

import com.baremaps.model.LandingPage;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

public class RootServiceTest extends JerseyTest {

  @Override
  protected ResourceConfig configure() {
    enable(TestProperties.LOG_TRAFFIC);
    enable(TestProperties.DUMP_ENTITY);
    return new ResourceConfig(RootService.class);
  }

  @Test
  public void testRoot() {
    LandingPage landingPage = target().path("").request().get(LandingPage.class);
    assertEquals("Baremaps", landingPage.getTitle());
  }

}