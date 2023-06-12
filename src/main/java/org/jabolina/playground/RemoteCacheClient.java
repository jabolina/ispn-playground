package org.jabolina.playground;


import static org.jabolina.playground.Infinispan.TUTORIAL_CACHE_NAME;

import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.impl.operations.RetryOnFailureOperation;
import org.infinispan.client.hotrod.multimap.MultimapCacheManager;
import org.infinispan.client.hotrod.multimap.RemoteMultimapCache;
import org.infinispan.client.hotrod.multimap.RemoteMultimapCacheManagerFactory;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.configuration.StringConfiguration;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.util.CloseableIteratorSet;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;

public class RemoteCacheClient {

  public static void main(String[] args) {
    Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(RetryOnFailureOperation.class);
    root.setLevel(Level.DEBUG);
    RemoteCacheManager cacheManager = Infinispan.connect();

    try {
      RemoteCache<String, String> cache = cacheManager.getCache(TUTORIAL_CACHE_NAME);
      loop(cacheManager, cache);
    } catch (Throwable t) {
      System.out.println("CLIENT INTELLIGENCE: ");
      System.out.println(cacheManager.getChannelFactory().getClientIntelligence());
    } finally {
      System.out.println("Exiting...");
      cacheManager.stop();
    }
  }

  private static void loop(RemoteCacheManager cacheManager, RemoteCache<String, String> cache) {
    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.println("Enter command: ");
      String in = scanner.nextLine();

      if (in.startsWith("quit")) break;

      String[] values = in.replaceAll("[\r\n]*", "").split(" ");

      if (in.startsWith("put")) {
        System.out.printf("PUT: %s -> %s\n\n", values[1], values[2]);
        cache.put(values[1], values[2]);
        continue;
      }

      if (in.startsWith("getall")) {
        System.out.printf("GET ALL -> %s\n\n", cache.getAll(generateKeys(Integer.parseInt(values[1]))));
        continue;
      }

      if (in.startsWith("get")) {
        System.out.printf("GET -> %s\n\n", cache.get(values[1]));
        continue;
      }

      if (in.startsWith("size")) {
        System.out.printf("SIZE -> %d\n\n", cache.size());
        continue;
      }

      if (in.startsWith("keys")) {
        CloseableIteratorSet<String> keys = cache.keySet();
        System.out.printf("KEYS [%d] -> %s\n\n", keys.size(), keys);
        continue;
      }

      if (in.startsWith("populate")) {
        populate(cache, Integer.parseInt(values[1]));
        System.out.printf("POPULATED -> %s\n\n", values[1]);
        continue;
      }

      if (in.startsWith("multimap")) {
        String operation = values[1];
        MultimapCacheManager<String, String> remoteMultimapCacheManager = RemoteMultimapCacheManagerFactory.from(cacheManager);
        RemoteMultimapCache<String, String> rmc = remoteMultimapCacheManager.get("multimap", true);

        switch (operation) {
          case "put" -> rmc.put(values[2], values[3]).join();
          case "get" -> {
            Collection<String> contents = rmc.get(values[2]).join();
            for (String content : contents) {
              System.out.println("CONTENT: " + content);
            }
          }
        }
      }
    }
  }

  private static void populate(RemoteCache<String, String> cache, int size) {
    for (int i = 0; i < size; i++) {
      cache.put("test" + i, "testvalue" + i);
    }
  }

  private static Set<String> generateKeys(int size) {
    Set<String> keys = new HashSet<>();
    for (int i = 0; i < size; i++) {
      keys.add("test" + i);
    }
    return keys;
  }

  private static void createMultiMap(RemoteCacheManager rcm) {
    rcm.administration()
        .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
        .getOrCreateCache("multimap", new StringConfiguration(
            "<local-cache>" +
                "<encoding>" +
                "<key media-type=\"" + MediaType.TEXT_PLAIN_TYPE + "\"/>" +
                "<value media-type=\"" + MediaType.APPLICATION_OCTET_STREAM_TYPE + "\"/>" +
                "</encoding>" +
                "</local-cache>"));
  }
}
