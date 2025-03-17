package org.jabolina.playground;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyAffinityServiceFactory;
import org.infinispan.affinity.impl.RndKeyGenerator;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.IsolationLevel;


public class EmbeddedCache {

  public static void main(String[] args) throws Exception {
    Scanner scanner = new Scanner(System.in);
    GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
    global.transport().defaultTransport();

    DefaultCacheManager cacheManager = new DefaultCacheManager(global.build());

    // Create a transaction cache config
    ConfigurationBuilder builder = new ConfigurationBuilder();
    Configuration cacheConfig = builder
        .clustering()
          .cacheMode(CacheMode.DIST_SYNC)
          .hash().numOwners(2)
        .locking()
          .isolationLevel(IsolationLevel.READ_COMMITTED)
          .useLockStriping(false)
        .transaction()
          .transactionMode(TransactionMode.NON_TRANSACTIONAL)
          .lockingMode(LockingMode.OPTIMISTIC)
        .build();
    // Create a cache with the config
    Cache<String, String> cache = cacheManager.administration()
        .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
        .getOrCreateCache(Infinispan.TUTORIAL_CACHE_NAME + "-embedded", cacheConfig);

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
}
