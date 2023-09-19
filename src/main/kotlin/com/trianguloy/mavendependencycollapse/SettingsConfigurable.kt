package com.trianguloy.mavendependencycollapse

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JPanel

/** Provides controller functionality for application settings. */
class SettingsConfigurable(project: Project) : Configurable {
    private val service = project.getService(SettingsState::class.java)
    private var component: SettingsComponent? = null
    override fun getDisplayName() = "Maven Dependency Collapse"

    override fun getPreferredFocusedComponent() = component?.preferredFocusedComponent

    override fun createComponent() = SettingsComponent().also { component = it }.panel

    override fun isModified() =
        service.collapsedText != component?.collapsedText || service.collapsedByDefault != component?.collapsedByDefault

    override fun apply() = component?.let {
        service.collapsedText = it.collapsedText
        service.collapsedByDefault = it.collapsedByDefault
    }.run {}

    override fun reset() = run {
        component?.collapsedText = service.collapsedText
        component?.collapsedByDefault = service.collapsedByDefault
    }

    override fun disposeUIResources() {
        component = null
    }
}

/** Supports creating and managing a [JPanel] for the Settings Dialog. */
class SettingsComponent {

    private val collapsedTextField = JBTextField()
    private val collapsedByDefaultField = JBCheckBox("Collapse dependencies by default")

    val panel = FormBuilder.createFormBuilder()
        .addComponent(collapsedByDefaultField, 1)
        .addSeparator()
        .addLabeledComponent("Collapsed text format: ", collapsedTextField, 1, true)
        .addComponent(
            JBLabel(
                """
            The collapsed text supports the following replacements:
                ${'$'}{node|replacement|notFound}
                ${'$'}{node|replacement}
                ${'$'}{node}
            
            Where:
            - node is the name of a node to find from the dependency itself. 
              Example: "artifactId"
            - replacement is a formatting string that will be used when the node exists, with the node text as parameter.
              Example: "[%s]"
              If replacement is not present, it is considered as "%s"
            - notFound is a string that will be used if node is not found.
              Example: "not found"
              If notFound is not present it is considered as an empty string.
            You can use \ to scape a character inside the brackets (like ${'$'}{node|{%s\}})
            Any other character outside will be shown without modification.
            
            
            Example:
            
            A pom.xml with
                <dependency>
                    <groupId>org.mapstruct</groupId>
                    <artifactId>mapstruct</artifactId>
                    <version>1.0</version>
                </dependency>
            
            with the replacement text:
                [${'$'}{artifactId}] [${'$'}{version|{%s\}}] [${'$'}{potato||potato not found}]
            
            Will result in the following collapsed text:
                [mapstruct] [{1.0}] [potato not found]
        """.trimIndent().toJLabelMultiline
            )
        )
        .addComponentFillVertically(JPanel(), 0)
        .addComponent(JButton("Restore default settings").apply {
            addActionListener {
                collapsedText = DEFAULT_COLLAPSED_TEXT
                collapsedByDefault = DEFAULT_COLLAPSED_BY_DEFAULT
            }
        })
        .panel

    val preferredFocusedComponent get() = collapsedTextField
    var collapsedText
        get() = collapsedTextField.getText()
        set(newText) = collapsedTextField.setText(newText)

    var collapsedByDefault: Boolean
        get() = collapsedByDefaultField.isSelected
        set(newStatus) = collapsedByDefaultField.setSelected(newStatus)
}


private val String.toJLabelMultiline
    get() = "<html>${replace("<","&lt;").replace(">","&gt;").replace(" ","&nbsp;").replace("\n", "<br>")}</html>"