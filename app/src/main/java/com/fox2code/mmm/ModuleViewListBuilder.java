package com.fox2code.mmm;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fox2code.mmm.installer.InstallerInitializer;
import com.fox2code.mmm.manager.ModuleInfo;
import com.fox2code.mmm.manager.ModuleManager;
import com.fox2code.mmm.repo.RepoManager;
import com.fox2code.mmm.repo.RepoModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class ModuleViewListBuilder {
    private static final String TAG = "ModuleViewListBuilder";
    private final EnumSet<NotificationType> notifications = EnumSet.noneOf(NotificationType.class);
    private final HashMap<String, ModuleHolder> mappedModuleHolders = new HashMap<>();
    private final Object updateLock = new Object();
    private final Object queryLock = new Object();
    private final Activity activity;
    @NonNull
    private String query = "";
    private int footerPx;
    private boolean noUpdate;

    public ModuleViewListBuilder(Activity activity) {
        this.activity = activity;
    }

    public void addNotification(NotificationType notificationType) {
        synchronized (this.updateLock) {
            this.notifications.add(notificationType);
        }
    }

    public void appendInstalledModules() {
        synchronized (this.updateLock) {
            for (ModuleHolder moduleHolder : this.mappedModuleHolders.values()) {
                moduleHolder.moduleInfo = null;
            }
            ModuleManager moduleManager = ModuleManager.getINSTANCE();
            moduleManager.runAfterScan(() -> {
                Log.i(TAG, "A1: " + moduleManager.getModules().size());
                for (ModuleInfo moduleInfo : moduleManager.getModules().values()) {
                    ModuleHolder moduleHolder = this.mappedModuleHolders.get(moduleInfo.id);
                    if (moduleHolder == null) {
                        this.mappedModuleHolders.put(moduleInfo.id,
                                moduleHolder = new ModuleHolder(moduleInfo.id));
                    }
                    moduleHolder.moduleInfo = moduleInfo;
                }
            });
        }
    }

    public void appendRemoteModules() {
        synchronized (this.updateLock) {
            boolean showIncompatible = MainApplication.isShowIncompatibleModules();
            for (ModuleHolder moduleHolder : this.mappedModuleHolders.values()) {
                moduleHolder.repoModule = null;
            }
            RepoManager repoManager = RepoManager.getINSTANCE();
            repoManager.runAfterUpdate(() -> {
                Log.i(TAG, "A2: " + repoManager.getModules().size());
                for (RepoModule repoModule : repoManager.getModules().values()) {
                    ModuleInfo moduleInfo = repoModule.moduleInfo;
                    if (!showIncompatible && (moduleInfo.minApi > Build.VERSION.SDK_INT ||
                            (moduleInfo.maxApi != 0 && moduleInfo.maxApi < Build.VERSION.SDK_INT) ||
                            // Only check Magisk compatibility if root is present
                            (InstallerInitializer.peekMagiskPath() != null &&
                                    repoModule.moduleInfo.minMagisk >
                                            InstallerInitializer.peekMagiskVersion()
                            )))
                        continue; // Skip adding incompatible modules
                    ModuleHolder moduleHolder = this.mappedModuleHolders.get(repoModule.id);
                    if (moduleHolder == null) {
                        this.mappedModuleHolders.put(repoModule.id,
                                moduleHolder = new ModuleHolder(repoModule.id));
                    }
                    moduleHolder.repoModule = repoModule;
                }
            });
        }
    }

    public void applyTo(RecyclerView moduleList, ModuleViewAdapter moduleViewAdapter) {
        if (this.noUpdate) return;
        this.noUpdate = true;
        final ArrayList<ModuleHolder> moduleHolders;
        final int newNotificationsLen;
        try {
            synchronized (this.updateLock) {
                // Build start
                moduleHolders = new ArrayList<>();
                int special = 0;
                Iterator<NotificationType> notificationTypeIterator = this.notifications.iterator();
                while (notificationTypeIterator.hasNext()) {
                    NotificationType notificationType = notificationTypeIterator.next();
                    if (notificationType.shouldRemove()) {
                        notificationTypeIterator.remove();
                    } else {
                        if (notificationType.special) special++;
                        moduleHolders.add(new ModuleHolder(notificationType));
                    }
                }
                newNotificationsLen = this.notifications.size() - special;
                EnumSet<ModuleHolder.Type> headerTypes = EnumSet.of(
                        ModuleHolder.Type.NOTIFICATION, ModuleHolder.Type.SEPARATOR);
                Iterator<ModuleHolder> moduleHolderIterator = this.mappedModuleHolders.values().iterator();
                synchronized (this.queryLock) {
                    while (moduleHolderIterator.hasNext()) {
                        ModuleHolder moduleHolder = moduleHolderIterator.next();
                        if (moduleHolder.shouldRemove()) {
                            moduleHolderIterator.remove();
                        } else {
                            ModuleHolder.Type type = moduleHolder.getType();
                            if (matchFilter(moduleHolder)) {
                                if (headerTypes.add(type)) {
                                    moduleHolders.add(new ModuleHolder(type));
                                }
                                moduleHolders.add(moduleHolder);
                            }
                        }
                    }
                }
                Collections.sort(moduleHolders, ModuleHolder::compareTo);
                Log.i(TAG, "Got " + moduleHolders.size() + " entries!");
                // Build end
            }
        } finally {
            this.noUpdate = false;
        }
        this.activity.runOnUiThread(() -> {
            final EnumSet<NotificationType> oldNotifications =
                    EnumSet.noneOf(NotificationType.class);
            if (this.footerPx != 0) {
                moduleHolders.add(new ModuleHolder(this.footerPx));
            }
            boolean isTop = !moduleList.canScrollVertically(-1);
            boolean isBottom = !isTop && !moduleList.canScrollVertically(1);
            int oldNotificationsLen = 0;
            int oldOfflineModulesLen = 0;
            for (ModuleHolder moduleHolder : moduleViewAdapter.moduleHolders) {
                NotificationType notificationType = moduleHolder.notificationType;
                if (notificationType != null) {
                    oldNotifications.add(notificationType);
                    if (!notificationType.special)
                        oldNotificationsLen++;
                }
                if (moduleHolder.separator == ModuleHolder.Type.INSTALLABLE)
                    break;
                oldOfflineModulesLen++;
            }
            oldOfflineModulesLen -= oldNotificationsLen;
            int newOfflineModulesLen = 0;
            for (ModuleHolder moduleHolder : moduleHolders) {
                if (moduleHolder.separator == ModuleHolder.Type.INSTALLABLE)
                    break;
                newOfflineModulesLen++;
            }
            newOfflineModulesLen -= newNotificationsLen;
            moduleViewAdapter.moduleHolders.size();
            int newLen = moduleHolders.size();
            int oldLen = moduleViewAdapter.moduleHolders.size();
            moduleViewAdapter.moduleHolders.clear();
            moduleViewAdapter.moduleHolders.addAll(moduleHolders);
            if (oldNotificationsLen != newNotificationsLen ||
                    !oldNotifications.equals(this.notifications)) {
                notifySizeChanged(moduleViewAdapter, 0,
                        oldNotificationsLen, newNotificationsLen);
            }
            if (newLen - newNotificationsLen == 0) {
                notifySizeChanged(moduleViewAdapter, newNotificationsLen,
                        oldLen - oldNotificationsLen, 0);
            } else {
                notifySizeChanged(moduleViewAdapter, newNotificationsLen,
                        oldOfflineModulesLen, newOfflineModulesLen);
                notifySizeChanged(moduleViewAdapter,
                        newNotificationsLen + newOfflineModulesLen,
                        oldLen - oldNotificationsLen - oldOfflineModulesLen,
                        newLen - newNotificationsLen - newOfflineModulesLen);
            }
            if (isTop) moduleList.scrollToPosition(0);
            if (isBottom) moduleList.scrollToPosition(newLen);
        });
    }

    public void setFooterPx(int footerPx) {
        this.footerPx = Math.max(footerPx, 0);
    }

    private boolean matchFilter(ModuleHolder moduleHolder) {
        if (this.query.isEmpty()) return true;
        ModuleInfo moduleInfo = moduleHolder.getMainModuleInfo();
        return moduleInfo.id.toLowerCase(Locale.ROOT).contains(this.query) ||
                moduleInfo.name.toLowerCase(Locale.ROOT).contains(this.query);
    }

    private static void notifySizeChanged(ModuleViewAdapter moduleViewAdapter,
                                          int index, int oldLen, int newLen) {
        // Log.i(TAG, "A: " + index + " " + oldLen + " " + newLen);
        if (oldLen == newLen) {
            if (newLen != 0)
                moduleViewAdapter.notifyItemRangeChanged(index, newLen);
        } else if (oldLen < newLen) {
            if (oldLen != 0)
                moduleViewAdapter.notifyItemRangeChanged(index, oldLen);
            moduleViewAdapter.notifyItemRangeInserted(
                    index + oldLen, newLen - oldLen);
        } else {
            if (newLen != 0)
                moduleViewAdapter.notifyItemRangeChanged(index, newLen);
            moduleViewAdapter.notifyItemRangeRemoved(
                    index + newLen, oldLen - newLen);
        }
    }

    public void setQuery(String query) {
        synchronized (this.queryLock) {
            Log.i(TAG, "Query " + this.query + " -> " + query);
            this.query = query == null ? "" :
                    query.trim().toLowerCase(Locale.ROOT);
        }
    }

    public boolean setQueryChange(String query) {
        synchronized (this.queryLock) {
            String newQuery = query == null ? "" :
                    query.trim().toLowerCase(Locale.ROOT);
            Log.i(TAG, "Query change " + this.query + " -> " + newQuery);
            if (this.query.equals(newQuery))
                return false;
            this.query = newQuery;
        }
        return true;
    }
}
