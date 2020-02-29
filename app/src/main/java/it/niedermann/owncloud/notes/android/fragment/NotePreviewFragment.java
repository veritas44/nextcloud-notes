package it.niedermann.owncloud.notes.android.fragment;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.SpanFactory;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListItem;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.ext.tasklist.TaskListSpan;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import it.niedermann.owncloud.notes.R;
import it.niedermann.owncloud.notes.databinding.FragmentNotePreviewBinding;
import it.niedermann.owncloud.notes.model.LoginStatus;
import it.niedermann.owncloud.notes.persistence.NotesDatabase;
import it.niedermann.owncloud.notes.util.DisplayUtils;
import it.niedermann.owncloud.notes.util.NoteLinksUtils;
import it.niedermann.owncloud.notes.util.SSOUtil;

import static it.niedermann.owncloud.notes.util.MarkDownUtil.CHECKBOX_CHECKED_MINUS;
import static it.niedermann.owncloud.notes.util.MarkDownUtil.CHECKBOX_CHECKED_STAR;
import static it.niedermann.owncloud.notes.util.MarkDownUtil.CHECKBOX_UNCHECKED_MINUS;
import static it.niedermann.owncloud.notes.util.MarkDownUtil.CHECKBOX_UNCHECKED_STAR;

public class NotePreviewFragment extends SearchableBaseNoteFragment implements OnRefreshListener {

    private String changedText;

    private Markwon markwon;

    private FragmentNotePreviewBinding binding;

    public static NotePreviewFragment newInstance(long accountId, long noteId) {
        NotePreviewFragment f = new NotePreviewFragment();
        Bundle b = new Bundle();
        b.putLong(PARAM_NOTE_ID, noteId);
        b.putLong(PARAM_ACCOUNT_ID, accountId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_edit).setVisible(true);
        menu.findItem(R.id.menu_preview).setVisible(false);
    }

    @Override
    public ScrollView getScrollView() {
        return binding.scrollView;
    }

    @Override
    protected FloatingActionButton getSearchNextButton() {
        return binding.searchNext;
    }

    @Override
    protected FloatingActionButton getSearchPrevButton() {
        return binding.searchPrev;
    }

    @Override
    protected Layout getLayout() {
        binding.singleNoteContent.onPreDraw();
        return binding.singleNoteContent.getLayout();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup
            container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotePreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        MarkdownProcessor markdownProcessor;
//        markdownProcessor = new MarkdownProcessor(requireContext());
//        markdownProcessor.factory(TextFactory.create());
//        markdownProcessor.config(
//                MarkDownUtil.getMarkDownConfiguration(noteContent.getContext())
//                        .setOnTodoClickCallback((view, line, lineNumber) -> {
//                                    try {
//                                        String[] lines = TextUtils.split(note.getContent(), "\\r?\\n");
//                                        /*
//                                         * Workaround for RxMarkdown-bug:
//                                         * When (un)checking a checkbox in a note which contains code-blocks, the "`"-characters get stripped out in the TextView and therefore the given lineNumber is wrong
//                                         * Find number of lines starting with ``` before lineNumber
//                                         */
//                                        for (int i = 0; i < lines.length; i++) {
//                                            if (lines[i].startsWith("```")) {
//                                                lineNumber++;
//                                            }
//                                            if (i == lineNumber) {
//                                                break;
//                                            }
//                                        }
//
//                                        /*
//                                         * Workaround for multiple RxMarkdown-bugs:
//                                         * When (un)checking a checkbox which is in the last line, every time it gets toggled, the last character of the line gets lost.
//                                         * When (un)checking a checkbox, every markdown gets stripped in the given line argument
//                                         */
//                                        if (line.startsWith(CHECKBOX_UNCHECKED_MINUS) || line.startsWith(CHECKBOX_UNCHECKED_STAR)) {
//                                            line = line.replace(CHECKBOX_UNCHECKED_MINUS, CHECKBOX_CHECKED_MINUS);
//                                            line = line.replace(CHECKBOX_UNCHECKED_STAR, CHECKBOX_CHECKED_STAR);
//                                        } else {
//                                            line = line.replace(CHECKBOX_CHECKED_MINUS, CHECKBOX_UNCHECKED_MINUS);
//                                            line = line.replace(CHECKBOX_CHECKED_STAR, CHECKBOX_UNCHECKED_STAR);
//                                        }
//
//                                        changedText = TextUtils.join("\n", lines);
//                                        noteContent.setText(markdownProcessor.parse(changedText));
//                                        saveNote(null);
//                                    } catch (IndexOutOfBoundsException e) {
//                                        Toast.makeText(getActivity(), R.string.checkbox_could_not_be_toggled, Toast.LENGTH_SHORT).show();
//                                        e.printStackTrace();
//                                    }
//                                    return line;
//                                }
//                        )
//                        .setOnLinkClickCallback((view, link) -> {
//                            if (NoteLinksUtils.isNoteLink(link)) {
//                                long noteRemoteId = NoteLinksUtils.extractNoteRemoteId(link);
//                                long noteLocalId = db.getLocalIdByRemoteId(this.note.getAccountId(), noteRemoteId);
//                                Intent intent = new Intent(requireActivity().getApplicationContext(), EditNoteActivity.class);
//                                intent.putExtra(EditNoteActivity.PARAM_NOTE_ID, noteLocalId);
//                                startActivity(intent);
//                            } else {
//                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
//                                startActivity(browserIntent);
//                            }
//                        })
//                        .build());

        //                .usePlugin(SyntaxHighlightPlugin.create(requireContext()))
        markwon = Markwon.builder(requireContext())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .usePlugin(TaskListPlugin.create(requireContext()))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(ImagesPlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                        SpanFactory origin = builder.getFactory(TaskListItem.class);

                        builder.setFactory(TaskListItem.class, (configuration, props) -> {
                            TaskListSpan span = (TaskListSpan) origin.getSpans(configuration, props);
                            ClickableSpan c = new ClickableSpan() {
                                @Override
                                public void onClick(@NonNull View widget) {
                                    Log.v("checkbox", "abcdef");
                                    span.setDone(!span.isDone());
                                    widget.invalidate();

                                    // it must be a TextView
                                    final TextView textView = (TextView) widget;
                                    // it must be spanned
                                    final Spanned spanned = (Spanned) textView.getText();

                                    // actual text of the span (this can be used along with the  `span`)
                                    final CharSequence task = spanned.subSequence(
                                            spanned.getSpanStart(this),
                                            spanned.getSpanEnd(this)
                                    );

                                    int lineNumber = 0;

                                    CharSequence textBeforeTask = spanned.subSequence(0, spanned.getSpanStart(this));
                                    for (int i = 0; i < textBeforeTask.length(); i++) {
                                        if (textBeforeTask.charAt(i) == '\n')
                                            lineNumber++;
                                    }

                                    // Work on the original content now, because the previous stuff is rendered and inline markdown might be removed at this point

                                    String[] lines = TextUtils.split(note.getContent(), "\\r?\\n");
                                    /*
                                     * Workaround fory RxMarkdown-bug:
                                     * When (un)checking a checkbox in a note which contains code-blocks, the "`"-characters get stripped out in the TextView and therefore the given lineNumber is wrong
                                     * Find number of lines starting with ``` before lineNumber
                                     */
                                    for (int i = 0; i < lines.length; i++) {
                                        if (lines[i].startsWith("```")) {
                                            lineNumber++;
                                        }
                                        if (i == lineNumber) {
                                            break;
                                        }
                                    }


                                    /*
                                     * Workaround for multiple RxMarkdown-bugs:
                                     * When (un)checking a checkbox which is in the last line, every time it gets toggled, the last character of the line gets lost.
                                     * When (un)checking a checkbox, every markdown gets stripped in the given line argument
                                     */
                                    if (lines[lineNumber].startsWith(CHECKBOX_UNCHECKED_MINUS) || lines[lineNumber].startsWith(CHECKBOX_UNCHECKED_STAR)) {
                                        lines[lineNumber] = lines[lineNumber].replace(CHECKBOX_UNCHECKED_MINUS, CHECKBOX_CHECKED_MINUS);
                                        lines[lineNumber] = lines[lineNumber].replace(CHECKBOX_UNCHECKED_STAR, CHECKBOX_CHECKED_STAR);
                                    } else {
                                        lines[lineNumber] = lines[lineNumber].replace(CHECKBOX_CHECKED_MINUS, CHECKBOX_UNCHECKED_MINUS);
                                        lines[lineNumber] = lines[lineNumber].replace(CHECKBOX_CHECKED_STAR, CHECKBOX_UNCHECKED_STAR);
                                    }

                                    changedText = TextUtils.join("\n", lines);
                                    markwon.setMarkdown(binding.singleNoteContent, changedText);
                                    saveNote(null);
                                }

                                @Override
                                public void updateDrawState(@NonNull TextPaint ds) {
                                    //NoOp
                                }
                            };
                            return new Object[]{
                                    span,
                                    c,
                            };
                        });
                    }
                })
//                .usePlugin(SyntaxHighlightPlugin.create(requireContext()))
                .

                        build();
        markwon.setMarkdown(binding.singleNoteContent, note.getContent());
//        try {
//            CharSequence parsedMarkdown = /*markdownProcessor.parse(*/NoteLinksUtils.replaceNoteLinksWithDummyUrls(note.getContent(), db.getRemoteIds(note.getAccountId()))/*)*/;
//            binding.noteContent.setText(parsedMarkdown);
//        } catch (StringIndexOutOfBoundsException e) {
//            // Workaround for RxMarkdown: https://github.com/stefan-niedermann/nextcloud-notes/issues/668
//            binding.noteContent.setText(NoteLinksUtils.replaceNoteLinksWithDummyUrls(note.getContent(), db.getRemoteIds(note.getAccountId())));
//            Toast.makeText(binding.noteContent.getContext(), R.string.could_not_load_preview_two_digit_numbered_list, Toast.LENGTH_LONG).show();
//            e.printStackTrace();
//        }
        changedText = note.getContent();
        binding.singleNoteContent.setMovementMethod(LinkMovementMethod.getInstance());

        db = NotesDatabase.getInstance(

                requireContext());
        binding.swiperefreshlayout.setOnRefreshListener(this);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireActivity().getApplicationContext());
        binding.singleNoteContent.setTextSize(TypedValue.COMPLEX_UNIT_PX,

                getFontSizeFromPreferences(sp));
        if (sp.getBoolean(

                getString(R.string.pref_key_font), false)) {
            binding.singleNoteContent.setTypeface(Typeface.MONOSPACE);
        }
    }

    @Override
    protected void colorWithText(String newText) {
        if (binding != null && ViewCompat.isAttachedToWindow(binding.singleNoteContent)) {
            binding.singleNoteContent.setText(/*markdownProcessor.parse(*/DisplayUtils.searchAndColor(getContent(), new SpannableString
                            (getContent()), newText, getResources().getColor(R.color.primary)/*)*/),
                    TextView.BufferType.SPANNABLE);
        }
    }

    @Override
    protected String getContent() {
        return changedText;
    }

    @Override
    public void onRefresh() {
        if (db.getNoteServerSyncHelper().isSyncPossible() && SSOUtil.isConfigured(getContext())) {
            binding.swiperefreshlayout.setRefreshing(true);
            try {
                SingleSignOnAccount ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(requireContext());
                db.getNoteServerSyncHelper().addCallbackPull(ssoAccount, () -> {
                    note = db.getNote(note.getAccountId(), note.getId());
                    binding.singleNoteContent.setText(/*markdownProcessor.parse(*/NoteLinksUtils.replaceNoteLinksWithDummyUrls(note.getContent(), db.getRemoteIds(note.getAccountId()))/*)*/);
                    binding.swiperefreshlayout.setRefreshing(false);
                });
                db.getNoteServerSyncHelper().scheduleSync(ssoAccount, false);
            } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
                e.printStackTrace();
            }
        } else {
            binding.swiperefreshlayout.setRefreshing(false);
            Toast.makeText(requireContext(), getString(R.string.error_sync, getString(LoginStatus.NO_NETWORK.str)), Toast.LENGTH_LONG).show();
        }
    }
}
