package com.jetbrains.rider.plugins.entityframeworkvisualizer

import com.intellij.database.console.JdbcConsole
import com.intellij.database.console.session.DatabaseSessionManager
import com.intellij.database.dataSource.DataSourceStorage
import com.intellij.database.dataSource.importDataSourceFromText
import com.intellij.database.datagrid.DataRequest
import com.intellij.database.plan.ExplainPlanProvider
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.application.writeIntentReadAction
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.uml.core.actions.ShowDiagram
import com.intellij.util.ui.UIUtil
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.debugger.DotNetStackFrame
import com.jetbrains.rider.debugger.IDotNetValue
import com.jetbrains.rider.debugger.evaluation.DotNetExpression
import com.jetbrains.rider.debugger.evaluators.RiderCustomComponentEvaluator
import com.jetbrains.rider.debugger.visualizers.RiderDebuggerValuePresenter
import com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage
import com.jetbrains.rider.model.debuggerWorker.ComputeObjectPropertiesArg
import com.jetbrains.rider.model.debuggerWorker.ObjectPropertiesBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.concurrency.await


class EntityFrameworkVisualizerEvaluator(
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
) : RiderCustomComponentEvaluator(
    node, properties, session, place, presenters,
    lifetime,
    onPopupBeingClicked,
    shouldIgnorePropertiesComputation,
    shouldUpdatePresentation, dotNetValue,
    actionName
) {
    override fun startEvaluation(p0: XFullValueEvaluationCallback) {
        lifetime.coroutineScope.launch(Dispatchers.Main) {
            val stackFrame = session.currentStackFrame as DotNetStackFrame
            val evaluator = stackFrame.evaluator

            val computeObjectPropertiesRequest = ComputeObjectPropertiesArg(
                true,
                true,
                false,
                null,
                emptyList(),
                null,
                null,
                null,
                null
            )
            val queryStringDotnetValue = evaluator.evaluate(
                DotNetExpression(
                    "${dotNetValue.customMemberName}.CreateDbCommand().CommandText",
                    CSharpLanguage,
                    EvaluationMode.CODE_FRAGMENT
                ), stackFrame.sourcePosition).await()
            val connectionStringDotnetValue = evaluator.evaluate(
                DotNetExpression(
                    // TODO: query.CreateDbCommand().Connection.ConnectionString does not include a password
//                    "${dotNetValue.customMemberName}.CreateDbCommand().Connection.ConnectionString",
                    "context.Database.GetConnectionString()",
                    CSharpLanguage,
                    EvaluationMode.CODE_FRAGMENT
                ), stackFrame.sourcePosition).await()

            val queryString = queryStringDotnetValue.objectProxy.computeObjectProperties.startSuspending(
                lifetime,
                computeObjectPropertiesRequest
            ).value.first().value
            val connectionString = connectionStringDotnetValue.objectProxy.computeObjectProperties.startSuspending(
                lifetime,
                computeObjectPropertiesRequest
            ).value.first().value

            thisLogger().info("Query: $queryString")
            thisLogger().info("Connection: $connectionString")

            val project = session.project
            // TODO: assumption that there is a one-and-only data source already
            val source = DataSourceStorage.getStorage(project).dataSources.first()
            // TODO: using importDataSourceFromText _SEEMS_ to expect an XML format similar to the following
            //    <data-source source="LOCAL" name="BloggingDB@localhost" uuid="204df24b-2fc7-4bba-87a9-4bcbf32299c2"><database-info product="Microsoft SQL Server" version="16.00.4175" jdbc-version="4.2" driver-name="JetBrains JDBC Driver for SQL Server" driver-version="1.0" dbms="MSSQL" exact-version="16.0.4175" exact-driver-version="1.0"><extra-name-characters>$#@</extra-name-characters><identifier-quote-string>&quot;</identifier-quote-string></database-info><case-sensitivity plain-identifiers="mixed" quoted-identifiers="mixed"/><driver-ref>sqlserver.jb</driver-ref><synchronize>true</synchronize><configured-by-url>true</configured-by-url><jdbc-driver>com.jetbrains.jdbc.sqlserver.SqlServerDriver</jdbc-driver><jdbc-url>Server=localhost,1433;Database=BloggingDB;Uid=sa;Pwd=Pa55w0rd;TrustServerCertificate=Yes;</jdbc-url><secret-storage>master_key</secret-storage><auth-provider>no-auth</auth-provider><schema-mapping><introspection-scope><node kind="database" qname="@"><node kind="schema" qname="@"/></node></introspection-scope></schema-mapping><working-dir>$ProjectFileDir$</working-dir></data-source>
//            val source = importDataSourceFromText(connectionString, project).first()
            val session = DatabaseSessionManager.getSession(project, source, "EF Visualizer")

            val provider = ExplainPlanProvider.getDefaultProvider(source)
            val console = JdbcConsole.newConsole(project).fromDataSource(source).useSession(session).build()
            val request: DataRequest.RawRequest? = provider?.createExplainRequest(
                console,
                { model -> UIUtil.invokeLaterIfNeeded { console.showPlan(model) } },
                console.dataSource, queryString, false
            )
            if (request != null)
                console.messageBus.dataProducer.processRequest(request)

//            val component = writeIntentReadAction { console.consoleView.component }
//            val dc = object : DataContext {
//                override fun getData(dataId: String): Any? {
//                    if (dataId == PlatformCoreDataKeys.CONTEXT_COMPONENT.name)
//                        return component
//                    if (dataId == PlatformCoreDataKeys.PROJECT.name)
//                        return project
//
//                    return null
//                }
//            }
//            val event = AnActionEvent.createEvent(dc, null, "Any place", ActionUiKind.NONE, null)
//            ShowDiagram.Default().actionPerformed(event)
        }
    }
}
