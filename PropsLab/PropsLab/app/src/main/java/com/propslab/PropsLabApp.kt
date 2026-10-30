package com.propslab

import android.app.Application
import com.propslab.agent.AgentTools
import com.propslab.agent.ConsoleEngine
import com.propslab.agent.ToolRegistry
import com.propslab.compute.ComputeSettings
import com.propslab.compute.KaggleClient
import com.propslab.data.PropsDatabase
import com.propslab.data.PropsRepository
import com.propslab.data.RoomTranscript
import com.propslab.inference.ChampionTable
import com.propslab.inference.DeviceAssistant
import com.propslab.feed.FeedSettings
import com.propslab.feed.FeedSync
import com.propslab.mathx.HybridMathModel
import com.propslab.mathx.LocalMathModel
import com.propslab.mathx.LlamaCppModel
import com.propslab.mathx.MathXAssistant
import com.propslab.work.FeedWorker
import com.propslab.work.FreshnessWorker
import com.propslab.work.LearningWorker
import com.propslab.work.SandboxWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class PropsLabApp : Application() {
    private var repoRef: PropsRepository? = null
    val repo: PropsRepository get() = checkNotNull(repoRef) { "the repository is created in Application.onCreate" }

    /** The only language-model boundary: llama.cpp reading weights from this app's own storage. */
    private var mathModelRef: LocalMathModel? = null
    val mathModel: LocalMathModel get() = checkNotNull(mathModelRef) { "the model is created in Application.onCreate" }

    /** The tools a local model may call once weights are installed. */
    private var agentToolsRef: ToolRegistry? = null
    val agentTools: ToolRegistry get() = checkNotNull(agentToolsRef) { "the tools are created in Application.onCreate" }

    /** The console: routed operations on one side, the on-device model on the other. */
    private var consoleRef: ConsoleEngine? = null
    val console: ConsoleEngine get() = checkNotNull(consoleRef) { "the console is created in Application.onCreate" }

    /** Where a Kaggle account is saved, for the high-compute lane. */
    private var computeSettingsRef: ComputeSettings? = null
    val computeSettings: ComputeSettings get() = checkNotNull(computeSettingsRef) { "compute settings are created in Application.onCreate" }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Tunables first: fmtOdds, the staking line and the sampler all read this object,
        // and none of them can reach a Context to load preferences themselves.
        com.propslab.settings.AppSettings.init(this)
        // Entitlement, the optional account and the optional cloud endpoint are read before
        // any screen composes, so a premium build never flashes the free gates first.
        com.propslab.billing.TierStore.init(this)
        com.propslab.account.Account.init(this)
        com.propslab.inference.CloudModel.init(this)
        // The weights are looked up in files/models at the first question, off the main
        // thread. With none present the model says so and the template assistant continues.
        val model = LlamaCppModel(File(filesDir, "models"))
        mathModelRef = model
        val database = PropsDatabase.build(this)
        // Local weights first; the paid cloud endpoint is only a fallback when the phone has
        // nothing to run. A phone with weights never sends a prompt off the device.
        val assistant = MathXAssistant(HybridMathModel(model))
        // The promoted Kaggle GBM needs five feature columns this feed does not carry, so the
        // app runs the exported empirical points table instead. It is loaded once and used
        // for points only; every other market stays on the game-log normal model.
        val champion = runCatching {
            assets.open("champion_table.json").bufferedReader().use { ChampionTable.fromJson(it.readText()) }
        }.getOrNull()
        val feed = FeedSync(database, FeedSettings(this), assistant, champion = champion)
        val compute = ComputeSettings(this)
        computeSettingsRef = compute
        // The Kaggle lane is wired, not promised: with no account saved the tool says so.
        val repository = PropsRepository(database, assistant, feed, KaggleClient(), compute)
        repoRef = repository
        // The registry carries the system tools too, so a sentence can run the machine —
        // the same registry the console routes into, so the two can never drift apart.
        val tools = AgentTools.create(repository, repository)
        agentToolsRef = tools
        consoleRef = ConsoleEngine(tools, RoomTranscript(database.consoleDao()), DeviceAssistant(model))
        appScope.launch {
            repo.seedFixturesIfEmpty()
            // The app does its own first pass: sync, first formula round, learning update.
            // Every step is idempotent, so a second launch is cheap and a failed network
            // call is reported without stopping the rest.
            repo.bootstrap()
        }
        FreshnessWorker.schedule(this)
        SandboxWorker.schedule(this)
        FeedWorker.schedule(this)
        LearningWorker.schedule(this)
    }
}
