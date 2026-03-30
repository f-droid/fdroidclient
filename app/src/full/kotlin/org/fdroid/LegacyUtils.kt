package org.fdroid

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object LegacyUtils {

  @JvmStatic
  @JvmOverloads
  fun <T> collectInJava(
    scope: CoroutineScope = CoroutineScope(context = kotlinx.coroutines.Dispatchers.Main),
    flow: Flow<T>,
    action: (T) -> Any
  ): Job {
    return scope.launch {
      flow.collect { value ->
        action(value)
      }
    }
  }

}
