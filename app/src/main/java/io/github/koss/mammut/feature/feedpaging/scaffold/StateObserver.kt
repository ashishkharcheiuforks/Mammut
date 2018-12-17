package io.github.koss.mammut.feature.feedpaging.scaffold

/**
 * Standard StateObserver interface
 *
 * [State] The state to be observed
 */
interface StateObserver<State> {
    fun stateChanged(oldState: State?, newState: State)
}