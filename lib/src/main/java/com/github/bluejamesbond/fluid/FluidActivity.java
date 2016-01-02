package com.github.bluejamesbond.fluid;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public abstract class FluidActivity extends FragmentActivity {

  private OnBackPressedListener mOnBackPressedListener;

  @Override
  public void onBackPressed() {
    if (mOnBackPressedListener != null) {
      if (mOnBackPressedListener.onBackPressed(this, () -> performBackPressAction())) return;
    }

    performBackPressAction();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    onActivityRegisterPersistence();

    FluidPersistence.bind(this);

    onActivityCreate(savedInstanceState);
  }

  public void onActivityRegisterPersistence() {
  }

  public void onActivityCreate(Bundle savedInstanceState) {

  }

  public abstract void performBackPressAction();

  public void setOnBackPressedListener(OnBackPressedListener mOnBackPressedListener) {
    this.mOnBackPressedListener = mOnBackPressedListener;
  }

  public abstract void onFragmentChange(AbstractFluidFragmentManager fluidFragmentManager, FluidFragment from, FluidFragment to, String context, Bundle args);

  public interface OnBackPressedListener {
    boolean onBackPressed(Activity activity, FluidPostpone postpone);
  }
}