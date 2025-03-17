package org.jabolina.playground;

import static org.jabolina.playground.Infinispan.TUTORIAL_CACHE_NAME;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.commons.util.concurrent.CompletionStages;

public class RemotePutIfAbsentLoad {

   public static void main(String[] args) throws Exception {
      ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
      RemoteCacheManager cacheManager = Infinispan.connect();

      RemoteCache<String, String> cache = cacheManager.getCache(TUTORIAL_CACHE_NAME);
      Map<String, String> cm = new ConcurrentHashMap<>();

      AggregateCompletionStage<Void> ags = CompletionStages.aggregateCompletionStage();
      System.out.println("Starting test...");

      for (int i = 0; i < 100; i++) {
         for (int j = 0; j < 10; j++) {
            ags.dependsOn(CompletableFuture.supplyAsync(() -> {
               String key = key(ThreadLocalRandom.current().nextInt(10));
               String value = UUID.randomUUID().toString();
               String res = cache.withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(key, value);

               if (res == null) {
                  System.out.printf("Created %s -> %s%n", key, value);
                  cm.putIfAbsent(key, value);
               }

               return null;
            }, executor));
         }
      }

      System.out.println("Waiting for complete");
      ags.freeze().toCompletableFuture().get();

      System.out.println("Identifying failures...\n\n");
      for (int i = 0; i < 10; i++) {
         String key = key(i);
         String res = cache.withFlags(Flag.FORCE_RETURN_VALUE).get(key);
         String existing = cm.get(key);
         if (!res.equals(existing)) {
            String message = """
                           Values differ for key %s:
                           ret -> %s
                           exi -> %s
                           """.formatted(key, res, existing);
            System.out.println(message);
         }
      }

      System.out.println("Done");
      executor.shutdown();
      cacheManager.close();
   }

   private static String key(int i) {
      return "key-" + i;
   }
}
