package com.github.bluejamesbond.fluid;

import android.os.Bundle;

public abstract class AbstractFluidFragmentManager<T extends FluidActivity> {
  private T mActivity;
  private Integer mBaseId;
  public static final Bundle EMPTY_BUNDLE = new Bundle();
  private boolean mTransacting;
  public static final String DEFAULT_CONTEXT = "NO_CONTEXT";

  public AbstractFluidFragmentManager(T activity, int baseId) {
    mActivity = activity;
    mBaseId = baseId;
  }

  public boolean hasActiveTransaction() {
    return mTransacting;
  }

  protected void setTransacting(boolean transacting) {
    mTransacting = transacting;
  }

  public T getActivity() {
    return mActivity;
  }

  public int getBaseId() {
    return mBaseId;
  }

  public boolean toPreviousFragment() {
    return toPreviousFragment(EMPTY_BUNDLE);
  }

  public abstract boolean toPreviousFragment(Bundle args);

  public void toFragment(FluidFragment<T> fragment, String context) {
    toFragment(fragment, context, EMPTY_BUNDLE);
  }

  public abstract void toFragment(FluidFragment<T> fragment, String context, Bundle args);

}