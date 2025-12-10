package com.app.trackd.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.trackd.R;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.model.Tag;
import com.app.trackd.model.ref.AlbumTagCrossRef;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class TaggingActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_TAG_IDS = "selectedTagIds";
    public static final String EXTRA_ALBUM_ID = "albumId";

    private long albumId;

    private FlexboxLayout chipGroup;
    private EditText newTagInput;
    private Button btnSave, btnAddTag;

    private AppDatabase db;
    private List<Tag> allTags;
    private boolean deleteMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tagging);

        albumId = getIntent().getLongExtra(EXTRA_ALBUM_ID, -1);
        if (albumId == -1) {
            finish();
            return;
        }

        db = AppDatabase.get(this);

        setupLayout();
        loadTags();
        setupAddTagButton();
        setupSaveButton();
    }

    private void setupLayout() {
        chipGroup = findViewById(R.id.chipGroupTags);
        newTagInput = findViewById(R.id.etNewTag);
        btnAddTag = findViewById(R.id.btnAddTag);
        btnSave = findViewById(R.id.btnSaveTags);

        // Long press anywhere in chipGroup to enter delete mode
        chipGroup.setOnLongClickListener(v -> {
            enterDeleteMode();
            return true;
        });

        newTagInput = findViewById(R.id.etNewTag);
        newTagInput.setInputType(InputType.TYPE_CLASS_TEXT);

        btnAddTag = findViewById(R.id.btnAddTag);
        btnSave = findViewById(R.id.btnSaveTags);

    }

    private void setupAddTagButton() {
        btnAddTag.setOnClickListener(v -> {
            String name = newTagInput.getText().toString().trim();
            if (!name.isEmpty()) {
                new Thread(() -> {
                    long id = db.tagDao().insert(new Tag(name));
                    Tag tag = new Tag(name);
                    tag.setId(id);
                    runOnUiThread(() -> {
                        addChip(tag);
                        allTags.add(tag);
                        newTagInput.setText("");
                    });
                }).start();
            }
        });
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            new Thread(() -> {
                List<Long> selectedTagIds = new ArrayList<>();
                for (int i = 0; i < chipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) chipGroup.getChildAt(i);
                    if (chip.isChecked()) {
                        selectedTagIds.add((Long) chip.getTag());
                    }
                }

                List<Long> existingTagIds = db.albumTagDao().getTagIdsForAlbum(albumId);

                for (Long tagId : selectedTagIds) {
                    if (!existingTagIds.contains(tagId)) {
                        db.albumTagDao().insert(new AlbumTagCrossRef(albumId, tagId));
                    }
                }

                for (Long tagId : existingTagIds) {
                    if (!selectedTagIds.contains(tagId)) {
                        db.albumTagDao().delete(new AlbumTagCrossRef(albumId, tagId));
                    }
                }

                Intent result = new Intent();
                result.putExtra(EXTRA_SELECTED_TAG_IDS, selectedTagIds.stream().mapToLong(Long::longValue).toArray());
                setResult(Activity.RESULT_OK, result);

                runOnUiThread(this::finish);
            }).start();
        });
    }

    private void loadTags() {
        new Thread(() -> {
            allTags = db.tagDao().getAll();
            List<Long> albumTagIds = db.albumTagDao().getTagIdsForAlbum(albumId);

            runOnUiThread(() -> {
                for (Tag tag : allTags) {
                    Chip chip = addChip(tag);
                    if (albumTagIds.contains(tag.getId())) {
                        chip.setChecked(true);
                    }
                }
            });
        }).start();
    }

    private Chip addChip(Tag tag) {
        Chip chip = new Chip(this);
        chip.setText(tag.getName());
        chip.setCheckable(true);
        chip.setTag(tag.getId());
        chip.setCloseIconVisible(false);
        chip.setTag(R.id.delete_mode, false); // default not in delete mode

        chip.setOnCloseIconClickListener(v -> {
            // 1. First cancel ongoing animation
            chip.clearAnimation();

            // 2. Delete data in DB in background
            new Thread(() -> {
                db.albumTagDao().deleteAllByTagId(tag.getId());
                db.tagDao().delete(tag);
            }).start();

            // 3. Remove from UI
            runOnUiThread(() -> {
                chipGroup.removeView(chip);
                allTags.remove(tag);

                // If still in delete mode, re-show X icons on remaining chips
                if (deleteMode) {
                    for (int i = 0; i < chipGroup.getChildCount(); i++) {
                        ((Chip) chipGroup.getChildAt(i)).setCloseIconVisible(true);
                    }
                }
            });
        });


        attachChipLongPress(chip);

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT);
        int margin = dpToPx(4);
        lp.setMargins(margin, 0, margin, 0);
        chip.setLayoutParams(lp);
        chipGroup.addView(chip);
        return chip;
    }

    private void attachChipLongPress(Chip chip) {
        chip.setOnLongClickListener(v -> {
            enterDeleteMode();
            return true;
        });
    }

    private void enterDeleteMode() {
        if (deleteMode) return;
        deleteMode = true;
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            chip.setCloseIconVisible(true);
            chip.setTag(R.id.delete_mode, true);
            startShakeAnimation(chip);
        }
    }

    private void exitDeleteMode() {
        deleteMode = false;
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            chip.setCloseIconVisible(false);
            chip.setTag(R.id.delete_mode, false);
            chip.clearAnimation();
        }
    }

    private void startShakeAnimation(Chip chip) {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        chip.startAnimation(shake);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (deleteMode && ev.getAction() == MotionEvent.ACTION_DOWN) {
            int[] loc = new int[2];
            chipGroup.getLocationOnScreen(loc);
            float x = ev.getRawX();
            float y = ev.getRawY();
            if (x < loc[0] || x > loc[0] + chipGroup.getWidth() ||
                    y < loc[1] || y > loc[1] + chipGroup.getHeight()) {
                exitDeleteMode();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private int dpToPx(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }
}
