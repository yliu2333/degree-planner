<script setup>
import { computed, onMounted, ref } from 'vue'
import CourseSelect from './components/CourseSelect.vue'
import SemesterCard from './components/SemesterCard.vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const courses = ref([])
const completedCourseIds = ref([])
const targetCourseIds = ref([])
const maxCreditsPerSemester = ref(16)
const startTerm = ref('FALL')
const plan = ref(null)
const isLoadingCatalog = ref(false)
const isGenerating = ref(false)
const errorMessage = ref('')
const catalogOpen = ref(true)

const sortedCourses = computed(() =>
  [...courses.value].sort((a, b) => a.id.localeCompare(b.id))
)

const canGenerate = computed(() => targetCourseIds.value.length > 0 && !isGenerating.value)

async function fetchJson(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message || `Request failed with status ${response.status}`)
  }

  return response.json()
}

async function loadCourses() {
  isLoadingCatalog.value = true
  errorMessage.value = ''

  try {
    courses.value = await fetchJson('/api/courses')
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoadingCatalog.value = false
  }
}

async function generatePlan() {
  if (!canGenerate.value) return

  isGenerating.value = true
  errorMessage.value = ''

  try {
    plan.value = await fetchJson('/api/plan', {
      method: 'POST',
      body: JSON.stringify({
        completedCourseIds: completedCourseIds.value,
        targetCourseIds: targetCourseIds.value,
        maxCreditsPerSemester: Number(maxCreditsPerSemester.value),
        startTerm: startTerm.value
      })
    })
  } catch (error) {
    plan.value = null
    errorMessage.value = error.message
  } finally {
    isGenerating.value = false
  }
}

onMounted(loadCourses)
</script>

<template>
  <main class="page-shell">
    <section class="intro">
      <p class="eyebrow">Degree Planner</p>
      <h1>Plan a semester path through a CS curriculum.</h1>
    </section>

    <section class="panel setup-panel" aria-labelledby="setup-heading">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Setup</p>
          <h2 id="setup-heading">Choose your courses</h2>
        </div>
        <span v-if="isLoadingCatalog" class="quiet-note">Loading catalog</span>
      </div>

      <div class="form-grid">
        <CourseSelect
          v-model="completedCourseIds"
          label="Completed courses"
          :courses="sortedCourses"
          placeholder="Search completed courses"
        />

        <CourseSelect
          v-model="targetCourseIds"
          label="Target courses"
          :courses="sortedCourses"
          placeholder="Search target courses"
        />

        <label class="field compact-field">
          <span class="field-label">Max credits per semester</span>
          <input v-model="maxCreditsPerSemester" min="1" class="number-input" type="number" />
        </label>

        <div class="field compact-field">
          <span class="field-label">Start term</span>
          <div class="term-toggle" role="group" aria-label="Start term">
            <button
              type="button"
              :class="{ active: startTerm === 'FALL' }"
              @click="startTerm = 'FALL'"
            >
              Fall
            </button>
            <button
              type="button"
              :class="{ active: startTerm === 'SPRING' }"
              @click="startTerm = 'SPRING'"
            >
              Spring
            </button>
          </div>
        </div>
      </div>

      <button class="primary-button" type="button" :disabled="!canGenerate" @click="generatePlan">
        <span v-if="isGenerating" class="spinner" aria-hidden="true"></span>
        <span>{{ isGenerating ? 'Generating' : 'Generate Plan' }}</span>
      </button>
    </section>

    <section v-if="errorMessage" class="error-banner" role="alert">
      {{ errorMessage }}
    </section>

    <section class="result-section" aria-labelledby="result-heading">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Result</p>
          <h2 id="result-heading">Semester timeline</h2>
        </div>
      </div>

      <div v-if="!plan" class="empty-state">
        Select at least one target course to generate a plan.
      </div>

      <div v-else class="timeline">
        <SemesterCard
          v-for="semester in plan.semesters"
          :key="`${semester.term}-${semester.year}`"
          :semester="semester"
        />
      </div>
    </section>

    <section class="catalog-section">
      <button class="catalog-toggle" type="button" @click="catalogOpen = !catalogOpen">
        <span>Course catalog</span>
        <span>{{ catalogOpen ? 'Collapse' : 'Expand' }}</span>
      </button>

      <div v-if="catalogOpen" class="catalog-table-wrap">
        <table class="catalog-table">
          <thead>
            <tr>
              <th>Course</th>
              <th>Credits</th>
              <th>Terms</th>
              <th>Prerequisites</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="course in sortedCourses" :key="course.id">
              <td>
                <strong>{{ course.id }}</strong>
                <span>{{ course.name }}</span>
              </td>
              <td>{{ course.credits }}</td>
              <td>{{ course.offeredTerms.join(', ') }}</td>
              <td>
                <span v-if="!course.prerequisiteIds.length" class="quiet-note">None</span>
                <span
                  v-for="prerequisite in course.prerequisiteIds"
                  v-else
                  :key="prerequisite"
                  class="tag"
                >
                  {{ prerequisite }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>
