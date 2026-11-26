package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Course
import com.example.data.seed.CategoryDefinitions

object HomeCourseFilter {

  private val canonicalLevelOrder = listOf("BEGINNER", "INTERMEDIATE", "ADVANCED")

  fun formatSkillLevelLabel(rawLevel: String): String {
    return rawLevel.trim()
      .lowercase()
      .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
  }

  fun resolvedCategoriesForCourse(course: Course): List<String> {
    val explicit = buildList {
      if (course.category.isNotBlank()) add(course.category.trim())
      course.categories.map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(it) }
    }.distinctBy { it.lowercase() }
    return explicit.ifEmpty {
      CategoryDefinitions.resolveCategories(course.academyId, course.title)
    }
  }

  fun extractSkillLevels(courses: List<Course>): List<String> {
    val distinctByLower = linkedMapOf<String, String>()
    courses.forEach { course ->
      val level = course.level.trim()
      if (level.isNotEmpty()) {
        distinctByLower.putIfAbsent(level.lowercase(), level)
      }
    }
    val ordered = mutableListOf<String>()
    canonicalLevelOrder.forEach { key ->
      distinctByLower.remove(key.lowercase())?.let { ordered.add(it) }
    }
    distinctByLower.values.sortedBy { it.lowercase() }.forEach { ordered.add(it) }
    return ordered
  }

  fun extractCategories(courses: List<Course>): List<String> {
    val presentByLower = linkedMapOf<String, String>()
    courses.forEach { course ->
      resolvedCategoriesForCourse(course).forEach { cat ->
        presentByLower.putIfAbsent(cat.lowercase(), cat)
      }
    }
    val ordered = mutableListOf<String>()
    CategoryDefinitions.categories.forEach { spec ->
      presentByLower.remove(spec.name.lowercase())?.let { ordered.add(it) }
    }
    presentByLower.values.sortedBy { it.lowercase() }.forEach { ordered.add(it) }
    return ordered
  }

  fun filterCourses(
    courses: List<Course>,
    query: String,
    selectedCategory: String?,
    selectedLevel: String?
  ): List<Course> {
    val q = query.trim().lowercase()
    val categoryTarget = selectedCategory?.trim()?.takeIf { it.isNotEmpty() }
    val levelTarget = selectedLevel?.trim()?.takeIf { it.isNotEmpty() }

    return courses.filter { course ->
      val courseCategories = resolvedCategoriesForCourse(course)

      val matchesCategory = categoryTarget == null ||
        courseCategories.any { it.equals(categoryTarget, ignoreCase = true) }

      val matchesLevel = levelTarget == null ||
        course.level.trim().equals(levelTarget, ignoreCase = true)

      val matchesQuery = q.isEmpty() ||
        course.title.lowercase().contains(q) ||
        course.subtitle.lowercase().contains(q) ||
        course.academyName.lowercase().contains(q) ||
        course.instructorName.lowercase().contains(q) ||
        course.level.lowercase().contains(q) ||
        courseCategories.any { it.lowercase().contains(q) } ||
        course.tags.any { it.lowercase().contains(q) } ||
        course.description.lowercase().contains(q)

      matchesCategory && matchesLevel && matchesQuery
    }
  }
}

@Composable
fun HomeSearchFilterBar(
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  availableCategories: List<String>,
  selectedCategory: String?,
  onSelectCategory: (String?) -> Unit,
  availableLevels: List<String>,
  selectedLevel: String?,
  onSelectLevel: (String?) -> Unit,
  onClearAll: () -> Unit,
  modifier: Modifier = Modifier
) {
  val hasActiveFilter = searchQuery.isNotBlank() || selectedCategory != null || selectedLevel != null

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .testTag("home_search_input"),
      placeholder = {
        Text(text = stringResource(R.string.home_search_placeholder))
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Filled.Search,
          contentDescription = null
        )
      },
      trailingIcon = {
        if (hasActiveFilter) {
          IconButton(
            onClick = onClearAll,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("home_search_clear_button")
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = stringResource(R.string.home_search_clear_description)
            )
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
      )
    )

    if (availableLevels.isNotEmpty() || availableCategories.isNotEmpty()) {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (hasActiveFilter) {
          item(key = "reset_filters") {
            TextButton(
              onClick = onClearAll,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("home_filter_reset")
            ) {
              Text(
                text = stringResource(R.string.home_filter_clear_all),
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        items(availableLevels, key = { "level_$it" }) { level ->
          val isSelected = selectedLevel.equals(level, ignoreCase = true)
          val tagSlug = level.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_")
          FilterChip(
            selected = isSelected,
            onClick = { onSelectLevel(if (isSelected) null else level) },
            label = { Text(HomeCourseFilter.formatSkillLevelLabel(level)) },
            leadingIcon = {
              Icon(
                imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.SignalCellularAlt,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
              )
            },
            modifier = Modifier.testTag("home_filter_level_$tagSlug")
          )
        }

        if (availableLevels.isNotEmpty() && availableCategories.isNotEmpty()) {
          item(key = "filter_divider") {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Spacer(modifier = Modifier.width(2.dp))
              VerticalDivider(
                modifier = Modifier.height(22.dp),
                color = MaterialTheme.colorScheme.outline
              )
              Spacer(modifier = Modifier.width(2.dp))
            }
          }
        }

        items(availableCategories, key = { "cat_$it" }) { category ->
          val isSelected = selectedCategory.equals(category, ignoreCase = true)
          val tagSlug = category.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_")
          FilterChip(
            selected = isSelected,
            onClick = { onSelectCategory(if (isSelected) null else category) },
            label = { Text(category) },
            leadingIcon = {
              Icon(
                imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Category,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
              )
            },
            modifier = Modifier.testTag("home_filter_category_$tagSlug")
          )
        }
      }
    }
  }
}
