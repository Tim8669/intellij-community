/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide;

import com.intellij.ide.ui.UINumericRange;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Transient;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

@State(
  name = "GeneralSettings",
  storages = @Storage("ide.general.xml")
)
public class GeneralSettings implements PersistentStateComponent<GeneralSettings> {
  public static final int OPEN_PROJECT_ASK = -1;
  public static final int OPEN_PROJECT_NEW_WINDOW = 0;
  public static final int OPEN_PROJECT_SAME_WINDOW = 1;

  public enum ProcessCloseConfirmation {ASK, TERMINATE, DISCONNECT}

  public static final String PROP_INACTIVE_TIMEOUT = "inactiveTimeout";
  public static final String PROP_SUPPORT_SCREEN_READERS = "supportScreenReaders";

  static final UINumericRange SAVE_FILES_AFTER_IDLE_SEC = new UINumericRange(15, 1, 300);

  private String myBrowserPath = BrowserUtil.getDefaultAlternativeBrowserPath();
  private boolean myShowTipsOnStartup = true;
  private boolean myReopenLastProject = true;
  private boolean mySupportScreenReaders = false;
  private boolean mySyncOnFrameActivation = true;
  private boolean mySaveOnFrameDeactivation = true;
  private boolean myAutoSaveIfInactive = false;  // If true the IDEA automatically saves files if it is inactive for some seconds
  private int myInactiveTimeout = 15; // Number of seconds of inactivity after which IDEA automatically saves all files
  private boolean myUseSafeWrite = true;
  private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);
  private boolean myUseDefaultBrowser = true;
  private boolean mySearchInBackground;
  private boolean myConfirmExit = true;
  private int myConfirmOpenNewProject = OPEN_PROJECT_ASK;
  private ProcessCloseConfirmation myProcessCloseConfirmation = ProcessCloseConfirmation.ASK;

  public static GeneralSettings getInstance(){
    return ServiceManager.getService(GeneralSettings.class);
  }

  public GeneralSettings() {
  }

  public void addPropertyChangeListener(PropertyChangeListener listener){
    myPropertyChangeSupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener){
    myPropertyChangeSupport.removePropertyChangeListener(listener);
  }

  public String getBrowserPath() {
    return myBrowserPath;
  }

  /**
   * Use RecentProjectsManagerBase
   */
  @Deprecated
  public String getLastProjectCreationLocation() {
    return null;
  }

  /**
   * Use RecentProjectsManagerBase
   */
  @Deprecated
  public void setLastProjectCreationLocation(String lastProjectLocation) {
  }

  public void setBrowserPath(String browserPath) {
    myBrowserPath = browserPath;
  }

  @Deprecated
  public boolean showTipsOnStartup() {
    return isShowTipsOnStartup();
  }

  public boolean isShowTipsOnStartup() {
    return myShowTipsOnStartup;
  }

  public void setShowTipsOnStartup(boolean b) {
    myShowTipsOnStartup = b;
  }

  @Transient
  public int getLastTip() {
    return StringUtil.parseInt(PropertiesComponent.getInstance().getValue("lastTip"), 0);
  }

  public void setLastTip(int i) {
    PropertiesComponent.getInstance().setValue("lastTip", Integer.toString(i), "0");
  }

  public boolean isReopenLastProject() {
    return myReopenLastProject;
  }

  public void setReopenLastProject(boolean reopenLastProject) {
    myReopenLastProject = reopenLastProject;
  }

  public boolean isSupportScreenReaders() {
    return mySupportScreenReaders;
  }

  public void setSupportScreenReaders(boolean enabled) {
    boolean oldValue = mySupportScreenReaders;
    mySupportScreenReaders = enabled;
    myPropertyChangeSupport.firePropertyChange(
      PROP_SUPPORT_SCREEN_READERS, Boolean.valueOf(oldValue), Boolean.valueOf(enabled)
    );
  }

  public ProcessCloseConfirmation getProcessCloseConfirmation() {
    return myProcessCloseConfirmation;
  }

  public void setProcessCloseConfirmation(ProcessCloseConfirmation processCloseConfirmation) {
    myProcessCloseConfirmation = processCloseConfirmation;
  }

  @OptionTag("autoSyncFiles")
  public boolean isSyncOnFrameActivation() {
    return mySyncOnFrameActivation;
  }

  public void setSyncOnFrameActivation(boolean syncOnFrameActivation) {
    mySyncOnFrameActivation = syncOnFrameActivation;
  }

  @OptionTag("autoSaveFiles")
  public boolean isSaveOnFrameDeactivation() {
    return mySaveOnFrameDeactivation;
  }

  public void setSaveOnFrameDeactivation(boolean saveOnFrameDeactivation) {
    mySaveOnFrameDeactivation = saveOnFrameDeactivation;
  }

  /**
   * @return {@code true} if IDEA saves all files after "idle" timeout.
   */
  public boolean isAutoSaveIfInactive(){
    return myAutoSaveIfInactive;
  }

  public void setAutoSaveIfInactive(boolean autoSaveIfInactive) {
    myAutoSaveIfInactive = autoSaveIfInactive;
  }

  /**
   * @return timeout in seconds after which IDEA saves all files if there was no user activity.
   * The method always return positive (more then zero) value.
   */
  public int getInactiveTimeout(){
    return SAVE_FILES_AFTER_IDLE_SEC.fit(myInactiveTimeout);
  }

  public void setInactiveTimeout(int inactiveTimeoutSeconds) {
    int oldInactiveTimeout = myInactiveTimeout;

    myInactiveTimeout = SAVE_FILES_AFTER_IDLE_SEC.fit(inactiveTimeoutSeconds);
    myPropertyChangeSupport.firePropertyChange(
        PROP_INACTIVE_TIMEOUT, Integer.valueOf(oldInactiveTimeout), Integer.valueOf(myInactiveTimeout)
    );
  }

  public boolean isUseSafeWrite() {
    return myUseSafeWrite;
  }

  public void setUseSafeWrite(final boolean useSafeWrite) {
    myUseSafeWrite = useSafeWrite;
  }

  @Nullable
  @Override
  public GeneralSettings getState() {
    return this;
  }

  @Override
  public void loadState(GeneralSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean isUseDefaultBrowser() {
    return myUseDefaultBrowser;
  }

  public void setUseDefaultBrowser(boolean value) {
    myUseDefaultBrowser = value;
  }

  @Transient
  @Deprecated
  public boolean isConfirmExtractFiles() {
    return true;
  }

  @Deprecated
  public void setConfirmExtractFiles(boolean value) {
  }

  public boolean isConfirmExit() {
    return myConfirmExit;
  }

  public void setConfirmExit(boolean confirmExit) {
    myConfirmExit = confirmExit;
  }

  @MagicConstant(intValues = {OPEN_PROJECT_ASK, OPEN_PROJECT_NEW_WINDOW, OPEN_PROJECT_SAME_WINDOW})
  @interface OpenNewProjectOption {}
  /**
   * @return
   * <ul>
   * <li>{@link GeneralSettings#OPEN_PROJECT_NEW_WINDOW} if new project should be opened in new window
   * <li>{@link GeneralSettings#OPEN_PROJECT_SAME_WINDOW} if new project should be opened in same window
   * <li>{@link GeneralSettings#OPEN_PROJECT_ASK} if a confirmation dialog should be shown
   * </ul>
   */
  @OpenNewProjectOption
  @OptionTag("confirmOpenNewProject2")
  public int getConfirmOpenNewProject() {
    return myConfirmOpenNewProject;
  }

  public void setConfirmOpenNewProject(@OpenNewProjectOption int confirmOpenNewProject) {
    myConfirmOpenNewProject = confirmOpenNewProject;
  }

  public boolean isSearchInBackground() {
    return mySearchInBackground;
  }

  public void setSearchInBackground(final boolean searchInBackground) {
    mySearchInBackground = searchInBackground;
  }
}
