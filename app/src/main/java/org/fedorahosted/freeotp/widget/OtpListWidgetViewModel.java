package org.fedorahosted.freeotp.widget;

import android.util.SparseArray;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by root on 17/04/17.
 */
final class OtpListWidgetViewModel {
    private final static SparseArray<OtpListWidgetViewModel> models = new SparseArray<>();

    private final Set<Integer> codePositionsToShow = new HashSet<>();

    private OtpListWidgetViewModel() {
    }

    static OtpListWidgetViewModel getInstance(int widgetId) {
        OtpListWidgetViewModel model = models.get(widgetId);
        if (model == null) {
            model = new OtpListWidgetViewModel();
            models.put(widgetId, model);
        }
        return model;
    }

    void addCodePositionToShow(int position) {
        codePositionsToShow.add(position);
    }

    void removeCodePositionToShow(int position) {
        codePositionsToShow.remove(position);
    }

    boolean shouldShowCodeInPosition(int position) {
        return codePositionsToShow.contains(position);
    }
}
