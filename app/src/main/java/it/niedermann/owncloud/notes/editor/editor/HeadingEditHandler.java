package it.niedermann.owncloud.notes.editor.editor;

import android.text.Editable;
import android.text.Spanned;

import androidx.annotation.NonNull;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.core.spans.HeadingSpan;
import io.noties.markwon.editor.EditHandler;
import io.noties.markwon.editor.PersistedSpans;

public class HeadingEditHandler implements EditHandler<HeadingSpan> {

    private MarkwonTheme theme;
    private int size;

    public HeadingEditHandler(int size) {
        this.size = size;
    }

    @Override
    public void init(@NonNull Markwon markwon) {
        this.theme = markwon.configuration().theme();
    }

    @Override
    public void configurePersistedSpans(@NonNull PersistedSpans.Builder builder) {
        builder.persistSpan(HeadingSpan.class, () -> new HeadingSpan(theme, size));
    }

    @Override
    public void handleMarkdownSpan(
            @NonNull PersistedSpans persistedSpans,
            @NonNull Editable editable,
            @NonNull String input,
            @NonNull HeadingSpan span,
            int spanStart,
            int spanTextLength) {
        String hashes = input.substring(spanStart, spanStart + spanTextLength + size + 1);
        for (int i = 0; i < size; i++) {
            if (hashes.charAt(i) != '#') {
                return;
            }
        }
        if (input.charAt(spanStart + size) != ' ') {
            return;
        }
        editable.setSpan(
                persistedSpans.get(HeadingSpan.class),
                spanStart,
                spanStart + spanTextLength + size + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    @NonNull
    @Override
    public Class<HeadingSpan> markdownSpanType() {
        return HeadingSpan.class;
    }
}
