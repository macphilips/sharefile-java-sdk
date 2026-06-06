package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ResourceRequestExecutorTest {

  @Test
  void entityUriPreservesSpecialIdsAsLiteralKeys() {
    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(new ClientTestSupport.TestTransport())) {
      ResourceRequestExecutor executor = context.executor();

      assertEquals("/Items(home)", executor.entityUri("home").toString());
      assertEquals("/Items(favorites)", executor.entityUri("favorites").toString());
      assertEquals("/Items(allshared)", executor.entityUri("allshared").toString());
      assertEquals("/Items(connectors)", executor.entityUri("connectors").toString());
      assertEquals("/Items(box)", executor.entityUri("box").toString());
      assertEquals("/Items(top)", executor.entityUri("top").toString());
    }
  }

  @Test
  void compositeKeyUriUsesODataCompositeKeyFormat() {
    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(new ClientTestSupport.TestTransport())) {
      ResourceRequestExecutor executor = context.executor();

      assertEquals(
          "/Items(principalid=user-1,itemid=item-2)",
          executor.compositeKeyUri("principalid=user-1", "itemid=item-2").toString());
    }
  }

  @Test
  void compositeKeyUriMapUsesODataCompositeKeyFormat() {
    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(new ClientTestSupport.TestTransport())) {
      ResourceRequestExecutor executor = context.executor();
      HashMap<String, String> keys = new HashMap<>();
      keys.put("itemid", "item-2");
      keys.put("principalid", "user-1");
      assertEquals(
          "/Items(itemid=item-2,principalid=user-1)", executor.compositeKeyUri(keys).toString());
    }
  }
}
