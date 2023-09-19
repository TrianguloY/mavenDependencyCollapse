package com.trianguloy.mavendependencycollapse

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Supports storing the application settings in a persistent way.
 * The [State] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
    name = "com.trianguloy.mavendependencycollapse.DependencyFoldsState",
    storages = [Storage("DependencyFoldPlugin.xml")]
)
@Service(Service.Level.PROJECT)
class SettingsState : PersistentStateComponent<SettingsState?> {
    var collapsedText = DEFAULT_COLLAPSED_TEXT
    var collapsedByDefault = DEFAULT_COLLAPSED_BY_DEFAULT
    override fun getState() = this

    override fun loadState(state: SettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}

const val DEFAULT_COLLAPSED_TEXT = """ ${'$'}{scope|[%s] }${'$'}{groupId||{no groupId\}} : ${'$'}{artifactId||{no artifactId\}}${'$'}{version| : %s} """
const val DEFAULT_COLLAPSED_BY_DEFAULT = true