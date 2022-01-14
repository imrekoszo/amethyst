/*
 * Copyright (c) 2020 the original author or authors.
 * Licensed under the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0
 */

package org.illyasviel.amethyst

import org.gradle.api.tasks.SourceSetOutput
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ASM8
import org.objectweb.asm.util.ASMifier
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets

/**
 * Bypass plugin compatibility verification, clojure's defrecord class make it unhappy.
 */
object InstrumentClojure {

    private val candidates = listOf(
      //  "cider.inlined_deps.toolsnamespace.v0v3v0_alpha4.clojure.tools.namespace.dependency.MapDependencyGraph",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.comment.CommentNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.fn.FnNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.forms.FormsNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.integer.IntNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.keyword.KeywordNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.meta.MetaNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.namespaced_map.MapQualifierNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.namespaced_map.NamespacedMapNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.quote.QuoteNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.reader_macro.DerefNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.reader_macro.ReaderMacroNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.reader_macro.ReaderNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.regex.RegexNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.seq.SeqNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.stringz.StringNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.token.SymbolNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.token.TokenNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.uneval.UnevalNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.whitespace.CommaNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.whitespace.NewlineNode",
        "refactor_nrepl.inlined_deps.rewrite_clj.v1v0v699_alpha.rewrite_clj.node.whitespace.WhitespaceNode"
    ).map { it.replace(".", File.separator) + ".class" }

    fun instrument(sourceSetOutput: SourceSetOutput) {
        println("enhancement clojure defrecord class")
//        val fileTree = sourceSetOutput.dirs.asFileTree
//        candidates.forEach { candidate ->
//            handleClass(fileTree.filter { it.path.endsWith(candidate) }.singleFile)
//        }
    }

    private fun handleClass(file: File) {
        File("build/asm/").mkdirs()
        PrintWriter("build/asm/${file.name}.txt", StandardCharsets.UTF_8).use { printer ->
            val cr = ClassReader(file.inputStream())
            val cw = ClassWriter(cr, 0)
            val tv = TraceClassVisitor(cw, printer)
            val adapter = AddAssocExMethodAdapter(tv)
            cr.accept(adapter, 0)

            file.writeBytes(cw.toByteArray())
        }
    }

    private fun debugClass(file: File) {
        File("build/asm/asmifier/").mkdirs()
        PrintWriter("build/asm/asmifier/${file.name}.java", StandardCharsets.UTF_8).use { printer ->
            val cr = ClassReader(File("").inputStream())
            val cv = TraceClassVisitor(null, ASMifier(), printer)
            cr.accept(cv, 0)
        }
    }
}

class AddAssocExMethodAdapter(cv: ClassVisitor) : ClassVisitor(ASM8, cv) {
    private var name: String? = null
    private var isAssocExPresent = false

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        this.name = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        if (name == "assocEx") {
            isAssocExPresent = true
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    override fun visitEnd() {
        if (name == null) throw IllegalStateException("name should not be null")
        if (!isAssocExPresent) {
            val mv = cv.visitMethod(ACC_PUBLIC, "assocEx",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Lclojure/lang/IPersistentMap;", null, null)
            mv.visitCode()
            val label0 = Label()
            mv.visitLabel(label0)
            mv.visitLineNumber(6, label0)
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsupportedOperationException")
            mv.visitInsn(Opcodes.DUP)
            mv.visitLdcInsn("Instrument by amethyst, this should not be thrown.")
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>",
                    "(Ljava/lang/String;)V", false)
            mv.visitInsn(Opcodes.ATHROW)
            val label1 = Label()
            mv.visitLabel(label1)
            mv.visitLocalVariable("this", "L$name;", null, label0, label1, 0)
            mv.visitLocalVariable("key", "Ljava/lang/Object;", null, label0, label1, 1)
            mv.visitLocalVariable("val", "Ljava/lang/Object;", null, label0, label1, 2)
            mv.visitMaxs(3, 3)
            mv.visitEnd()

            println("add method `assocEx` to $name")
        }
        super.visitEnd()
    }
}
