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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AlbumFilterBottomSheet extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFilterApplied(boolean vinyl, boolean cds);
    }

    private boolean preCheckVinyl, preCheckCds;
    private FilterListener listener;

    public AlbumFilterBottomSheet(boolean vinyl, boolean cds, FilterListener listener) {
        this.preCheckVinyl = vinyl;
        this.preCheckCds = cds;
        this.listener = listener;
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

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }
}
