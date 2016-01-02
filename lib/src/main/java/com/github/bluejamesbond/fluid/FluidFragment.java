package com.github.bluejamesbond.fluid;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.bluejamesbond.fluid.annotations.FluidFragmentDidMount;
import com.github.bluejamesbond.fluid.annotations.FluidFragmentUpdate;
import com.github.bluejamesbond.fluid.annotations.FluidFragmentWillDismount;
import com.github.bluejamesbond.fluid.annotations.FluidLayout;
import com.orhanobut.logger.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import butterknife.ButterKnife;

public abstract class FluidFragment<T extends FluidActivity> extends Fragment implements FluidActivity.OnBackPressedListener {

  public final String TAG = getClass().getSimpleName();
  private AbstractFluidFragmentManager<T> mFragmentManager;
  private View mView;

  public FluidFragment() {
    super();

    setArguments(new Bundle());
  }

  public T getBaseActivity() {
    return ((T) getActivity());
  }

  public AbstractFluidFragmentManager<T> getFluidFragmentManager() {
    return mFragmentManager;
  }

  void setFluidFragmentManager(AbstractFluidFragmentManager<T> fragmentManager) {
    mFragmentManager = fragmentManager;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    fragmentDestruct();
  }

  private boolean invokeAnnotated(Class cls, Object match, Object... args) {
    if (FluidConfig.DEBUG) {
      Logger.d("invokeAnnotated()");
    }

    Method[] methods = getClass().getDeclaredMethods();
    boolean has = false;

    for (Method method : methods) {
      Object res = null;

      try {

        method.setAccessible(true);

        if (method.isAnnotationPresent(cls)) {
          Annotation annotation = method.getAnnotation(cls);
          if (annotation instanceof FluidFragmentWillDismount) {
            if (((FluidFragmentWillDismount) annotation).postpone()) {
              res = method.invoke(this, args[0]);
            } else {
              res = method.invoke(this);
            }
          } else if (annotation instanceof FluidFragmentUpdate) {
            if (((FluidFragmentUpdate) annotation).arguments()) {
              res = method.invoke(this, args[0]);
            } else {
              res = method.invoke(this);
            }
          } else {
            res = method.invoke(this, args);
          }
        }

      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }

      has |= match == null ? match == res : match.equals(res);
    }

    return has;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    getBaseActivity().setOnBackPressedListener(this);

    Class cls = getClass();
    FluidLayout layout = (FluidLayout) cls.getAnnotation(FluidLayout.class);

    if (layout != null) {
      mView = inflater.inflate(layout.value(), container, false);
    } else {
      mView = fragmentConstruct(inflater, container, savedInstanceState);
    }

    fragmentDidMount();

    return mView;
  }

  @Override
  public View getView() {
    return mView;
  }

  @Override
  public void onResume() {
    super.onResume();

    try {
      ButterKnife.bind(this, mView);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  View fragmentConstruct(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    if (FluidConfig.DEBUG) Logger.d("fragmentConstruct()");

    return onFragmentConstruct(getBaseActivity(), inflater, container, savedInstanceState);
  }

  void fragmentDidMount() {
    if (FluidConfig.DEBUG) Logger.d("fragmentDidMount()");

    try {
      ButterKnife.bind(this, mView);
    } catch (Exception e) {
      e.printStackTrace();
    }

    FluidPersistence.bind(this);
    onFragmentDidMount(getBaseActivity(), mView);
    invokeAnnotated(FluidFragmentDidMount.class, false);
  }

  void fragmentUpdate(Bundle bundle) {
    if (FluidConfig.DEBUG) Logger.d("fragmentUpdate()");

    onFragmentUpdate(getBaseActivity(), mView, bundle);
    invokeAnnotated(FluidFragmentUpdate.class, false, bundle);
  }

  private String name() {
    return getClass().getSimpleName();
  }

  boolean fragmentWillDismount(FluidPostpone postpone) {
    if (FluidConfig.DEBUG) Logger.d("fragmentWillDismount()");

    FluidPostpone wrapPost = () -> {
      FluidPersistence.unbind(FluidFragment.this);
      ButterKnife.unbind(FluidFragment.this);
      postpone.run();
    };

    if (!onFragmentWillDismount(getBaseActivity(), mView, wrapPost) && !invokeAnnotated(FluidFragmentDidMount.class, true)) {
      wrapPost.run();
      return false;
    }

    return true;
  }

  void fragmentDidDismount() {
    if (FluidConfig.DEBUG) Logger.d("fragmentDidDismount()");

    onFragmentDidDismount(getBaseActivity());
  }

  void fragmentDestruct() {
    if (FluidConfig.DEBUG) Logger.d("fragmentDestruct()");

    onFragmentDestruct(getBaseActivity());
  }

  boolean fragmentBackPress(FluidPostpone performDefault) {
    return onFragmentBackPress(getBaseActivity(), performDefault);
  }

  public View onFragmentConstruct(T activity, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return null;
  }

  // FIXME not always called correctly
  // invoked when the fragment will be visible
  public void onFragmentDidMount(T activity, View view) {
  }

  // fragment is reused and gets new arguments while visible
  public void onFragmentUpdate(T activity, View view, Bundle prevArgs) {
  }

  // fragment will no longer have screen time; do any asynchronous like animations here and then call post.run();
  // @return true - if you decide to invoke post.run(); false - fluid fragment manager will perform the post.run() for you
  public boolean onFragmentWillDismount(T activity, View view, FluidPostpone postpone) {
    return false;
  }

  // RESERVED
  public final void onFragmentDidDismount(T activity) {
  }

  // fragment object will be destroyed; do all your resource freeing here
  public void onFragmentDestruct(T activity) {
  }

  // back press is called; do any asynchronous actions like animations - then invoke performDefault.run()
  // however, it best to do all your animations in onFragmentWillDismount()
  public boolean onFragmentBackPress(T activity, FluidPostpone postpone) {
    return onFragmentWillDismount(activity, mView, postpone);
  }

  @Override
  public boolean onBackPressed(Activity activity, FluidPostpone postpone) {
    return fragmentBackPress(postpone);
  }
}
