package com.iobeam.api.resource;

import com.iobeam.api.auth.ProjectBearerAuthToken;
import com.iobeam.api.auth.UserBearerAuthToken;
import com.iobeam.api.client.RestError;
import com.iobeam.api.resource.annotations.JsonIgnore;
import com.iobeam.api.resource.annotations.JsonProperty;
import com.iobeam.api.resource.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps resources to JSON and vice versa.
 */
public class ResourceMapper {

    @SuppressWarnings("unchecked")
    public <T> T fromJson(final JSONObject json,
                          final Class<T> resourceClass) throws ResourceException {

        try {
            if (resourceClass.equals(Device.class)) {
                return (T) Device.fromJson(json);
            } else if (resourceClass.equals(Device.Id.class)) {
                return (T) Device.Id.fromJson(json);
            } else if (resourceClass.equals(DeviceList.class)) {
                return (T) DeviceList.fromJson(json);
            } else if (resourceClass.equals(ProjectBearerAuthToken.class)) {
                return (T) ProjectBearerAuthToken.fromJson(json);
            } else if (resourceClass.equals(UserBearerAuthToken.class)) {
                return (T) UserBearerAuthToken.fromJson(json);
            } else if (resourceClass.equals(RestError.class)) {
                // We only support one error so far.
                final JSONArray arr = json.getJSONArray("errors");
                final JSONObject err = arr.getJSONObject(0);
                return (T) RestError.fromJson(err);
            } else if (resourceClass.equals(Void.class)) {
                return null;
            }
        } catch (JSONException e) {
            // Fall through and throw ResourceException
        } catch (ParseException e) {
            // Fall through and throw ResourceException
        }

        throw new ResourceException("JSON resource mapping failure for class "
                                    + resourceClass.getName()
                                    + "\n\nJSON:\n" + json);
    }

    // Recursively serialize bean. Code partly based on wrap() function
    // org.json.JSONObject
    private Object beanSerialize(final Object resource,
                                 final Map<String, Object> out) {
        final Class clazz = resource.getClass();
        final Method[] methods = clazz.getDeclaredMethods();
        final JSONObject json = new JSONObject();

        if (clazz.equals(Import.class)) {
            ((Import) resource).serialize(out);
            return json;
        } else if (clazz.equals(ImportBatch.class)) {
            ((ImportBatch) resource).serialize(out);
            return json;
        }

        for (final Method m : methods) {

            try {
                if (Modifier.isPublic(m.getModifiers())) {
                    final String name = m.getName();
                    if (name.equals("getClass") || name.equals("getDeclaringClass")) {
                        continue;
                    }

                    final JsonProperty jsonProp = m.getAnnotation(JsonProperty.class);
                    if (m.getAnnotation(JsonIgnore.class) != null) {
                        continue;
                    }

                    boolean isGetter = name.length() > 3 && name.startsWith("get")
                                       && Character.isUpperCase(name.charAt(3));
                    boolean isBoolGetter = name.length() > 2 && name.startsWith("is")
                                           && Character.isUpperCase(name.charAt(2));

                    final String key;
                    if (jsonProp != null) {
                        key = jsonProp.value();
                    } else if (isGetter) {
                        key = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    } else if (isBoolGetter) {
                        key = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                    } else {
                        continue;  // on to the next method
                    }

                    if (key != null) {
                        final Object object = m.invoke(resource, (Object[]) null);
                        Object result;

                        if (object != null) {
                            try {
                                if (object instanceof JSONObject
                                    || object instanceof JSONArray
                                    || object instanceof Byte
                                    || object instanceof Character
                                    || object instanceof Short
                                    || object instanceof Integer
                                    || object instanceof Long
                                    || object instanceof Boolean
                                    || object instanceof Float
                                    || object instanceof Double
                                    || object instanceof String) {
                                    result = object;
                                } else if (object instanceof Collection) {
                                    result = new JSONArray((Collection) object);
                                } else if (object.getClass().isArray()) {
                                    result = new JSONArray(Arrays.asList(object));
                                } else if (object instanceof Map) {
                                    result = new JSONObject((Map) object);
                                } else if (object instanceof Date) {
                                    result = Util.DATE_FORMAT.format((Date) object);
                                } else {
                                    Package objectPackage = object.getClass().getPackage();
                                    String objectPackageName =
                                        objectPackage != null ? objectPackage.getName() : "";
                                    if (objectPackageName.startsWith("java.")
                                        || objectPackageName.startsWith("javax.")
                                        || object.getClass().getClassLoader() == null) {
                                        result = object.toString();
                                    } else {
                                        result = beanSerialize(object, out);
                                    }
                                }
                                if (result != null) {
                                    out.put(key, result);
                                }
                            } catch (Exception exception) {
                                return null;
                            }
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return json;
    }

    /*
     * The age old json.org library in Android doesn't support
     * mapping bean-style objects directly to JSON, so we have
     * to call this from a more recent version of JSONObject that
     * we include in our jar.
     */
    public byte[] toJsonBytes(final Object resource) throws UnsupportedEncodingException {
        if (resource instanceof JSONObject) {
            final JSONObject json = (JSONObject) resource;
            return json.toString().getBytes("UTF-8");
        }

        final HashMap<String, Object> out = new HashMap<String, Object>();
        beanSerialize(resource, out);
        return new JSONObject(out).toString().getBytes("UTF-8");
    }
}
