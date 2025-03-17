package org.jabolina.playground;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;

/**
 * Utility class for the simple tutorials in client server mode.
 *
 * @author Katia Aresti, karesti@redhat.com
 */
public class Infinispan {

  public static final String USER = "admin";
  public static final String PASSWORD = "password";
  public static final String HOST = "127.0.0.1";
  public static final int SINGLE_PORT = ConfigurationProperties.DEFAULT_HOTROD_PORT;

  public static final String TUTORIAL_CACHE_NAME = "playground";
  public static final String TUTORIAL_CACHE_CONFIG = """
        <?xml version="1.0"?>
        <distributed-cache name="CACHE_NAME" mode="SYNC" remote-timeout="30000" statistics="true"/>""";

  /**
   * Returns the configuration builder with the connection information
   *
   * @return a Configuration Builder with the connection config
   */
  public static ConfigurationBuilder connectionConfig() {
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.addServer()
        .host(HOST).port(SINGLE_PORT)
        .security()
        .authentication()
        //Add user credentials.
        .username(USER)
        .password(PASSWORD);

    // Docker 4 Mac Workaround. Don't use BASIC intelligence in production
    builder.clientIntelligence(ClientIntelligence.HASH_DISTRIBUTION_AWARE);

    // Make sure the remote cache is available.
    // If the cache does not exist, the cache will be created
    builder.remoteCache(TUTORIAL_CACHE_NAME)
        .configuration(TUTORIAL_CACHE_CONFIG.replace("CACHE_NAME", TUTORIAL_CACHE_NAME))
    ;
    return builder;
  }

  /**
   * Connect to the running Infinispan Server in localhost:11222.
   *
   * This method illustrates how to connect to a running Infinispan Server with a downloaded
   * distribution or a container.
   *
   * @return a connected RemoteCacheManager
   */
  public static RemoteCacheManager connect() {
    ConfigurationBuilder builder = connectionConfig();

    // Return the connected cache manager
    return new RemoteCacheManager(builder.build());
  }

}
