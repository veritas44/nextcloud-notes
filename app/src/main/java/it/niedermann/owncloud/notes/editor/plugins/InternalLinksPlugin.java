package it.niedermann.owncloud.notes.editor.plugins;

import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;

import org.commonmark.node.Link;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.SpanFactory;
import io.noties.markwon.core.CorePlugin;
import it.niedermann.owncloud.notes.util.NoteLinksUtils;

public class InternalLinksPlugin extends AbstractMarkwonPlugin {

    @NonNull
    private final Set<String> existingRemoteIds = new HashSet<>();

    public InternalLinksPlugin(@NonNull Collection<? extends String> existingRemoteIds) {
        this.existingRemoteIds.addAll(existingRemoteIds);
    }

    @Override
    public void configure(@NonNull Registry registry) {
        registry.require(CorePlugin.class, corePlugin -> corePlugin.addOnTextAddedListener((visitor, text, start) -> {

            // @since 4.2.0 obtain span factory for links
            //  we will be using the link that is used by markdown (instead of directly applying URLSpan)
            final SpanFactory spanFactory = visitor.configuration().spansFactory().get(Link.class);
            if (spanFactory == null) {
                return;
            }

            // @since 4.2.0 we no longer re-use builder (thread safety achieved for
            //  render calls from different threads and ... better performance)
            final SpannableStringBuilder builder = new SpannableStringBuilder(text);

            // TARGET LOGIC

            // .setOnLinkClickCallback((view, link) -> {
            //     if (NoteLinksUtils.isNoteLink(link)) {
            //         long noteRemoteId = NoteLinksUtils.extractNoteRemoteId(link);
            //         long noteLocalId = db.getLocalIdByRemoteId(this.note.getAccountId(), noteRemoteId);
            //         Intent intent = new Intent(requireActivity().getApplicationContext(), EditNoteActivity.class);
            //         intent.putExtra(EditNoteActivity.PARAM_NOTE_ID, noteLocalId);
            //         startActivity(intent);
            //     } else {
            //         Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            //         startActivity(browserIntent);
            //     }
            // })


            // COPDIED FROM LINKIFY PLUGIN

            // if (Linkify.addLinks(builder, mask)) {
            //     // target URL span specifically
            //     final URLSpan[] spans = builder.getSpans(0, builder.length(), URLSpan.class);
            //     if (spans != null
            //             && spans.length > 0) {
            //
            //         final RenderProps renderProps = visitor.renderProps();
            //         final SpannableBuilder spannableBuilder = visitor.builder();
            //
            //         for (URLSpan span : spans) {
            //             CoreProps.LINK_DESTINATION.set(renderProps, span.getURL());
            //             SpannableBuilder.setSpans(
            //                     spannableBuilder,
            //                     spanFactory.getSpans(visitor.configuration(), renderProps),
            //                     start + builder.getSpanStart(span),
            //                     start + builder.getSpanEnd(span)
            //             );
            //         }
            //     }
            // }
        }));
    }

    @NonNull
    @Override
    public String processMarkdown(@NonNull String markdown) {
        return NoteLinksUtils.replaceNoteLinksWithDummyUrls(markdown, existingRemoteIds);
    }
}
