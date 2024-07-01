package com.atlassian.performance.tools.report.api.jfr

/**
 * JVM symbol examples:
 * - package names, e.g. `org/apache/tomcat/util/modeler`
 * - class names, e.g. `webwork/action/factory/JspActionFactoryProxy`
 * - method names, e.g. `compileSoy`
 * - method signatures, e.g. `(Lcom/google/template/soy/exprtree/ExprNode$ParentExprNode;)Ljava/util/List;`
 * - native function names, e.g. `AddNode::Ideal`
 *
 * To optimize JFR filtering, the byte array is not defensively copied.
 * @since 4.5.0
 */
class MutableJvmSymbol(
    /**
     * The binary representation of the JVM symbol. The bytes can be overwritten.
     */
    val payload: ByteArray
) {
    override fun toString() = String(payload)
}
