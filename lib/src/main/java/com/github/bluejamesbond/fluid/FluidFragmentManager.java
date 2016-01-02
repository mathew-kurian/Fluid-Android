package com.github.bluejamesbond.fluid;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Tree<T> {
  public Node<T> root;

  public Tree() {
    root = new Node<T>();
    root.children = new ArrayList<Node<T>>();
  }

  public static class Node<T> {
    public T data;
    public Node<T> parent;
    public List<Node<T>> children;
  }
}

public class FluidFragmentManager<T extends FluidActivity> extends AbstractFluidFragmentManager<T> {

  public static final String TAG = "FluidFragmentManager";
  private Tree<BFMNode> mFragmentTree = new Tree<>();
  private Map<String, Tree.Node<BFMNode>> mFragmentContexts = new HashMap<>();
  private boolean mCanExitActivity;
  private Tree.Node<BFMNode> mLastNode = mFragmentTree.root;
  private FluidFragment<T> mLastFragment;

  public FluidFragmentManager(T activity, int baseId, boolean canExitActivity) {
    super(activity, baseId);
    mCanExitActivity = canExitActivity;
  }

  private void removeChildrenFragment(FragmentTransaction transaction, Tree.Node<BFMNode> node) {
    T activity = getActivity();

    if (FluidConfig.DEBUG) {
      Logger.d("removeChildrenFragment(...) -> " + node.children.size());
    }

    for (Tree.Node<BFMNode> child : node.children) {
      if (FluidConfig.DEBUG) {
        Logger.d("removeChildrenFragment(...) -> Will destroy - " + child.data.mFluidFragments.size());
      }

      for (FluidFragment<T> frag : child.data.mFluidFragments) {
        if (FluidConfig.DEBUG) {
          Logger.d("removeChildrenFragment(...) -> Destroy Fragment#" + frag.getClass().getSimpleName());
        }

        frag.fragmentDestruct();
        frag.setFluidFragmentManager(null);
        transaction.remove(frag);
        removeChildrenFragment(transaction, child);
      }

      if (mFragmentContexts.containsKey(child.data.mContext)) {
        mFragmentContexts.remove(child.data.mContext);
      }

      child.data.mFluidFragments.clear();
      child.parent = null;
    }

    node.children.clear();
  }

  private FluidFragment<T> findInsertFragmentByClass(List<FluidFragment<T>> list, FluidFragment<T> fragment) {
    for (FluidFragment<T> f : list) {
      if (fragment.getClass().equals(f.getClass())) {
        return f;
      }
    }

    list.add(fragment);

    return fragment;
  }

  public boolean toPreviousFragment(Bundle args) {
    final T activity = getActivity();
    if (mLastNode.parent != null) {

      if (FluidConfig.DEBUG) {
        Logger.d("performBackPressAction() -> Reverting to Context " + mLastNode.parent.data.mContext);
      }

      final Tree.Node<BFMNode> parentNode = mLastNode.parent;

      FluidPostpone removeAction = () -> {

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        removeChildrenFragment(transaction, parentNode);
        transaction.commit();

        FluidFragment<T> parentFragment = parentNode.data.mLastActiveFragment;
        String tag = parentNode.data.mContext + parentFragment.getClass().getCanonicalName();

        fragmentManager
                .beginTransaction()
                .replace(getBaseId(), parentFragment, tag)
                .addToBackStack(tag)
                .commit();

        mLastNode = parentNode;

        setTransacting(false);
      };

      setTransacting(true);
      if (!mLastFragment.fragmentWillDismount(removeAction)) {
        removeAction.run();
      }

      return true;
    } else if (mCanExitActivity) {
      FluidPostpone finishAction = () -> {
        activity.finish();
        setTransacting(false);

      };

      setTransacting(true);

      if (mLastFragment != null && !mLastFragment.fragmentWillDismount(finishAction)) {
        finishAction.run();
      } else {
        finishAction.run();
      }

      return true;
    }

    return false;
  }

  @Override
  public void toFragment(FluidFragment<T> fragment, final String context, Bundle args) {
    final T activity = getActivity();
    Tree.Node<BFMNode> node;
    FluidFragment<T> toMount;
    final FragmentManager fragmentManager = activity.getSupportFragmentManager();
    final String tag = context + fragment.getClass().getCanonicalName();
    boolean removeChildren = false;
    Bundle prevBundle = new Bundle();

    // if context exists
    if (mFragmentContexts.containsKey(context)) {
      node = mFragmentContexts.get(context);

      removeChildren = true;

      toMount = findInsertFragmentByClass(node.data.mFluidFragments, fragment);
      prevBundle.putAll(toMount.getArguments());
      toMount.getArguments().putAll(fragment.getArguments());

      mLastNode = node;
    } else {
      Tree.Node<BFMNode> next;

      if (mFragmentContexts.size() == 0) {
        next = mLastNode;
        next.data = new BFMNode();
      } else {
        next = new Tree.Node<>();
        next.parent = mLastNode;
        next.data = new BFMNode();
        next.children = new ArrayList<>();

        mLastNode.children.add(next);
        mLastNode = next;

        if (FluidConfig.DEBUG) Logger.d("nextBaseFragment(...) -> New Node, Parent: " + mLastNode);
      }

      next.data.mContext = context;
      next.data.mFluidFragments.add(fragment);

      mFragmentContexts.put(context, next);

      if (FluidConfig.DEBUG)
        Logger.d("nextBaseFragment(...) -> Context: " + context + ", ContextCount:" + mFragmentContexts.size());

      toMount = fragment;
    }

    toMount.setFluidFragmentManager(this);

    if (toMount.isVisible()) {
      toMount.fragmentUpdate(prevBundle);
      activity.onFragmentChange(FluidFragmentManager.this, mLastFragment, toMount, context, args);
      return;
    }

    // set the last active fragment in each context
    mLastNode.data.mLastActiveFragment = toMount;

    final FluidFragment<T> toMountFinal = toMount;
    final boolean removeChildrenFinal = removeChildren;
    FluidPostpone mountAction = () -> {

      if (removeChildrenFinal) {
        // remove all the children
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        removeChildrenFragment(transaction, mLastNode);
        transaction.commit();
      }

      fragmentManager
              .beginTransaction()
              .replace(getBaseId(), toMountFinal, tag)
              .addToBackStack(tag)
              .commit();

      activity.onFragmentChange(FluidFragmentManager.this, mLastFragment, toMountFinal, context, args);

      // set the last fragment
      mLastFragment = toMountFinal;

      setTransacting(false);
    };

    setTransacting(true);
    if (mLastFragment != null) {
      if (FluidConfig.DEBUG)
        Logger.d("nextBaseFragment() -> Hiding last fragment, Class: " + mLastFragment.getClass().getSimpleName());
      if (!mLastFragment.fragmentWillDismount(mountAction)) {
        mountAction.run();
      }
    } else {
      mountAction.run();
    }


  }

  private class BFMNode {
    List<FluidFragment<T>> mFluidFragments;
    String mContext;
    FluidFragment<T> mLastActiveFragment;

    BFMNode() {
      mFluidFragments = new ArrayList<>();
    }
  }

}