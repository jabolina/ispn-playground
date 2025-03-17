package org.jabolina.playground;

import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;

public class EmbeddedPlaying {

   public static void main(String[] args) throws Exception {
      Scanner scanner = new Scanner(System.in);

      Properties properties = new Properties();
      properties.put("jgroups.bind.address", "127.0.0.1");

      ParserRegistry parserRegistry = new ParserRegistry(Thread.currentThread().getContextClassLoader(), false, properties);
      URL url = FileLookupFactory.newInstance().lookupFileLocation("sample.xml", Thread.currentThread().getContextClassLoader());
      ConfigurationBuilderHolder holder = parserRegistry.parse(url);

      GlobalConfiguration global = getGlobalConfiguration(holder);

      try (DefaultCacheManager dcm = new DefaultCacheManager(global)) {

         System.out.println("Process: " + ProcessHandle.current().pid());

         while (true) {
            String in = scanner.nextLine();

            if (in.startsWith("quit"))
               break;
         }
      }

      System.out.println("Bye!");
   }

   private static GlobalConfiguration getGlobalConfiguration(ConfigurationBuilderHolder holder) {
      GlobalConfigurationBuilder gcb = holder.getGlobalConfigurationBuilder();
      return gcb.build();
   }
}
