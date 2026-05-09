package io.github.indraftapp.sharefile.core.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import io.github.indraftapp.sharefile.core.model.AccessControl;
import io.github.indraftapp.sharefile.core.model.Account;
import io.github.indraftapp.sharefile.core.model.AccountUser;
import io.github.indraftapp.sharefile.core.model.AsyncOperation;
import io.github.indraftapp.sharefile.core.model.Contact;
import io.github.indraftapp.sharefile.core.model.Device;
import io.github.indraftapp.sharefile.core.model.DeviceUser;
import io.github.indraftapp.sharefile.core.model.Favorite;
import io.github.indraftapp.sharefile.core.model.File;
import io.github.indraftapp.sharefile.core.model.Folder;
import io.github.indraftapp.sharefile.core.model.Group;
import io.github.indraftapp.sharefile.core.model.Item;
import io.github.indraftapp.sharefile.core.model.Link;
import io.github.indraftapp.sharefile.core.model.Metadata;
import io.github.indraftapp.sharefile.core.model.Note;
import io.github.indraftapp.sharefile.core.model.ODataEntity;
import io.github.indraftapp.sharefile.core.model.Session;
import io.github.indraftapp.sharefile.core.model.Share;
import io.github.indraftapp.sharefile.core.model.ShareAlias;
import io.github.indraftapp.sharefile.core.model.SymbolicLink;
import io.github.indraftapp.sharefile.core.model.User;
import io.github.indraftapp.sharefile.core.model.WebhookSubscription;
import io.github.indraftapp.sharefile.core.model.Zone;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves ShareFile {@code odata.type} strings to Java model classes.
 *
 * <p>This class is public because {@code @JsonTypeIdResolver} annotations on model classes
 * reference it across packages. It is <strong>not</strong> part of the public SDK API and should
 * not be used directly by SDK consumers.
 */
public final class ODataTypeResolver extends TypeIdResolverBase {

  private static final Map<String, Class<? extends ODataEntity>> TYPES_BY_ID =
      new LinkedHashMap<>();
  private static final Map<Class<?>, String> IDS_BY_TYPE = new LinkedHashMap<>();
  private JavaType baseType;

  static {
    register(Item.class, "ShareFile.Api.Models.Item");
    register(File.class, "ShareFile.Api.Models.File");
    register(Folder.class, "ShareFile.Api.Models.Folder");
    register(Note.class, "ShareFile.Api.Models.Note");
    register(Link.class, "ShareFile.Api.Models.Link");
    register(SymbolicLink.class, "ShareFile.Api.Models.SymbolicLink");
    register(User.class, "ShareFile.Api.Models.User");
    register(AccountUser.class, "ShareFile.Api.Models.AccountUser");
    register(AsyncOperation.class, "ShareFile.Api.Models.AsyncOperation");
    register(Group.class, "ShareFile.Api.Models.Group");
    register(Share.class, "ShareFile.Api.Models.Share");
    register(AccessControl.class, "ShareFile.Api.Models.AccessControl");
    register(Account.class, "ShareFile.Api.Models.Account");
    register(Contact.class, "ShareFile.Api.Models.Contact");
    register(Device.class, "ShareFile.Api.Models.Device");
    register(DeviceUser.class, "ShareFile.Api.Models.DeviceUser");
    register(Favorite.class, "ShareFile.Api.Models.Favorite");
    register(Metadata.class, "ShareFile.Api.Models.Metadata");
    register(Session.class, "ShareFile.Api.Models.Session");
    register(ShareAlias.class, "ShareFile.Api.Models.ShareAlias");
    register(WebhookSubscription.class, "ShareFile.Api.Models.WebhookSubscription");
    register(Zone.class, "ShareFile.Api.Models.Zone");
  }

  public static Optional<Class<? extends ODataEntity>> resolveEntityType(String typeId) {
    return Optional.ofNullable(TYPES_BY_ID.get(normalize(typeId)));
  }

  private static void register(Class<? extends ODataEntity> type, String externalId) {
    TYPES_BY_ID.put(normalize(externalId), type);
    TYPES_BY_ID.put(type.getSimpleName(), type);
    IDS_BY_TYPE.put(type, externalId);
  }

  private static String normalize(String typeId) {
    if (typeId == null) {
      return "";
    }
    String normalized = typeId.trim();
    return normalized.startsWith("#") ? normalized.substring(1) : normalized;
  }

  @Override
  public void init(JavaType baseType) {
    this.baseType = baseType;
    super.init(baseType);
  }

  @Override
  public String idFromValue(Object value) {
    return idFromValueAndType(value, value.getClass());
  }

  @Override
  public String idFromValueAndType(Object value, Class<?> suggestedType) {
    return IDS_BY_TYPE.getOrDefault(suggestedType, suggestedType.getSimpleName());
  }

  @Override
  public JavaType typeFromId(DatabindContext context, String id) {
    JavaType currentBaseType = baseType;
    Class<?> rawBaseType = currentBaseType.getRawClass();
    Optional<Class<? extends ODataEntity>> resolvedType = resolveEntityType(id);

    if (resolvedType.isEmpty()) {
      if (rawBaseType.isInterface()
          || java.lang.reflect.Modifier.isAbstract(rawBaseType.getModifiers())) {
        throw new IllegalArgumentException("Unknown ShareFile odata.type: " + id);
      }
      return context.constructSpecializedType(currentBaseType, rawBaseType);
    }

    Class<? extends ODataEntity> rawResolvedType = resolvedType.get();
    if (!rawBaseType.isAssignableFrom(rawResolvedType)) {
      throw new IllegalArgumentException(
          "ShareFile odata.type %s is not assignable to %s".formatted(id, rawBaseType.getName()));
    }

    return context.constructSpecializedType(currentBaseType, rawResolvedType);
  }

  @Override
  public JsonTypeInfo.Id getMechanism() {
    return JsonTypeInfo.Id.CUSTOM;
  }
}
