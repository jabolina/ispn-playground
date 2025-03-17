package org.jabolina.playground;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.DefaultConnectionFactory;

public class MemcachedClient {

   public static void main(String[] args) throws Exception {
      /*ConnectionFactory factory = new ConnectionFactoryBuilder()
            .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
            .build();*/
      ConnectionFactory factory = new DefaultConnectionFactory();
      net.spy.memcached.MemcachedClient mc = new net.spy.memcached.MemcachedClient(factory, List.of(new InetSocketAddress("127.0.0.1", 11211)));

      mc.set("key", 0, UUID.randomUUID().toString()).get(10, TimeUnit.SECONDS);
      System.out.println("VALUE: " + mc.get("key"));
      mc.shutdown();
   }
}
