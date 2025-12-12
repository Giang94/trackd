package com.app.trackd.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.trackd.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AlbumFilterBottomSheet extends BottomSheetDialogFragment {

    private static final float MAX_BOTTOM_SHEET_HEIGHT = 0.75f;
    private final boolean preCheckVinyl;
    private final boolean preCheckCds;
    private final FilterListener listener;

    public AlbumFilterBottomSheet(boolean vinyl, boolean cds, FilterListener listener) {
        this.preCheckVinyl = vinyl;
        this.preCheckCds = cds;
        this.listener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();

        setupBehaviors();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_filter, container, false);

        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);

        CheckBox cbVinyl = view.findViewById(R.id.cbVinyl);
        CheckBox cbCds = view.findViewById(R.id.cbCds);
        ImageButton btnApply = view.findViewById(R.id.btnApply);

        cbVinyl.setChecked(preCheckVinyl);
        cbCds.setChecked(preCheckCds);

        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterApplied(cbVinyl.isChecked(), cbCds.isChecked());
            }
            dismiss();
        });

        return view;
    }

    private void setupBehaviors() {
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            bottomSheet.post(() -> {
                int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * MAX_BOTTOM_SHEET_HEIGHT);

                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                bottomSheet.requestLayout();

                behavior.setPeekHeight(maxHeight, true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            });
        }
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }

    public interface FilterListener {
        void onFilterApplied(boolean vinyl, boolean cds);
    }
}
