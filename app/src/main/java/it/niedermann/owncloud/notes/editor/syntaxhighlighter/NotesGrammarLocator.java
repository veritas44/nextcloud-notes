package it.niedermann.owncloud.notes.editor.syntaxhighlighter;

import io.noties.prism4j.annotations.PrismBundle;

@PrismBundle(
        includeAll = true,
        grammarLocatorClassName = ".NotesGrammarLocatorImpl"
)
public class NotesGrammarLocator {
}
