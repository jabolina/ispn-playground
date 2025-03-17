package org.jabolina.playground;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyAffinityServiceFactory;
import org.infinispan.affinity.impl.RndKeyGenerator;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.configuration.ClassAllowList;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.UnsafeConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.rocksdb.configuration.RocksDBStoreConfigurationBuilder;
import org.infinispan.remoting.transport.Address;

public class EmbeddedZeroNodeReproducer {

   public static void main(String[] args) throws Throwable {
      Scanner scanner = new Scanner(System.in);
      System.setProperty("jgroups.tcp.address", "127.0.0.1");
      System.setProperty("jgroups.bind.address", "127.0.0.1");

      DefaultCacheManager cacheManager = initCacheManager(nodeName(), "zc-reproducer", "tcp.xml");

      // Create a cache with the config
      Cache<String, String> cache = cacheManager.administration()
            .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache(Infinispan.TUTORIAL_CACHE_NAME + "-embedded", getCacheConfiguration(false));

      System.out.println("Starting =>> " + ProcessHandle.current().pid());
      System.out.println("Addr =>> " + cacheManager.getAddress());

      while (true) {
         System.out.println("Enter command: ");
         String in = scanner.nextLine();

         if (in.startsWith("quit")) break;

         String[] values = in.replaceAll("[\r\n]*", "").split(" ");

         if (in.startsWith("populate")) {
            populate(cache, Integer.parseInt(values[1]));
            System.out.printf("POPULATED -> %s\n\n", values[1]);
            continue;
         }

         if (in.startsWith("put")) {
            System.out.printf("PUT: %s -> %s\n\n", values[1], values[2]);
            cache.put(values[1], values[2]);
            continue;
         }

         if (in.startsWith("getall")) {
            System.out.printf("GET ALL -> %s\n\n", cache.entrySet());
            continue;
         }

         if (in.startsWith("get")) {
            System.out.printf("GET -> %s\n\n", cache.get(values[1]));
            continue;
         }

         if (in.startsWith("affinity")) {
            List<Address> members = new ArrayList<>();
            for (Address member : cacheManager.getMembers()) {
               if (member.equals(cacheManager.getAddress())) continue;
               members.add(member);
            }
            RndKeyGenerator gnt = new RndKeyGenerator();
            KeyAffinityService<String> kas = KeyAffinityServiceFactory
                  .newKeyAffinityService(cache, members, () -> gnt.getKey().toString(), Executors.newSingleThreadExecutor(), 100);
            kas.start();
            System.out.println("MEMBERS => " + members + "\nOption: ");
            Address mbr = members.get(Integer.parseInt(scanner.nextLine()));
            for (int i = 0; i < Integer.parseInt(values[1]); i++) {
               cache.put(kas.getKeyForAddress(mbr), String.valueOf(i));
            }
         }

      /*if (in.startsWith("lock")) {
        TransactionManager tm = cache.getAdvancedCache().getTransactionManager();
        tm.begin();
        try {
          cache.getAdvancedCache().lock(values[1]);
          System.out.printf("LOCKED -> %s\n\n", values[1]);
          System.out.println("VALUE => " + cache.get(values[1]));
          tm.commit();
          continue;
        } catch (Throwable t) {
          t.printStackTrace();
          tm.rollback();
        }
      }*/
      }

      cacheManager.close();
   }

   private static void populate(Cache<String, String> cache, int size) {
      for (int i = 0; i < size; i++) {
         cache.put(String.valueOf(i), String.valueOf(i));
      }
   }

   private static String nodeName() {
      return null; //Objects.requireNonNull(System.getProperty("node.name"));
   }

   private static DefaultCacheManager initCacheManager(String nodeName, String clusterName, String jgroupsConfig) {
      GlobalConfigurationBuilder configBuilder = new GlobalConfigurationBuilder();
      configBuilder
            .zeroCapacityNode(needsZeroCapacity(clusterName))
            .transport()
            .defaultTransport()
            .addProperty("configurationFile", jgroupsConfig)
            .addProperty("jgroups.tcp.address", "127.0.0.1")
            .addProperty("jgroups.bind.address", "127.0.0.1")
            .nodeName(nodeName)
            .clusterName(clusterName);

      return new DefaultCacheManager(configBuilder.build(), true);
   }

   private static Configuration getCacheConfiguration(boolean persistent) {
      UnsafeConfigurationBuilder config = new ConfigurationBuilder()
            .clustering().cacheMode(CacheMode.REPL_SYNC).hash()
            .clustering().remoteTimeout(30000, TimeUnit.MILLISECONDS)
            .locking().lockAcquisitionTimeout(10, TimeUnit.SECONDS)
            .clustering().stateTransfer().timeout(240, TimeUnit.SECONDS)
            .clustering().stateTransfer().chunkSize(512)
            .clustering().stateTransfer().awaitInitialTransfer(false)
            .unsafe().unreliableReturnValues(true);
      if (persistent) {
         config.clustering().hash().numSegments(20)
               .persistence()
               .addStore(RocksDBStoreConfigurationBuilder.class)
               .location("/tmp/rocksdb/data/")
               .expiredLocation("/tmp/rocksdb/expired/")
               .addProperty("database.max_log_file_size", "104857600")
               .addProperty("database.keep_log_file_num", "2")
               .preload(true);
      }
      return config.build();
   }

   private static boolean needsZeroCapacity(String clusterName) {
      return Boolean.getBoolean("zero.capacity.on");
   }
}
