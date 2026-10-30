package com.example.model

/**
 * What the AI window can honestly say about the local model.
 *
 * READY means llama-server answered /health. It is never set by a launch command alone.
 */
enum class AiEngineState {
    NOT_SET_UP,
    CHECKING,
    INSTALLING,
    DOWNLOADING,
    STARTING,
    READY,
    STOPPED,
    ERROR
}
