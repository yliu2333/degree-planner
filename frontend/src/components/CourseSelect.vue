<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  courses: {
    type: Array,
    required: true
  },
  modelValue: {
    type: Array,
    required: true
  },
  placeholder: {
    type: String,
    default: 'Search courses'
  }
})

const emit = defineEmits(['update:modelValue'])

const query = ref('')
const isOpen = ref(false)

const selectedCourses = computed(() =>
  props.modelValue
    .map((id) => props.courses.find((course) => course.id === id))
    .filter(Boolean)
)

const availableCourses = computed(() => {
  const needle = query.value.trim().toLowerCase()

  return props.courses
    .filter((course) => !props.modelValue.includes(course.id))
    .filter((course) => {
      if (!needle) return true
      return `${course.id} ${course.name}`.toLowerCase().includes(needle)
    })
    .slice(0, 12)
})

function addCourse(courseId) {
  emit('update:modelValue', [...props.modelValue, courseId])
  query.value = ''
  isOpen.value = false
}

function removeCourse(courseId) {
  emit(
    'update:modelValue',
    props.modelValue.filter((id) => id !== courseId)
  )
}
</script>

<template>
  <label class="field">
    <span class="field-label">{{ label }}</span>
    <div class="select-shell" @focusin="isOpen = true">
      <div class="selected-list" v-if="selectedCourses.length">
        <button
          v-for="course in selectedCourses"
          :key="course.id"
          class="selected-pill"
          type="button"
          @click="removeCourse(course.id)"
          :aria-label="`Remove ${course.id}`"
        >
          <span>{{ course.id }}</span>
          <span aria-hidden="true">×</span>
        </button>
      </div>

      <input
        v-model="query"
        class="course-search"
        type="search"
        :placeholder="placeholder"
        @focus="isOpen = true"
      />

      <div v-if="isOpen && availableCourses.length" class="course-menu">
        <button
          v-for="course in availableCourses"
          :key="course.id"
          class="course-option"
          type="button"
          @mousedown.prevent="addCourse(course.id)"
        >
          <span class="course-option-id">{{ course.id }}</span>
          <span class="course-option-name">{{ course.name }}</span>
          <span class="course-option-credits">{{ course.credits }} cr</span>
        </button>
      </div>
    </div>
  </label>
</template>
