package com.github.bluejamesbond.fluid;

import android.content.Context;

import com.squareup.otto.Bus;

public class FluidStore extends Bus {
  private Context mApplicationContext;

  public FluidStore(Context applicationContext) {
    mApplicationContext = applicationContext;
  }

  public void initialize() {
    FluidPersistence.bind(this);
    onInitialize();
  }

  public void onInitialize() {

  }

  public Context getApplicationContext() {
    return mApplicationContext;
  }
}
