package io.github.cokit.client

import kotlinx.serialization.KSerializer

class CodexRpcMethod<P : Any, R : Any> internal constructor(
    val method: String,
    internal val paramsSerializer: KSerializer<P>,
    internal val resultSerializer: KSerializer<R>,
    internal val emptyResult: R? = null,
)

object CodexRpc {
    object Thread {
        val Start: CodexRpcMethod<ThreadStartParams, ThreadStartResult> = CodexRpcMethod(
            method = "thread/start",
            paramsSerializer = ThreadStartParams.serializer(),
            resultSerializer = ThreadStartResult.serializer(),
        )

        val Resume: CodexRpcMethod<ThreadResumeParams, ThreadResumeResult> = CodexRpcMethod(
            method = "thread/resume",
            paramsSerializer = ThreadResumeParams.serializer(),
            resultSerializer = ThreadResumeResult.serializer(),
        )

        val Fork: CodexRpcMethod<ThreadForkParams, ThreadForkResult> = CodexRpcMethod(
            method = "thread/fork",
            paramsSerializer = ThreadForkParams.serializer(),
            resultSerializer = ThreadForkResult.serializer(),
        )

        val List: CodexRpcMethod<ThreadListParams, ThreadListResult> = CodexRpcMethod(
            method = "thread/list",
            paramsSerializer = ThreadListParams.serializer(),
            resultSerializer = ThreadListResult.serializer(),
        )

        val Read: CodexRpcMethod<ThreadReadParams, ThreadReadResult> = CodexRpcMethod(
            method = "thread/read",
            paramsSerializer = ThreadReadParams.serializer(),
            resultSerializer = ThreadReadResult.serializer(),
        )

        val Archive: CodexRpcMethod<ThreadArchiveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/archive",
            paramsSerializer = ThreadArchiveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Unarchive: CodexRpcMethod<ThreadUnarchiveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/unarchive",
            paramsSerializer = ThreadUnarchiveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Unsubscribe: CodexRpcMethod<ThreadUnsubscribeParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/unsubscribe",
            paramsSerializer = ThreadUnsubscribeParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val SetName: CodexRpcMethod<ThreadSetNameParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/name/set",
            paramsSerializer = ThreadSetNameParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    object Turn {
        val Start: CodexRpcMethod<TurnStartParams, TurnStartResult> = CodexRpcMethod(
            method = "turn/start",
            paramsSerializer = TurnStartParams.serializer(),
            resultSerializer = TurnStartResult.serializer(),
        )

        val Steer: CodexRpcMethod<TurnSteerParams, CodexRpcUnit> = CodexRpcMethod(
            method = "turn/steer",
            paramsSerializer = TurnSteerParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Interrupt: CodexRpcMethod<TurnInterruptParams, CodexRpcUnit> = CodexRpcMethod(
            method = "turn/interrupt",
            paramsSerializer = TurnInterruptParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }
}
