package io.github.cokit.client

import io.github.cokit.client.commands.CommandExecParams
import io.github.cokit.client.commands.CommandExecResizeParams
import io.github.cokit.client.commands.CommandExecResult
import io.github.cokit.client.commands.CommandExecTerminateParams
import io.github.cokit.client.commands.CommandExecWriteParams
import io.github.cokit.client.filesystem.FilesystemGetMetadataParams
import io.github.cokit.client.filesystem.FilesystemGetMetadataResult
import io.github.cokit.client.filesystem.FilesystemCopyParams
import io.github.cokit.client.filesystem.FilesystemCreateDirectoryParams
import io.github.cokit.client.filesystem.FilesystemReadDirectoryParams
import io.github.cokit.client.filesystem.FilesystemReadDirectoryResult
import io.github.cokit.client.filesystem.FilesystemReadFileParams
import io.github.cokit.client.filesystem.FilesystemReadFileResult
import io.github.cokit.client.filesystem.FilesystemRemoveParams
import io.github.cokit.client.filesystem.FilesystemUnwatchParams
import io.github.cokit.client.filesystem.FilesystemWriteFileParams
import io.github.cokit.client.filesystem.FilesystemWatchParams
import io.github.cokit.client.filesystem.FilesystemWatchResult
import io.github.cokit.client.process.ProcessKillParams
import io.github.cokit.client.process.ProcessResizePtyParams
import io.github.cokit.client.process.ProcessSpawnParams
import io.github.cokit.client.process.ProcessWriteStdinParams
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

        val Delete: CodexRpcMethod<ThreadDeleteParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/delete",
            paramsSerializer = ThreadDeleteParams.serializer(),
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

        val UpdateMetadata: CodexRpcMethod<ThreadMetadataUpdateParams, ThreadMetadataUpdateResult> = CodexRpcMethod(
            method = "thread/metadata/update",
            paramsSerializer = ThreadMetadataUpdateParams.serializer(),
            resultSerializer = ThreadMetadataUpdateResult.serializer(),
        )

        val SetGoal: CodexRpcMethod<ThreadGoalSetParams, ThreadGoalSetResult> = CodexRpcMethod(
            method = "thread/goal/set",
            paramsSerializer = ThreadGoalSetParams.serializer(),
            resultSerializer = ThreadGoalSetResult.serializer(),
        )

        val GetGoal: CodexRpcMethod<ThreadGoalGetParams, ThreadGoalGetResult> = CodexRpcMethod(
            method = "thread/goal/get",
            paramsSerializer = ThreadGoalGetParams.serializer(),
            resultSerializer = ThreadGoalGetResult.serializer(),
        )

        val ClearGoal: CodexRpcMethod<ThreadGoalClearParams, ThreadGoalClearResult> = CodexRpcMethod(
            method = "thread/goal/clear",
            paramsSerializer = ThreadGoalClearParams.serializer(),
            resultSerializer = ThreadGoalClearResult.serializer(),
        )

        val StartCompaction: CodexRpcMethod<ThreadCompactionStartParams, CodexRpcUnit> = CodexRpcMethod(
            method = "thread/compact/start",
            paramsSerializer = ThreadCompactionStartParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        @ExperimentalCodexApi
        val ListTurns: CodexRpcMethod<ThreadTurnsListParams, ThreadTurnsListResult> = CodexRpcMethod(
            method = "thread/turns/list",
            paramsSerializer = ThreadTurnsListParams.serializer(),
            resultSerializer = ThreadTurnsListResult.serializer(),
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

    object Command {
        val Exec: CodexRpcMethod<CommandExecParams, CommandExecResult> = CodexRpcMethod(
            method = "command/exec",
            paramsSerializer = CommandExecParams.serializer(),
            resultSerializer = CommandExecResult.serializer(),
        )

        val WriteStdin: CodexRpcMethod<CommandExecWriteParams, CodexRpcUnit> = CodexRpcMethod(
            method = "command/exec/write",
            paramsSerializer = CommandExecWriteParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Resize: CodexRpcMethod<CommandExecResizeParams, CodexRpcUnit> = CodexRpcMethod(
            method = "command/exec/resize",
            paramsSerializer = CommandExecResizeParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Terminate: CodexRpcMethod<CommandExecTerminateParams, CodexRpcUnit> = CodexRpcMethod(
            method = "command/exec/terminate",
            paramsSerializer = CommandExecTerminateParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    object Filesystem {
        val ReadFile: CodexRpcMethod<FilesystemReadFileParams, FilesystemReadFileResult> = CodexRpcMethod(
            method = "fs/readFile",
            paramsSerializer = FilesystemReadFileParams.serializer(),
            resultSerializer = FilesystemReadFileResult.serializer(),
        )

        val GetMetadata: CodexRpcMethod<FilesystemGetMetadataParams, FilesystemGetMetadataResult> = CodexRpcMethod(
            method = "fs/getMetadata",
            paramsSerializer = FilesystemGetMetadataParams.serializer(),
            resultSerializer = FilesystemGetMetadataResult.serializer(),
        )

        val ReadDirectory: CodexRpcMethod<FilesystemReadDirectoryParams, FilesystemReadDirectoryResult> =
            CodexRpcMethod(
                method = "fs/readDirectory",
                paramsSerializer = FilesystemReadDirectoryParams.serializer(),
                resultSerializer = FilesystemReadDirectoryResult.serializer(),
            )

        val WriteFile: CodexRpcMethod<FilesystemWriteFileParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/writeFile",
            paramsSerializer = FilesystemWriteFileParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val CreateDirectory: CodexRpcMethod<FilesystemCreateDirectoryParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/createDirectory",
            paramsSerializer = FilesystemCreateDirectoryParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Copy: CodexRpcMethod<FilesystemCopyParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/copy",
            paramsSerializer = FilesystemCopyParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Remove: CodexRpcMethod<FilesystemRemoveParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/remove",
            paramsSerializer = FilesystemRemoveParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Watch: CodexRpcMethod<FilesystemWatchParams, FilesystemWatchResult> = CodexRpcMethod(
            method = "fs/watch",
            paramsSerializer = FilesystemWatchParams.serializer(),
            resultSerializer = FilesystemWatchResult.serializer(),
        )

        val Unwatch: CodexRpcMethod<FilesystemUnwatchParams, CodexRpcUnit> = CodexRpcMethod(
            method = "fs/unwatch",
            paramsSerializer = FilesystemUnwatchParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }

    @ExperimentalCodexApi
    object Process {
        val Spawn: CodexRpcMethod<ProcessSpawnParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/spawn",
            paramsSerializer = ProcessSpawnParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val WriteStdin: CodexRpcMethod<ProcessWriteStdinParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/writeStdin",
            paramsSerializer = ProcessWriteStdinParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val Kill: CodexRpcMethod<ProcessKillParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/kill",
            paramsSerializer = ProcessKillParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )

        val ResizePty: CodexRpcMethod<ProcessResizePtyParams, CodexRpcUnit> = CodexRpcMethod(
            method = "process/resizePty",
            paramsSerializer = ProcessResizePtyParams.serializer(),
            resultSerializer = CodexRpcUnit.serializer(),
            emptyResult = CodexRpcUnit,
        )
    }
}
