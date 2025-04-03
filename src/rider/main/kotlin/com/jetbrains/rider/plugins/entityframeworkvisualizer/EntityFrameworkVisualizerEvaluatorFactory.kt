package com.jetbrains.rider.plugins.entityframeworkvisualizer

import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.debugger.DotNetStackFrame
import com.jetbrains.rider.debugger.IDotNetValue
import com.jetbrains.rider.debugger.dotnetDebugProcess
import com.jetbrains.rider.debugger.evaluation.DotNetExpression
import com.jetbrains.rider.debugger.evaluators.RiderCustomComponentEvaluator
import com.jetbrains.rider.debugger.evaluators.RiderCustomComponentEvaluatorFactory
import com.jetbrains.rider.debugger.visualizers.RiderDebuggerValuePresenter
import com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage
import com.jetbrains.rider.model.debuggerWorker.ObjectPropertiesBase
import com.jetbrains.rider.model.debuggerWorker.ObjectPropertiesProxy
import com.jetbrains.rider.model.debuggerWorker.ValueFlags

class EntityFrameworkVisualizerEvaluatorFactory : RiderCustomComponentEvaluatorFactory() {
    override fun createEvaluator(
        node: XValueNode,
        properties: ObjectPropertiesBase,
        session: XDebugSession,
        place: XValuePlace,
        presenters: List<RiderDebuggerValuePresenter>,
        lifetime: Lifetime,
        onPopupBeingClicked: () -> Unit,
        shouldIgnorePropertiesComputation: () -> Boolean,
        shouldUpdatePresentation: Boolean,
        dotNetValue: IDotNetValue,
        actionName: String
    ): RiderCustomComponentEvaluator {
        return EntityFrameworkVisualizerEvaluator(
            node,
            properties,
            session,
            place,
            presenters,
            lifetime.createNested(),
            onPopupBeingClicked,
            shouldIgnorePropertiesComputation,
            shouldUpdatePresentation,
            dotNetValue,
            actionName
        )
    }

    override fun isApplicable(node: XValueNode, properties: ObjectPropertiesBase, session: XDebugSession): Boolean {
        if (properties is ObjectPropertiesProxy) {
            // Check if the type is related to Entity Framework (unverified)
            val typeName = properties.instanceType.definitionTypeFullName
            return typeName.contains("EntityFramework") ||
                   typeName.contains("DbContext") ||
                   typeName.contains("IQueryable") ||
                   typeName.contains("DbQuery")
        }

        return false
    }
}
