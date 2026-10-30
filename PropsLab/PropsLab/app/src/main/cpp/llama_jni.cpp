// The only native code in PropsLab: a narrow JNI surface over llama.cpp.
//
// Everything here is deliberately boring. One engine per process, one mutex around
// generation, and no way to reach the network. The functions are named by what they do
// rather than what llama.cpp calls them, so the Kotlin side reads as intent and the C++
// side stays replaceable: swap this file for another runtime and the app does not change.

#include <jni.h>
#include <android/log.h>

#include <chrono>
#include <atomic>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "llama.h"

namespace {

// The one place where the runtime says what it did. Failures and timings land in logcat
// under a single tag so a device can be diagnosed without a debugger attached.
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "PropsLabNative", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  "PropsLabNative", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "PropsLabNative", __VA_ARGS__)

using clock_ms = std::chrono::steady_clock;

int64_t since(const clock_ms::time_point & start) {
    return std::chrono::duration_cast<std::chrono::milliseconds>(clock_ms::now() - start).count();
}

std::mutex g_error_mutex;
std::string g_last_error;
std::string g_last_warning;

void set_error(const std::string & message) {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    g_last_error = message;
}

void set_warning(const std::string & message) {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    g_last_warning = message;
}

std::string get_error() {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    return g_last_error;
}

std::string get_warning() {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    return g_last_warning;
}

struct Engine {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    llama_adapter_lora * lora = nullptr;
    const llama_vocab * vocab = nullptr;
    std::string chat_template;
    int32_t n_ctx = 4096;
    // Set when the Kotlin caller gives up (a timeout, a cancelled screen). Checked once
    // per token, so a discarded generation stops within one token instead of holding the
    // mutex and making every later question queue behind it.
    std::atomic<bool> cancel{ false };
    // Generation is serial by construction: one model, one context, one caller at a time.
    std::mutex mutex;
};

Engine * engine_of(jlong handle) {
    return reinterpret_cast<Engine *>(handle);
}

std::string from_jstring(JNIEnv * env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return {};
    }
    std::string text(chars);
    env->ReleaseStringUTFChars(value, chars);
    return text;
}

void throw_runtime(JNIEnv * env, const std::string & message) {
    jclass type = env->FindClass("java/lang/RuntimeException");
    if (type != nullptr) {
        env->ThrowNew(type, message.c_str());
    }
}

void ensure_backend() {
    static std::once_flag once;
    std::call_once(once, [] { llama_backend_init(); });
}

std::vector<llama_token> tokenize(const llama_vocab * vocab, const std::string & text, bool add_special) {
    // llama_tokenize answers with the negative of the size it needs when the buffer is null
    // or short, so the size comes back before anything is written.
    const int32_t needed = llama_tokenize(vocab, text.c_str(), static_cast<int32_t>(text.size()), nullptr, 0, add_special, false);
    if (needed == 0) {
        return {};
    }
    const int32_t capacity = needed > 0 ? needed : -needed;
    if (capacity <= 0) {
        return {};
    }
    std::vector<llama_token> tokens(static_cast<size_t>(capacity));
    const int32_t written = llama_tokenize(
        vocab, text.c_str(), static_cast<int32_t>(text.size()), tokens.data(), capacity, add_special, false);
    if (written <= 0) {
        return {};
    }
    tokens.resize(static_cast<size_t>(written));
    return tokens;
}

std::string token_to_text(const llama_vocab * vocab, llama_token token) {
    char buffer[256];
    const int32_t written = llama_token_to_piece(vocab, token, buffer, static_cast<int32_t>(sizeof(buffer)), 0, false);
    if (written <= 0) {
        return {};
    }
    const int32_t size = written < static_cast<int32_t>(sizeof(buffer)) ? written : static_cast<int32_t>(sizeof(buffer));
    return std::string(buffer, static_cast<size_t>(size));
}

/**
 * The prompt, in the model's own chat format when it ships one.
 *
 * A Qwen model asked in plain text and the same model asked through its template are not
 * the same model, so the template is preferred and the plain fallback is only reached when
 * the GGUF carries none.
 */
std::string build_prompt(Engine * engine, const std::string & system, const std::string & user) {
    if (!engine->chat_template.empty()) {
        const llama_chat_message messages[2] = {
            { "system", system.c_str() },
            { "user",   user.c_str()   },
        };
        const int32_t needed = llama_chat_apply_template(engine->chat_template.c_str(), messages, 2, true, nullptr, 0);
        if (needed > 0) {
            std::vector<char> buffer(static_cast<size_t>(needed) + 1);
            const int32_t written = llama_chat_apply_template(
                engine->chat_template.c_str(), messages, 2, true, buffer.data(), needed + 1);
            if (written > 0) {
                return std::string(buffer.data(), static_cast<size_t>(written));
            }
        }
    }
    return system + "\n\n" + user + "\n\nAssistant:";
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_propslab_mathx_LlamaNative_load(
    JNIEnv * env, jobject,
    jstring j_model_path, jstring j_lora_path, jint n_ctx, jint n_threads) {
    ensure_backend();
    set_error("");
    set_warning("");

    const std::string model_path = from_jstring(env, j_model_path);
    if (model_path.empty()) {
        set_error("no model path was given");
        LOGE("load: no model path");
        return 0;
    }

    const auto started = clock_ms::now();
    auto * engine = new Engine();

    llama_model_params model_params = llama_model_default_params();
    // mmap is the loader's default at this revision; only the device policy is pinned.
    model_params.n_gpu_layers = 0; // CPU only: no claim of a GPU this app does not control.

    LOGI("load: opening %s", model_path.c_str());
    engine->model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (engine->model == nullptr) {
        set_error("llama.cpp could not read " + model_path);
        LOGE("load: could not read %s", model_path.c_str());
        delete engine;
        return 0;
    }
    LOGI("load: model mapped in %lld ms", static_cast<long long>(since(started)));

    const uint32_t cores = std::max(2u, std::thread::hardware_concurrency());
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx          = n_ctx > 0 ? static_cast<uint32_t>(n_ctx) : 4096;
    ctx_params.n_batch        = ctx_params.n_ctx < 2048u ? ctx_params.n_ctx : 2048u;
    ctx_params.n_seq_max      = 1;
    ctx_params.n_threads      = static_cast<int32_t>(cores / 2);
    ctx_params.n_threads_batch = static_cast<int32_t>(cores);
    engine->n_ctx = static_cast<int32_t>(ctx_params.n_ctx);

    engine->ctx = llama_init_from_model(engine->model, ctx_params);
    if (engine->ctx == nullptr) {
        set_error("the model loaded but no context could be created (not enough memory?)");
        LOGE("load: context creation failed at n_ctx=%u", ctx_params.n_ctx);
        llama_model_free(engine->model);
        delete engine;
        return 0;
    }

    engine->vocab = llama_model_get_vocab(engine->model);
    const char * tmpl = llama_model_chat_template(engine->model, nullptr);
    if (tmpl != nullptr) {
        engine->chat_template = tmpl;
    }

    // A custom adapter is optional and must never cost the user the base model: if it will
    // not attach, the run continues and the reason is reported rather than swallowed.
    const std::string lora_path = from_jstring(env, j_lora_path);
    if (!lora_path.empty()) {
        engine->lora = llama_adapter_lora_init(engine->model, lora_path.c_str());
        if (engine->lora == nullptr) {
            set_warning("the adapter at " + lora_path + " would not attach; running the base model");
            LOGW("load: adapter refused: %s", lora_path.c_str());
        } else {
            LOGI("load: adapter attached %s", lora_path.c_str());
            llama_adapter_lora * adapters[1] = { engine->lora };
            float scales[1] = { 1.0f };
            if (llama_set_adapters_lora(engine->ctx, adapters, 1, scales) != 0) {
                set_warning("the adapter loaded but the context refused it; running the base model");
                llama_adapter_lora_free(engine->lora);
                engine->lora = nullptr;
            }
        }
    }

    LOGI("load: ready in %lld ms (n_ctx=%d, lora=%s)",
         static_cast<long long>(since(started)), engine->n_ctx,
         engine->lora != nullptr ? "yes" : "no");
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT jstring JNICALL
Java_com_propslab_mathx_LlamaNative_prompt(
    JNIEnv * env, jobject, jlong handle, jstring j_system, jstring j_user) {
    Engine * engine = engine_of(handle);
    if (engine == nullptr) {
        return env->NewStringUTF("");
    }
    std::lock_guard<std::mutex> lock(engine->mutex);
    return env->NewStringUTF(build_prompt(engine, from_jstring(env, j_system), from_jstring(env, j_user)).c_str());
}

JNIEXPORT jstring JNICALL
Java_com_propslab_mathx_LlamaNative_generate(
    JNIEnv * env, jobject, jlong handle, jstring j_prompt,
    jint max_tokens, jfloat temperature, jint top_k, jfloat top_p, jint seed) {
    Engine * engine = engine_of(handle);
    if (engine == nullptr) {
        throw_runtime(env, "the model is not loaded");
        return nullptr;
    }

    std::lock_guard<std::mutex> lock(engine->mutex);

    const std::string prompt = from_jstring(env, j_prompt);
    const std::vector<llama_token> prompt_tokens = tokenize(engine->vocab, prompt, true);
    if (prompt_tokens.empty()) {
        throw_runtime(env, "the prompt produced no tokens");
        return nullptr;
    }

    const int32_t n_prompt = static_cast<int32_t>(prompt_tokens.size());
    if (n_prompt + 1 >= engine->n_ctx) {
        throw_runtime(env, "the prompt is longer than the model's context (" + std::to_string(engine->n_ctx) + " tokens)");
        return nullptr;
    }

    // One chain per call: temperature and length are properties of this turn, not of the engine.
    llama_sampler * sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (temperature <= 0.0f) {
        llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(sampler, llama_sampler_init_top_k(top_k > 0 ? top_k : 40));
        llama_sampler_chain_add(sampler, llama_sampler_init_top_p(top_p > 0.0f ? top_p : 0.9f, 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(seed >= 0 ? static_cast<uint32_t>(seed) : 1234u));
    }

    llama_batch batch = llama_batch_init(n_prompt + 1, 0, 1);
    for (int32_t i = 0; i < n_prompt; ++i) {
        batch.token[i]      = prompt_tokens[static_cast<size_t>(i)];
        batch.pos[i]        = i;
        batch.n_seq_id[i]   = 1;
        batch.seq_id[i][0]  = 0;
        batch.logits[i]     = (i == n_prompt - 1) ? 1 : 0;
    }
    batch.n_tokens = n_prompt;

    std::string output;
    const int32_t budget = max_tokens > 0 ? max_tokens : 256;
    const int32_t remaining = engine->n_ctx - n_prompt - 1;
    const int32_t limit = budget < remaining ? budget : remaining;
    const auto started = clock_ms::now();
    engine->cancel.store(false);
    LOGI("generate: start, %d prompt tokens, budget %d, temp %.2f", n_prompt, limit, temperature);

    int32_t rc = llama_decode(engine->ctx, batch);
    if (rc != 0) {
        llama_batch_free(batch);
        llama_sampler_free(sampler);
        set_error("llama_decode failed on the prompt (code " + std::to_string(rc) + ")");
        LOGE("generate: prompt decode failed rc=%d with %d tokens", rc, n_prompt);
        throw_runtime(env, "llama_decode failed on the prompt (code " + std::to_string(rc) + ")");
        return nullptr;
    }

    llama_pos position = n_prompt;
    for (int32_t step = 0; step < limit; ++step) {
        if (engine->cancel.load()) {
            LOGI("generate: cancelled after %d token(s)", step);
            break;
        }
        const int32_t last = batch.n_tokens - 1;
        const float * logits = llama_get_logits_ith(engine->ctx, last);
        const int32_t n_vocab = llama_vocab_n_tokens(engine->vocab);

        std::vector<llama_token_data> candidates(static_cast<size_t>(n_vocab));
        for (int32_t v = 0; v < n_vocab; ++v) {
            candidates[static_cast<size_t>(v)] = { v, logits[v], 0.0f };
        }
        llama_token_data_array array = { candidates.data(), candidates.size(), -1, false };
        llama_sampler_apply(sampler, &array);
        if (array.selected < 0) {
            break;
        }
        const llama_token token = candidates[static_cast<size_t>(array.selected)].id;
        llama_sampler_accept(sampler, token);

        if (llama_vocab_is_eog(engine->vocab, token)) {
            break;
        }
        output += token_to_text(engine->vocab, token);

        // The next step is a single token appended to the same sequence.
        batch.token[0]     = token;
        batch.pos[0]       = position;
        batch.n_seq_id[0]  = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0]    = 1;
        batch.n_tokens     = 1;
        position++;

        rc = llama_decode(engine->ctx, batch);
        if (rc != 0) {
            // A mid-run decode failure still leaves usable text; report it rather than
            // pretending the run ended cleanly.
            set_warning("generation stopped early: llama_decode failed (code " + std::to_string(rc) + ")");
            LOGW("generate: decode failed at step %d rc=%d", step, rc);
            break;
        }
    }

    LOGI("generate: %d prompt tokens -> %zu chars in %lld ms (budget %d)",
         n_prompt, output.size(), static_cast<long long>(since(started)), limit);
    llama_batch_free(batch);
    llama_sampler_free(sampler);
    return env->NewStringUTF(output.c_str());
}

JNIEXPORT void JNICALL
Java_com_propslab_mathx_LlamaNative_cancel(JNIEnv *, jobject, jlong handle) {
    Engine * engine = engine_of(handle);
    if (engine == nullptr) {
        return;
    }
    engine->cancel.store(true);
    LOGI("generate: cancel requested");
}

JNIEXPORT void JNICALL
Java_com_propslab_mathx_LlamaNative_free(JNIEnv *, jobject, jlong handle) {
    Engine * engine = engine_of(handle);
    if (engine == nullptr) {
        return;
    }
    std::lock_guard<std::mutex> lock(engine->mutex);
    if (engine->lora != nullptr) {
        llama_adapter_lora_free(engine->lora);
    }
    if (engine->ctx != nullptr) {
        llama_free(engine->ctx);
    }
    if (engine->model != nullptr) {
        llama_model_free(engine->model);
    }
    delete engine;
}

JNIEXPORT jstring JNICALL
Java_com_propslab_mathx_LlamaNative_lastError(JNIEnv * env, jobject) {
    return env->NewStringUTF(get_error().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_propslab_mathx_LlamaNative_lastWarning(JNIEnv * env, jobject) {
    return env->NewStringUTF(get_warning().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_propslab_mathx_LlamaNative_version(JNIEnv * env, jobject) {
    return env->NewStringUTF(llama_version());
}

} // extern "C"
