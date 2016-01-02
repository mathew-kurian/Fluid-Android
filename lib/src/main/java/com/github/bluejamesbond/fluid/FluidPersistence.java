package com.github.bluejamesbond.fluid;

import android.content.Context;

import com.github.bluejamesbond.fluid.annotations.FluidPersistent;
import com.orhanobut.logger.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FluidPersistence {

  private static Map<Class<? extends FluidStore>, FluidStore> mStores = new HashMap<>();
  private static Map<Class<? extends FluidDispatcher>, FluidDispatcher> mDispatchers = new HashMap<>();

  public static <T extends FluidStore> T registerStore(Class<T> store, Context applicationContext) {
    if (FluidConfig.DEBUG)
      Logger.d("registerStore() -> Registering store: " + store.getSimpleName());

    T ret = null;

    try {
      ret = store.getDeclaredConstructor(Context.class).newInstance(applicationContext.getApplicationContext());
      mStores.put(store, ret);
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }

    return ret;
  }

  public static <T extends FluidStore> T getStore(Class<T> store) {
    return (T) mStores.get(store);
  }

  public static void register(Context applicationContext, Class<?>... entities) {
    List<FluidStore> storeList = new ArrayList<>();

    for (Class<?> cls : entities) {
      if (FluidDispatcher.class.isAssignableFrom(cls)) {
        registerDispatcher((Class<FluidDispatcher>) cls, applicationContext);
      } else if (FluidStore.class.isAssignableFrom(cls)) {
        storeList.add(registerStore((Class<FluidStore>) cls, applicationContext));
      }
    }

    for (FluidStore store : storeList) {
      store.initialize();
    }

    storeList.clear();
  }

  public static <T extends FluidDispatcher> T registerDispatcher(Class<T> dispatcher, Context applicationContext) {
    if (FluidConfig.DEBUG)
      Logger.d("registerDispatcher() -> Registering dispatcher: " + dispatcher.getSimpleName());

    T ret = null;
    try {
      ret = dispatcher.getDeclaredConstructor(Context.class).newInstance(applicationContext.getApplicationContext());
      mDispatchers.put(dispatcher, ret);
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }

    return ret;
  }

  public static <T extends FluidDispatcher> T getDispatcher(Class<T> dispatcher) {
    return (T) mDispatchers.get(dispatcher);
  }

  public static void bind(Object object, Class<?> obj) {
    if (FluidConfig.DEBUG) Logger.d(obj.getSimpleName() + " -> bind()");

    for (Field field : obj.getDeclaredFields()) {

      if (field.isAnnotationPresent(FluidPersistent.class)) {
        field.setAccessible(true);

        Class fieldClass = field.getType();
        Object toSet = null;

        if (FluidConfig.DEBUG)
          Logger.d(obj.getSimpleName() + " -> bind() try " + field.getType().getSimpleName());

        if (FluidDispatcher.class.isAssignableFrom(fieldClass)) {
          FluidDispatcher dispatcher = getDispatcher(fieldClass);

          try {
            dispatcher.register(object);
          } catch (Exception e) {
            // ignore
          }

          toSet = dispatcher;
        } else if (FluidStore.class.isAssignableFrom(fieldClass)) {
          FluidStore store = getStore(fieldClass);

          try {
            store.register(object);
          } catch (Exception e) {
            // ignore
          }

          toSet = store;
        }

        try {
          field.set(object, toSet);

          if (FluidConfig.DEBUG)
            Logger.d(obj.getSimpleName() + " -> bind() bound " + toSet.getClass().getSimpleName());
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void bind(Object object) {
    Class<?> cls = object.getClass();
    while (cls != null) {
      bind(object, cls);
      cls = cls.getSuperclass();
    }
  }

  public static void unbind(Object object) {
    Class<?> cls = object.getClass();
    while (cls != null) {
      unbind(object, cls);
      cls = cls.getSuperclass();
    }
  }

  public static void unbind(Object object, Class<?> obj) {

    if (FluidConfig.DEBUG) {
      Logger.d(obj.getSimpleName() + " -> unbind()");
    }

    for (Field field : obj.getDeclaredFields()) {
      if (field.isAnnotationPresent(FluidPersistent.class)) {

        field.setAccessible(true);

        Class fieldClass = field.getType();
        Object toSet = null;

        if (FluidConfig.DEBUG) {
          Logger.d(obj.getSimpleName() + " -> unbind() try " + field.getType().getSimpleName());
        }

        if (FluidDispatcher.class.isAssignableFrom(fieldClass)) {
          FluidDispatcher dispatcher = getDispatcher(fieldClass);
          dispatcher.unregister(object);

          toSet = dispatcher;
        } else if (FluidStore.class.isAssignableFrom(fieldClass)) {
          FluidStore store = getStore(fieldClass);
          store.unregister(object);

          toSet = store;
        }

        try {
          field.set(object, null);

          if (FluidConfig.DEBUG) {
            Logger.d(obj.getSimpleName() + " -> unbind() unbound " + toSet.getClass().getSimpleName());
          }
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }

        field.setAccessible(false);
      }
    }
  }
}
