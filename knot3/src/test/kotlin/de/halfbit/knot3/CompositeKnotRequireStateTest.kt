package de.halfbit.knot3

import de.halfbit.knot3.utils.RxPluginsException
import org.junit.Rule
import org.junit.Test

class CompositeKnotRequireStateTest {

    @Rule
    @JvmField
    var rxPluginsException: RxPluginsException = RxPluginsException.none()

    private sealed class State {
        object Empty : State()
        object Loading : State()
        data class Content(val data: String) : State()
    }

    private sealed class Change {
        object Load : Change() {
            data class Success(val data: String) : Change()
        }
    }

    private sealed class Action {
        object Load : Action()
    }

    @Test
    fun `requireState lets known change through`() {
        val knot = createKnot(initialState = State.Empty)
        val states = knot.state.test()
        knot.change.accept(Change.Load)
        states.assertValues(
            State.Empty,
            State.Loading,
            State.Content("ok")
        )
    }

    @Test
    fun `requireState throws when unknown change received`() {
        rxPluginsException.expect(IllegalStateException::class)

        val knot = createKnot(initialState = State.Loading)
        knot.state.test()
        knot.change.accept(Change.Load)
    }

    private fun createKnot(initialState: State): CompositeKnot<State> =
        compositeKnot<State> {
            state { initial = initialState }
        }.apply {
            registerDelegate<Change, Action> {
                changes {
                    reduce<Change.Load> { change ->
                        requireState<State.Empty>(change) {
                            State.Loading + Action.Load
                        }
                    }
                    reduce<Change.Load.Success> { change ->
                        State.Content(change.data).only
                    }
                }
                actions {
                    perform<Action.Load> {
                        map { Change.Load.Success("ok") }
                    }
                }
            }
            compose()
        }
}
