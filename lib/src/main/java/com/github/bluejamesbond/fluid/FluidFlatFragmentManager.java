package com.github.bluejamesbond.fluid;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FluidFlatFragmentManager<T extends FluidActivity> extends AbstractFluidFragmentManager<T> {

  private Map<String, FluidFragment<T>> mFragments = new HashMap<>();
  private List<String> mFragmentContexts = new ArrayList<>();

  public FluidFlatFragmentManager(T activity, int baseId) {
    super(activity, baseId);
  }

  @Override
  public boolean toPreviousFragment(Bundle args) {
    if (mFragmentContexts.size() > 0) {
      if (hasActiveTransaction()) {
        return true;
      }

      FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
      String lastTag = mFragmentContexts.get(mFragmentContexts.size() - 1);
      FluidFragment<T> fragment = mFragments.get(lastTag);

      FluidPostpone removeFragment = () -> {

        fragment.fragmentDestruct();
        fragment.setFluidFragmentManager(null);

        fragmentManager.beginTransaction()
                .remove(fragment)
                .commit();

        if (mFragmentContexts.size() > 1) {
          String tag = mFragmentContexts.get(mFragmentContexts.size() - 2);
          FluidFragment<T> nextFragment = mFragments.get(tag);

          if (nextFragment.isVisible()) {
            nextFragment.fragmentDidMount();
          }

          fragmentManager
                  .beginTransaction()
                  .add(getBaseId(), nextFragment, tag)
                  .addToBackStack(tag)
                  .commit();
        }

        mFragmentContexts.remove(lastTag);
        mFragments.remove(lastTag);

        setTransacting(false);
      };

      setTransacting(true);

      if (!fragment.fragmentWillDismount(removeFragment)) {
        removeFragment.run();
      }

      return true;
    }
    return false;
  }

  public void toFragment(FluidFragment<T> fragment) {
    toFragment(fragment, DEFAULT_CONTEXT, EMPTY_BUNDLE);
  }


  public void toFragment(FluidFragment<T> fragment, Bundle args) {
    toFragment(fragment, DEFAULT_CONTEXT, args);
  }

  @Override
  public void toFragment(FluidFragment<T> fragment, String context, Bundle args) {
    T activity = getActivity();
    String tag = context + fragment.getClass().getCanonicalName();
    FragmentManager fragmentManager = activity.getSupportFragmentManager();
    boolean popBack = false;
    Bundle prevBundle = new Bundle();

    if (mFragments.containsKey(tag)) {
      Bundle arguments = fragment.getArguments();
      fragment = mFragments.get(tag);
      prevBundle.putAll(fragment.getArguments());
      fragment.getArguments().putAll(arguments);
      popBack = true;
    } else {
      mFragments.put(tag, fragment);
    }

    fragment.setFluidFragmentManager(this);

    if (fragment.isVisible()) {
      fragment.fragmentUpdate(prevBundle);
      activity.onFragmentChange(this, null, fragment, context, args);
      return;
    }

    if (popBack) {
      if (FluidConfig.DEBUG) {
        Logger.d("toFragment(...) -> Restoring previous fragment");
      }

      fragmentManager
              .beginTransaction()
              .remove(fragment)
              .replace(getBaseId(), fragment, tag)
              .addToBackStack(tag)
              .commit();

      fragmentManager.popBackStack(tag, 0);
    } else {
      fragmentManager
              .beginTransaction()
              .replace(getBaseId(), fragment, tag)
              .addToBackStack(tag)
              .commit();
    }

    mFragmentContexts.remove(tag);
    mFragmentContexts.add(tag);

    activity.onFragmentChange(this, null, fragment, context, args);
  }
}

