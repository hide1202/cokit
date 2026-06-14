package io.github.cokit.client

import io.github.cokit.rpc.JsonRpcSession

class ThreadsApi internal constructor(
    private val rpc: JsonRpcSession,
) {
    suspend fun start(request: StartThreadRequest = StartThreadRequest()): Thread {
        return rpc.request(CodexRpc.Thread.Start, request.toRpcParams()).thread
    }

    suspend fun resume(request: ResumeThreadRequest): Thread {
        return rpc.request(CodexRpc.Thread.Resume, request.toRpcParams()).thread
    }

    suspend fun fork(request: ForkThreadRequest): Thread {
        return rpc.request(CodexRpc.Thread.Fork, request.toRpcParams()).thread
    }

    suspend fun list(request: ListThreadsRequest = ListThreadsRequest()): ThreadList {
        val decoded = rpc.request(CodexRpc.Thread.List, request.toRpcParams())
        return ThreadList(decoded.threads, decoded.cursor)
    }

    suspend fun read(request: ReadThreadRequest): Thread {
        return rpc.request(CodexRpc.Thread.Read, request.toRpcParams()).thread
    }

    suspend fun archive(threadId: ThreadId) {
        rpc.request(CodexRpc.Thread.Archive, ThreadArchiveParams(threadId))
    }

    suspend fun unarchive(threadId: ThreadId) {
        rpc.request(CodexRpc.Thread.Unarchive, ThreadUnarchiveParams(threadId))
    }

    suspend fun unsubscribe(threadId: ThreadId) {
        rpc.request(CodexRpc.Thread.Unsubscribe, ThreadUnsubscribeParams(threadId))
    }

    suspend fun setName(request: SetThreadNameRequest) {
        rpc.request(CodexRpc.Thread.SetName, request.toRpcParams())
    }
}

private fun StartThreadRequest.toRpcParams(): ThreadStartParams = ThreadStartParams(
    cwd = cwd,
    approvalPolicy = approvalPolicy,
    sandbox = sandbox,
    permissions = permissions,
    model = model,
    effort = effort,
    personality = personality,
)

private fun ResumeThreadRequest.toRpcParams(): ThreadResumeParams = ThreadResumeParams(
    threadId = threadId,
    excludeTurns = excludeTurns,
    initialTurnsPage = initialTurnsPage,
)

private fun ForkThreadRequest.toRpcParams(): ThreadForkParams = ThreadForkParams(
    threadId = threadId,
    ephemeral = ephemeral,
    excludeTurns = excludeTurns,
)

private fun ListThreadsRequest.toRpcParams(): ThreadListParams = ThreadListParams(
    cursor = cursor,
    limit = limit,
    cwd = cwd,
    archived = archived,
    searchTerm = searchTerm,
)

private fun ReadThreadRequest.toRpcParams(): ThreadReadParams = ThreadReadParams(
    threadId = threadId,
    includeTurns = includeTurns,
)

private fun SetThreadNameRequest.toRpcParams(): ThreadSetNameParams = ThreadSetNameParams(
    threadId = threadId,
    name = name,
)
