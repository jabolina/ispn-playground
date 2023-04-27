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
  public static final int SINGLE_PORT = ConfigurationProperties.DEFAULT_HOTROD_PORT + 00;

  public static final String TUTORIAL_CACHE_NAME = "manual";
  public static final String TUTORIAL_CACHE_CONFIG =
      "<?xml version=\"1.0\"?>\n" +
      "<distributed-cache name=\"eviction\" statistics=\"true\" mode=\"SYNC\">\n" +
      " <encoding>\n" +
      "   <key media-type=\"text/plain\"/>\n" +
      "   <value media-type=\"text/plain\"/>\n" +
      " </encoding>\n" +
      " <partition-handling when-split=\"DENY_READ_WRITES\"\n" +
      "                     merge-policy=\"PREFERRED_ALWAYS\"/>\n" +
      " <locking isolation=\"READ_COMMITTED\"/>\n" +
      " <transaction\n" +
      "   locking=\"PESSIMISTIC\"\n" +
      "   auto-commit=\"true\"\n" +
      "   complete-timeout=\"60000\"\n" +
      "   mode=\"NON_XA\"\n" +
      "   notifications=\"true\"\n" +
      "   reaper-interval=\"30000\"\n" +
      "   recovery-cache=\"__recoveryInfoCacheName__\"\n" +
      "   stop-timeout=\"30000\"\n" +
      "   transaction-manager-lookup=\"org.infinispan.transaction.lookup.GenericTransactionManagerLookup\"/>" +
      " <memory max-count=\"1000\"/>\n" +
      " <persistence passivation=\"false\">\n" +
      "   <file-store purge=\"true\" read-only=\"false\" preload=\"false\"/>\n" +
      " </persistence>\n" +
      "</distributed-cache>";

  /**
   * Returns the configuration builder with the connection information
   *
   * @return a Configuration Builder with the connection config
   */
  public static ConfigurationBuilder connectionConfig() {
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.addServer().host(HOST).port(SINGLE_PORT).security()
        .authentication()
        //Add user credentials.
        .username(USER)
        .password(PASSWORD)
            .maxRetries(0)
                .socketTimeout(300_000);

    // Docker 4 Mac Workaround. Don't use BASIC intelligence in production
    builder.clientIntelligence(ClientIntelligence.BASIC);

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
