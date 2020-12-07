package com.jetbrains.jet.ide

import com.jetbrains.jet.engine.*
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import org.kordamp.ikonli.antdesignicons.AntDesignIconsFilled
import org.kordamp.ikonli.swing.FontIcon
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.CompletableFuture
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities.invokeLater
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

// Have to be lazy to make UI scaling work
private val APP_ICON by lazy { FontIcon.of(AntDesignIconsFilled.CODE, 64).toImageIcon().image }
private val ERROR_ICON by lazy { FontIcon.of(AntDesignIconsFilled.EXCLAMATION_CIRCLE, 16, Color.RED) }

class IDE(code: String = "") : JFrame("Jet IDE"), RT {
    private var currentProgramExecution: CompletableFuture<Unit>? = null

    private val editorTextArea = RSyntaxTextArea()
    private val editorScrollPane = RTextScrollPane(editorTextArea).apply {
        isIconRowHeaderEnabled = true
    }
    private val gutter = editorScrollPane.gutter
    private val outputTextArea = JTextArea().apply { isEditable = false }
    private val outputScrollPane = JScrollPane(outputTextArea)
    private val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, outputScrollPane).apply {
        resizeWeight = 0.75
    }

    init {
        iconImage = APP_ICON
        contentPane.add(splitPane, BorderLayout.CENTER)
        defaultCloseOperation = EXIT_ON_CLOSE
        setLocationRelativeTo(null)
        size = Dimension(800, 600)
        extendedState = extendedState or MAXIMIZED_BOTH
        editorTextArea.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_PYTHON

        editorTextArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {}

            override fun removeUpdate(e: DocumentEvent?) {}

            override fun changedUpdate(e: DocumentEvent?) {
                update()
            }
        })

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                currentProgramExecution?.cancel(true)
            }

            override fun windowOpened(e: WindowEvent?) {
                editorTextArea.text = code
            }
        })
    }

    private fun update() {
        currentProgramExecution?.cancel(true)
        outputTextArea.text = ""
        gutter.removeAllTrackingIcons()
        if (editorTextArea.text.isBlank()) {
            return
        }
        try {
            this.currentProgramExecution = parse(editorTextArea.text)
                .run(this)
                .apply {
                    exceptionally {
                        when (it) {
                            is ExecutionException -> error(it.line, it.column, it.message)
                        }
                    }
                }
        } catch (e: SyntaxException) {
            e.errors.forEach { error(it.line, it.column, it.message) }
        }
    }

    override fun out(o: Any) {
        invokeLater { outputTextArea.text += o.toString() }
    }

    override val reducer: Reducer = ParallelReducer()

    private fun error(line: Int, column: Int, message: String) {
        invokeLater {
            outputTextArea.text += "\n$line:$column $message"
            gutter.addLineTrackingIcon(line - 1, ERROR_ICON, message)
        }
    }
}

private const val UI_SCALE_PROPERTY = "sun.java2d.uiScale"

fun main() {
    System.setProperty(UI_SCALE_PROPERTY, System.getProperty(UI_SCALE_PROPERTY) ?: "2")

    val test = IDE::class.java.getResource("/demo.jet").readText()
    val ide = IDE(test)
    ide.isVisible = true
}
