package com.intellij.codeInspection.ex;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefImplicitConstructor;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.codeInspection.ui.InspectionTree;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.ex.DataConstantsEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;

import javax.swing.*;
import java.util.*;

/**
 * @author max
 */
public abstract class QuickFixAction extends AnAction {
  protected InspectionTool myTool;

  public InspectionResultsView getInvoker(AnActionEvent e) {
    return (InspectionResultsView)e.getDataContext().getData(DataConstantsEx.INSPECTION_VIEW);
  }

  protected QuickFixAction(String text, InspectionTool tool) {
    this(text, IconLoader.getIcon("/actions/createFromUsage.png"), null, tool);
  }

  protected QuickFixAction(String text, Icon icon, KeyStroke keyStroke, InspectionTool tool) {
    super(text, null, icon);
    myTool = tool;
    if (keyStroke != null) {
      registerCustomShortcutSet(new CustomShortcutSet(keyStroke), null);
    }
  }

  public void update(AnActionEvent e) {
    final InspectionResultsView view = getInvoker(e);
    if (view == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    final InspectionTree tree = view.getTree();
    if (!view.isSingleToolInSelection() || tree.getSelectedTool() != myTool) {
      e.getPresentation().setVisible(false);
      e.getPresentation().setEnabled(false);
      return;
    }

    if (!isProblemDescriptorsAcceptable() && tree.getSelectedElements().length > 0 ||
        isProblemDescriptorsAcceptable() && tree.getSelectedDescriptors().length > 0) {
      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(true);
    }
  }

  protected boolean isProblemDescriptorsAcceptable() {
    return false;
  }

  public String getText(RefElement where) {
    return getTemplatePresentation().getText();
  }

  public void actionPerformed(final AnActionEvent e) {
    if (isProblemDescriptorsAcceptable()) {
      final InspectionResultsView view = getInvoker(e);
      final InspectionTree tree = view.getTree();
      final ProblemDescriptor[] descriptors = tree.getSelectedDescriptors();
      if (descriptors.length > 0) {
        doApplyFix(view.getProject(), (DescriptorProviderInspection)tree.getSelectedTool(), descriptors);
        return;
      }
    }

    doApplyFix(getSelectedElements(e));
  }

  protected RefElement[] getSelectedElements(AnActionEvent e) {
    final InspectionResultsView invoker = getInvoker(e);
    if (invoker == null) return new RefElement[0];
    List<RefEntity> selection = new ArrayList<RefEntity>(Arrays.asList(invoker.getTree().getSelectedElements()));
    PsiDocumentManager.getInstance(invoker.getProject()).commitAllDocuments();
    Collections.sort(selection, new Comparator() {
      public int compare(Object o1, Object o2) {
        if (o1 instanceof RefElement && o2 instanceof RefElement) {
          RefElement r1 = (RefElement)o1;
          RefElement r2 = (RefElement)o2;
          int i1 = r1 instanceof RefImplicitConstructor ? 0 : r1.getElement().getTextOffset();
          int i2 = r2 instanceof RefImplicitConstructor ? 0 : r2.getElement().getTextOffset();
          if (i1 < i2) return 1;
          if (i1 == i2) return 0;
          return -1;
        }
        if (o1 instanceof RefElement){
          return 1;
        }
        if (o2 instanceof RefElement){
          return -1;
        }
        return ((RefEntity)o1).getName().compareTo(((RefEntity)o2).getName());
      }
    });

    return selection.toArray(new RefElement[selection.size()]);
  }

  private void doApplyFix(final Project project,
                          final DescriptorProviderInspection tool,
                          final CommonProblemDescriptor[] descriptors) {
    final Set<VirtualFile> readOnlyFiles = new com.intellij.util.containers.HashSet<VirtualFile>();
    for (CommonProblemDescriptor descriptor : descriptors) {
      final PsiElement psiElement = descriptor instanceof ProblemDescriptor ? ((ProblemDescriptor)descriptor).getPsiElement() : null;
      if (psiElement != null && !psiElement.isWritable()) {
        readOnlyFiles.add(psiElement.getContainingFile().getVirtualFile());
      }
    }

    if (!readOnlyFiles.isEmpty()) {
      final ReadonlyStatusHandler.OperationStatus operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(readOnlyFiles.toArray(new VirtualFile[readOnlyFiles.size()]));
      if (operationStatus.hasReadonlyFiles()) return;
    }

    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      public void run() {
        CommandProcessor.getInstance().markCurrentCommandAsComplex(project);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            for (CommonProblemDescriptor descriptor : descriptors) {
              final QuickFix[] fixes = descriptor.getFixes();
              if (fixes != null) {
                for (QuickFix fix : fixes) {
                  if (fix != null) {
                    final QuickFixAction quickFixAction = QuickFixAction.this;
                    if (quickFixAction instanceof LocalQuickFixWrapper &&
                        !((LocalQuickFixWrapper)quickFixAction).getFix().getClass().isInstance(fix)) {
                      continue;
                    }
                    if (descriptor instanceof ProblemDescriptor) {
                      if (((ProblemDescriptor)descriptor).getPsiElement() != null) {
                        ((LocalQuickFix)fix).applyFix(project, (ProblemDescriptor)descriptor);
                      }
                    } else {
                      ((GlobalQuickFix)fix).applyFix();
                    }
                    tool.ignoreProblem(descriptor, fix);
                  }
                }
              }
            }
          }
        });
      }
    }, getTemplatePresentation().getText(), null);
    ((InspectionManagerEx)InspectionManager.getInstance(project)).refreshViews();
  }

  public void doApplyFix(final RefElement[] refElements) {
    Set<VirtualFile> readOnlyFiles = getReadOnlyFiles(refElements);
    if (!readOnlyFiles.isEmpty()) {
      final Project project = refElements[0].getRefManager().getProject();
      final ReadonlyStatusHandler.OperationStatus operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(readOnlyFiles.toArray(new VirtualFile[readOnlyFiles.size()]));
      if (operationStatus.hasReadonlyFiles()) return;
    }
    if (refElements.length > 0) {
      final Project project = refElements[0].getRefManager().getProject();
      final boolean[] refreshNeeded = new boolean[1];

      CommandProcessor.getInstance().executeCommand(project, new Runnable() {
        public void run() {
          CommandProcessor.getInstance().markCurrentCommandAsComplex(project);
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
              refreshNeeded[0] = applyFix(refElements);
            }
          });
        }
      }, getTemplatePresentation().getText(), null);

      if (refreshNeeded[0]) {
        ((InspectionManagerEx)InspectionManager.getInstance(project)).refreshViews();
      }
    }
  }

  private Set<VirtualFile> getReadOnlyFiles(final RefElement[] refElements) {
    Set<VirtualFile> readOnlyFiles = new com.intellij.util.containers.HashSet<VirtualFile>();
    for (int i = 0; i < refElements.length; i++) {
      RefElement refElement = refElements[i];
      PsiElement psiElement = refElement.getElement();
      if (psiElement == null) continue;
      if (!psiElement.isWritable()) readOnlyFiles.add(psiElement.getContainingFile().getVirtualFile());
    }
    return readOnlyFiles;
  }

  /**
   * @return true if immediate UI update needed.
   */
  protected abstract boolean applyFix(RefElement[] refElements);
}
