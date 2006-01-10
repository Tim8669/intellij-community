/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.intellij.codeInspection.reference;

import com.intellij.openapi.module.Module;

import java.util.ArrayList;

/**
 * User: anna
 * Date: 09-Jan-2006
 */
public class RefModuleImpl extends RefEntityImpl implements RefModule {
  private Module myModule;

  protected RefModuleImpl(Module module) {
    super(module.getName());
    myModule = module;
  }

  public void add(RefEntity child) {
    if (myChildren == null) {
       myChildren = new ArrayList<RefEntity>();
    }
    myChildren.add(child);
  }

  protected void removeChild(RefEntity child) {
    if (myChildren != null) {
      myChildren.remove(child);
    }
  }

  public void accept(final RefVisitor refVisitor) {
    refVisitor.visitModule(this);
  }

  public Module getModule() {
    return myModule;
  }
}
