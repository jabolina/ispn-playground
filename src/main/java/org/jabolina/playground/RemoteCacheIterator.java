package org.jabolina.playground;

import static org.jabolina.playground.Infinispan.PASSWORD;
import static org.jabolina.playground.Infinispan.USER;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.util.CloseableIteratorSet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class RemoteCacheIterator {

   private static final Logger log = (Logger) org.slf4j.LoggerFactory.getLogger(RemoteCacheIterator.class);

   public static void main(String[] args) throws Exception {
      /*((Logger) org.slf4j.LoggerFactory.getLogger("org.infinispan.client.hotrod.impl.iteration.SegmentKeyTracker")).setLevel(Level.TRACE);
      ((Logger) org.slf4j.LoggerFactory.getLogger("org.infinispan.client.hotrod.impl.consistenthash.SegmentConsistentHash")).setLevel(Level.TRACE);*/
      hotrodTest();
   }

   static void hotrodTest() throws Exception {
      int dataSize = 1 << 10;
      RemoteCacheManager rcm = getConfiguration();
      RemoteCacheManagerAdmin adm = rcm.administration();
      RemoteCache<String, String> rc = adm.getOrCreateCache("testcache", new XMLStringConfiguration(testCache("testcache")));

      log.info("##### STARTING UPLOAD #####");
      for(int i = 0; i < dataSize; i++) {
         rc.put("key-" + i, "value-" + i);
      }

      log.info("\n\n###### STARTING ITERATING #####");
      CloseableIteratorSet<Map.Entry<String, String>> iterator = rc.entrySet();

      /*System.out.println("Kill server in 10s");
      Thread.sleep(10_000);
      System.out.println("Proceeding now...");*/

      Set<String> keys = new HashSet<>();
      int count = 0;
      for (Map.Entry<String, String> entry : iterator) {
         keys.add(entry.getKey());
         count++;
         //log.info("{}: {}", entry.getKey(), entry.getValue());
      }

      log.info("Read {} keys", keys.size());
      log.info("Counted {} keys", count);
   }

   private static RemoteCacheManager getConfiguration() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.maxRetries(1);
      builder.addServer().host("127.0.0.1").port(11222);
      builder.addServer().host("127.0.0.1").port(11232);
      builder.addServer().host("127.0.0.1").port(11322);
      //Add user credentials.
      builder
            .security().authentication()
            .username(USER)
            .password(PASSWORD)
            .socketTimeout(10_000);

      return new RemoteCacheManager(builder.build());
   }

   public static String testCache(String name) {
      return "<infinispan>\n" +
            "    <cache-container>\n" +
            "        <distributed-cache owners=\"2\" mode=\"SYNC\" name=\"" + name + "\">\n" +
            "            <encoding>\n" +
            "                <key media-type=\"text/plain\"/>\n" +
            "                <value media-type=\"text/plain\"/>\n" +
            "            </encoding>\n" +
            "            <transaction mode=\"NONE\"/>\n" +
            "            <partition-handling when-split=\"ALLOW_READ_WRITES\" merge-policy=\"REMOVE_ALL\"/>\n" +
            "        </distributed-cache>" +
            "    </cache-container>\n" +
            "</infinispan>\n";
   }
}
