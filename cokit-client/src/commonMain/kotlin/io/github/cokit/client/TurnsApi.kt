package io.github.cokit.client

import io.github.cokit.rpc.JsonRpcSession

class TurnsApi internal constructor(
    private val rpc: JsonRpcSession,
) {
    suspend fun start(request: StartTurnRequest): Turn {
        return rpc.request(CodexRpc.Turn.Start, request.toRpcParams()).turn
    }

    suspend fun steer(request: SteerTurnRequest) {
        rpc.request(CodexRpc.Turn.Steer, request.toRpcParams())
    }

    suspend fun interrupt(request: InterruptTurnRequest) {
        rpc.request(CodexRpc.Turn.Interrupt, request.toRpcParams())
    }
}

private fun StartTurnRequest.toRpcParams(): TurnStartParams = TurnStartParams(
    threadId = threadId,
    input = input,
    cwd = cwd,
    approvalPolicy = approvalPolicy,
    sandbox = sandboxPolicy,
    permissions = permissions,
    model = model,
    effort = effort,
    outputSchema = outputSchema,
)

private fun SteerTurnRequest.toRpcParams(): TurnSteerParams = TurnSteerParams(
    threadId = threadId,
    expectedTurnId = expectedTurnId,
    input = input,
    clientUserMessageId = clientUserMessageId,
)

private fun InterruptTurnRequest.toRpcParams(): TurnInterruptParams = TurnInterruptParams(
    threadId = threadId,
    turnId = turnId,
)
