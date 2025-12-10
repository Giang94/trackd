package com.app.trackd.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.trackd.R;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.Artist;
import com.app.trackd.model.Tag;
import com.app.trackd.model.ref.AlbumTagCrossRef;
import com.app.trackd.util.ImageUtils;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class TaggingActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_TAG_IDS = "selectedTagIds";
    public static final String EXTRA_ALBUM_ID = "albumId";

    private long albumId;
    private AlbumWithArtists albumWithArtists;

    private FlexboxLayout chipGroup;
    private EditText newTagInput;
    private ImageButton btnAddTag;
    private Button btnSave;
    private ImageView ivCover;
    private TextView albumTitle, albumArtist;

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
        albumWithArtists = db.albumDao().getAlbumWithArtistsById(albumId);

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

        newTagInput.setInputType(InputType.TYPE_CLASS_TEXT);
        newTagInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        newTagInput.setSingleLine(true);

        ivCover = findViewById(R.id.ivCover);
        albumTitle = findViewById(R.id.albumTitle);
        albumArtist = findViewById(R.id.albumArtist);

        Album album = albumWithArtists.getAlbum();

        if (album.getCover() != null) {
            Bitmap coverBitmap = ImageUtils.toBitmap(album.getCover());
            ivCover.setImageBitmap(coverBitmap);
        }
        albumTitle.setText(album.getTitle());

        List<String> artists = albumWithArtists.getArtists().stream()
                .map(Artist::getDisplayName)
                .toList();

        albumArtist.setText(TextUtils.join(" • ", artists));
    }

    private void setupAddTagButton() {
        newTagInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnAddTag.performClick();
                return true;
            }
            return false;
        });

        // Tick button logic
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
        chip.setTag(R.id.delete_mode, false);

        chip.setOnCloseIconClickListener(v -> {
            chip.clearAnimation();

            new Thread(() -> {
                db.albumTagDao().deleteAllByTagId(tag.getId());
                db.tagDao().delete(tag);
            }).start();

            runOnUiThread(() -> {
                chipGroup.removeView(chip);
                allTags.remove(tag);

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
