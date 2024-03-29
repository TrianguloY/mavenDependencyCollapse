package com.trianguloy.mavendependencycollapse

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.impl.source.xml.XmlTagImpl
import com.intellij.psi.impl.source.xml.XmlTextImpl
import com.intellij.psi.impl.source.xml.XmlTokenImpl
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.xml.impl.GenericDomValueReference


val USER_DATA = Key.create<Boolean>("com.trianguloy.mavendependencycollapse")

private const val GROUP =
    """((?:\\.|[^\\|}])*)""" // something that doesn't contain '\' '|' or '}' (unless it's escaped)
private val REGEX = Regex("""\$\{$GROUP(?:\|$GROUP)?(?:\|$GROUP)?\}""") // ${something|maybethis|maybethat}

class DependencyFoldsBuilder : FoldingBuilderEx() {

    /** Creates and returns the folding of maven dependencies */
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean) =
        // get all dependencies
        root.getTagsByName("dependency")
            // convert into a descriptor using the data
            .map {
                // mark this node. This is the only way I found to be able to identify it later :(
                it.putUserData(USER_DATA, true)
                // create the descriptor
                val tagEndTokens = it.getTokensByType(XmlTokenType.XML_TAG_END)
                FoldingDescriptor(
                    it.node, TextRange(
                        // start from the first tag_end: <dependency[start]>... (or the whole element as failsafe)
                        tagEndTokens.firstOrNull()?.textRange?.startOffset ?: it.textRange.startOffset,
                        // end in the last tag_end: ...</dependency[end]> (or the whole element as failsafe)
                        tagEndTokens.lastOrNull()?.textRange?.startOffset ?: it.textRange.endOffset
                    )
                )
            }
            // and return as array
            .toList().toTypedArray()

    /** Generate the foldable text */
    override fun getPlaceholderText(node: ASTNode) = node.service.collapsedText.replace(REGEX) { match ->
        val (_, name, replacement, notFound) = match.groupValues.map { it.replace(Regex("""\\(.)"""), "$1") }

        node.getTagContentByName(name)?.let { replacement.ifEmpty { "%s" }.format(it) } ?: notFound
    }

    /** Collapsed by default */
    override fun isCollapsedByDefault(node: ASTNode) = node.service.collapsedByDefault

}

/** Returns the SettingsState service from a node. */
private val ASTNode.service
    get() = psi.project.getService(SettingsState::class.java)

/** Return the resolved content of a node by its tag */
private fun ASTNode.getTagContentByName(name: String) = psi.getTagsByName(name).firstOrNull()?.run {
    // find the resolved text
    getReferences(PsiReferenceService.Hints.NO_HINTS).find { it is GenericDomValueReference<*> }?.canonicalText
    // if not found, just return the verbatim text
        ?: PsiTreeUtil.findChildrenOfType(this, XmlTextImpl::class.java).firstOrNull()?.text
}

/** find XmlTagImpl with a given name */
private fun PsiElement.getTagsByName(name: String) =
    PsiTreeUtil.findChildrenOfType(this, XmlTagImpl::class.java).filter { it.name == name }

/** find XmlTokenImpl with a given elementType */
private fun XmlTagImpl.getTokensByType(elementType: IElementType) =
    PsiTreeUtil.findChildrenOfType(this, XmlTokenImpl::class.java).filter { it.elementType == elementType }