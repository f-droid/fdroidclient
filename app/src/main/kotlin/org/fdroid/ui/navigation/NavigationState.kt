/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * From: https://github.com/android/nav3-recipes/blob/549398ffeefbdf8c0b09b71a098cd14b1520695b/app/src/main/java/com/example/nav3recipes/multiplestacks/NavigationState.kt
 */

package org.fdroid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/** Create a navigation state that persists config changes and process death. */
@Composable
fun rememberNavigationState(startRoute: NavKey, topLevelRoutes: List<NavKey>): NavigationState {
  val topLevelRoute =
    rememberSerializable(
      startRoute,
      topLevelRoutes,
      serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
      mutableStateOf(startRoute)
    }

  val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

  return remember(startRoute, topLevelRoutes) {
    NavigationState(startRoute = startRoute, topLevelRoute = topLevelRoute, backStacks = backStacks)
  }
}

/**
 * State holder for navigation state.
 *
 * @param startRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoute - the current top level route
 * @param backStacks - the back stacks for each top level route
 */
class NavigationState(
  val startRoute: NavKey,
  topLevelRoute: MutableState<NavKey>,
  val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
  var topLevelRoute: NavKey by topLevelRoute
  val stacksInUse: List<NavKey>
    get() =
      if (topLevelRoute == startRoute) {
        listOf(startRoute)
      } else {
        listOf(startRoute, topLevelRoute)
      }
}

/** Convert NavigationState into NavEntries. */
@Composable
fun NavigationState.toEntries(
  entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {
  val decoratedEntries =
    backStacks.mapValues { (_, stack) ->
      val decorators =
        listOf(
          rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
          rememberViewModelStoreNavEntryDecorator(),
        )
      rememberDecoratedNavEntries(
        backStack = stack,
        entryDecorators = decorators,
        entryProvider = entryProvider,
      )
    }

  return stacksInUse.flatMap { decoratedEntries[it] ?: emptyList() }.toMutableStateList()
}
