package com.example.memesji.util

import androidx.lifecycle.Observer

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents has not been handled.
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    // Corrected signature: parameter 'value' is non-nullable as per the interface definition
    override fun onChanged(value: Event<T>) {
        // Check the event's content, not the event object itself for nullability here,
        // as the LiveData might emit a non-null Event wrapper with null content.
        // However, the primary pattern is handling the event itself.
        value.getContentIfNotHandled()?.let { content ->
            onEventUnhandledContent(content)
        }
    }
}
