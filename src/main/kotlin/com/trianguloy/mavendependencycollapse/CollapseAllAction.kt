package com.trianguloy.mavendependencycollapse

import com.intellij.codeInsight.folding.impl.EditorFoldingInfo
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys

class CollapseAllAction : AnAction() {
    override fun update(e: AnActionEvent) {
        // enable if there is at least 1 custom folding region
        e.presentation.isEnabledAndVisible = e.foldingRegions?.isNotEmpty() ?: false
    }

    override fun actionPerformed(e: AnActionEvent) {
        // collapse all folding regions
        e.getData(LangDataKeys.EDITOR)?.foldingModel
            ?.runBatchFoldingOperation({ e.foldingRegions?.forEach { it.isExpanded = false } }, true, true)
    }

    /**
     * get our folding regions
     */
    private val AnActionEvent.foldingRegions
        get() = getData(LangDataKeys.EDITOR)?.foldingModel?.allFoldRegions?.filter { EditorFoldingInfo.get(it.editor).getPsiElement(it)?.getUserData(USER_DATA) == true }
}
